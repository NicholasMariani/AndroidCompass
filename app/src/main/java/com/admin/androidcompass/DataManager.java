package com.admin.androidcompass;

import android.content.Context;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Utility class that provides bearing values to true north.
 */
public class DataManager implements SensorEventListener, LocationListener {
    public static final String TAG = "DataManager";

    /**
     * Interface definition for a callback to be invoked when the bearing changes.
     */
    public static interface ChangeEventListener {

        void onTrueBearingChanged(double bearing);

        void onMagneticBearingChanged(int bearing);

        void onMagneticFieldChanged(double geoMagneticField);

        void onCurrentLocationChanged(Location loc);
    }

    private final SensorManager mSensorManager;
    private final LocationManager mLocationManager;
    private final Sensor mSensorAccelerometer;
    private final Sensor mSensorMagneticField;
    private final Sensor mSensorRotationVector;

    // some arrays holding intermediate values read from the sensors, used to calculate our azimuth
    // value

    private float[] mValuesAccelerometer;
    private float[] mValuesMagneticField;
    private float[] mMatrixR;
    private float[] mMatrixI;
    private float[] mMatrixValues;

    private Map<Integer, String> map;

    /**
     * minimum change of bearing (degrees) to notify the change listener
     */
    private final double mMinDiffForEvent;

    /**
     * minimum delay (millis) between notifications for the change listener
     */
    private final double mThrottleTime;

    /**
     * the change event listener
     */
    private ChangeEventListener mChangeEventListener;

    /**
     * angle to magnetic north
     */
    private AverageAngle mAzimuthRadians;

    /**
     * smoothed angle to magnetic north
     */
    private double mAzimuth = Double.NaN;

    /**
     * angle to true north
     */
    private double trueBearing = Double.NaN;

    /**
     * last notified angle to true north
     */
    private double lastTrueBearing = Double.NaN;

    /**
     * angle to magnetic north
     */
    private int magneticBearing = 0;

    /**
     * angle to magnetic north
     */
    private int lastMagneticBearing = 0;

    /**
     * magnetic field value
     */
    private double geoMagneticField;

    /**
     * Current GPS/WiFi location
     */
    private Location mLocation;

    /**
     * when we last dispatched the change event
     */
    private long mLastChangeDispatchedAt = -1;

    /**
     * Default constructor.
     *
     * @param context Application Context
     */
    public DataManager(Context context) {
        this(context, 10, 1, 200);
    }

    /**
     * @param context Application Context
     * @param smoothing the number of measurements used to calculate a mean for the azimuth. Set
     *                      this to 1 for the smallest delay. Setting it to 5-10 to prevents the
     *                      needle from going crazy
     * @param minDiffForEvent minimum change of bearing (degrees) to notify the change listener
     * @param throttleTime minimum delay (millis) between notifications for the change listener
     */
    public DataManager(Context context, int smoothing, double minDiffForEvent, int throttleTime) {
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        mSensorMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mSensorRotationVector = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mValuesAccelerometer = new float[3];
        mValuesMagneticField = new float[3];

        mMatrixR = new float[9];
        mMatrixI = new float[9];
        mMatrixValues = new float[3];

        mMinDiffForEvent = minDiffForEvent;
        mThrottleTime = throttleTime;

        mAzimuthRadians = new AverageAngle(smoothing);

        map = new HashMap<>();
    }

    //==============================================================================================
    // Public API
    //==============================================================================================

    /**
     * Call this method to start bearing updates.
     */
    public void start() {
        mSensorManager.registerListener(this, mSensorAccelerometer, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorMagneticField, SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(this, mSensorRotationVector, SensorManager.SENSOR_DELAY_UI);

        for (final String provider : mLocationManager.getProviders(true)) {
            if (LocationManager.GPS_PROVIDER.equals(provider)
                    || LocationManager.PASSIVE_PROVIDER.equals(provider)
                    || LocationManager.NETWORK_PROVIDER.equals(provider)) {
                if (mLocation == null) {
                    mLocation = mLocationManager.getLastKnownLocation(provider);
                }
                mLocationManager.requestLocationUpdates(provider, 0, 100.0f, this);
            }
        }

        Location lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnownLocation == null) {
            lastKnownLocation = mLocationManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
            onLocationChanged(lastKnownLocation);
        }
    }

    /**
     * call this method to stop bearing updates.
     */
    public void stop()
    {
        mSensorManager.unregisterListener(this, mSensorAccelerometer);
        mSensorManager.unregisterListener(this, mSensorMagneticField);
        mLocationManager.removeUpdates(this);
    }

    /**
     * @return current bearing
     */
    public double getTrueBearing()
    {
        return trueBearing;
    }

    /**
     * @return current bearing
     */
    public int getMagneticBearing()
    {
        return magneticBearing;
    }

    /**
     * Returns the bearing event listener to which bearing events must be sent.
     * @return the bearing event listener
     */
    public ChangeEventListener getChangeEventListener()
    {
        return mChangeEventListener;
    }

    /**
     * Specifies the bearing event listener to which bearing events must be sent.
     * @param changeEventListener the bearing event listener
     */
    public void setChangeEventListener(ChangeEventListener changeEventListener)
    {
        this.mChangeEventListener = changeEventListener;
    }

    //==============================================================================================
    // SensorEventListener implementation
    //==============================================================================================

    @Override
    public void onSensorChanged(SensorEvent event)
    {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, mValuesAccelerometer, 0, 3);
                break;

            case Sensor.TYPE_MAGNETIC_FIELD:
                float magX = event.values[0];
                float magY = event.values[1];
                float magZ = event.values[2];
                double magnitude = Math.sqrt((magX * magX) + (magY * magY) + (magZ * magZ));

                updateMagneticField(magnitude);
                System.arraycopy(event.values, 0, mValuesMagneticField, 0, 3);

                break;

            case Sensor.TYPE_ROTATION_VECTOR:

                float[] orientation = new float[3];
                float[] rMat = new float[9];

                SensorManager.getRotationMatrixFromVector(rMat, event.values);
                int heading = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
                updateMagneticBearing(heading);

                break;
        }

        boolean success = SensorManager.getRotationMatrix(mMatrixR, mMatrixI,
                mValuesAccelerometer,
                mValuesMagneticField);

        // calculate a new smoothed azimuth value and store to mAzimuth
        if (success) {
            SensorManager.getOrientation(mMatrixR, mMatrixValues);
            mAzimuthRadians.putValue(mMatrixValues[0]);
            mAzimuth = Math.toDegrees(mAzimuthRadians.getAverage());
        }

        updateTrueBearing();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {
        String acc = "";
        switch(accuracy)
        {
            case SensorManager.SENSOR_STATUS_NO_CONTACT:
                acc = "<font color='#000000'>No Contact</font>";
                break;
            case SensorManager.SENSOR_STATUS_UNRELIABLE:
                acc = "<font color='#FF0000'>Unreliable</font>";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_LOW:
                acc = "<font color='#FFFF00'>Low</font>";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM:
                acc = "<font color='#FFA500'>Medium</font>";
                break;
            case SensorManager.SENSOR_STATUS_ACCURACY_HIGH:
                acc = "<font color='#008000'>High</font>";
                break;
        }

        map.put(sensor.getType(), acc);
    }

    //==============================================================================================
    // LocationListener implementation
    //==============================================================================================

    @Override
    public void onLocationChanged(Location location)
    {
        // set the new location
        this.mLocation = location;

        if(mChangeEventListener != null) {
            mChangeEventListener.onCurrentLocationChanged(mLocation);
        }

        // update trueBearing
        updateTrueBearing();
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {   }

    @Override
    public void onProviderEnabled(String s) {   }

    @Override
    public void onProviderDisabled(String s) {   }

    //==============================================================================================
    // Private Utilities
    //==============================================================================================

    private void updateTrueBearing()
    {
        if (!Double.isNaN(this.mAzimuth)) {
            if(this.mLocation == null) {
                Log.w(TAG, "Location is NULL bearing is not true north!");
                trueBearing = mAzimuth;
            } else {
                trueBearing = getBearingForLocation(this.mLocation);
            }

            // Throttle dispatching based on mThrottleTime and minDiffForEvent
            if( System.currentTimeMillis() - mLastChangeDispatchedAt > mThrottleTime &&
                    (Double.isNaN(lastTrueBearing) || Math.abs(lastTrueBearing - trueBearing) >= mMinDiffForEvent)) {
                lastTrueBearing = trueBearing;
                if(mChangeEventListener != null) {
                    mChangeEventListener.onTrueBearingChanged(trueBearing);
                }
                mLastChangeDispatchedAt = System.currentTimeMillis();
            }
        }
    }

    private void updateMagneticBearing(int heading)
    {
        lastMagneticBearing = magneticBearing;
        magneticBearing = heading;

        if(mChangeEventListener != null) {
            mChangeEventListener.onMagneticBearingChanged(magneticBearing);
        }
    }

    private void updateMagneticField(double gmf) {
        geoMagneticField = gmf;
        if (mChangeEventListener != null) {
            mChangeEventListener.onMagneticFieldChanged(this.geoMagneticField);
        }
    }

    private double getBearingForLocation(Location location)
    {
        return mAzimuth + getGeomagneticField(location).getDeclination();
    }

    public GeomagneticField getGeomagneticField(Location location)
    {
        GeomagneticField gmf = new GeomagneticField(
                (float)location.getLatitude(),
                (float)location.getLongitude(),
                (float)location.getAltitude(),
                System.currentTimeMillis());
        return gmf;
    }

    public Location getLocation()
    {
        return mLocation;
    }

    public Map<Integer, String> getAccuracyMap()
    {
        return map;
    }
}

class AverageAngle {
    private double[] mValues;
    private int mCurrentIndex;
    private int mNumberOfFrames;
    private boolean mIsFull;
    private double mAverageValue = Double.NaN;

    public AverageAngle(int frames) {
        this.mNumberOfFrames = frames;
        this.mCurrentIndex = 0;
        this.mValues = new double[frames];
    }

    public void putValue(double d) {
        mValues[mCurrentIndex] = d;
        if (mCurrentIndex == mNumberOfFrames - 1) {
            mCurrentIndex = 0;
            mIsFull = true;
        } else {
            mCurrentIndex++;
        }
        updateAverageValue();
    }

    public double getAverage() {
        return this.mAverageValue;
    }

    private void updateAverageValue() {
        int numberOfElementsToConsider = mNumberOfFrames;
        if (!mIsFull) {
            numberOfElementsToConsider = mCurrentIndex + 1;
        }

        if (numberOfElementsToConsider == 1) {
            this.mAverageValue = mValues[0];
            return;
        }

        // Formula: http://en.wikipedia.org/wiki/Circular_mean
        double sumSin = 0.0;
        double sumCos = 0.0;
        for (int i = 0; i < numberOfElementsToConsider; i++) {
            double v = mValues[i];
            sumSin += Math.sin(v);
            sumCos += Math.cos(v);
        }
        this.mAverageValue = Math.atan2(sumSin, sumCos);
    }
}