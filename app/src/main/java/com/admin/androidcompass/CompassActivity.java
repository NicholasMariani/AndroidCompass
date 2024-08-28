package com.admin.androidcompass;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.maps.model.LatLng;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class CompassActivity extends Activity implements DataManager.ChangeEventListener{

    private SharedPreferences sharedPreferences;
    private int currentDegree = 0;

    private Address address;
    private String[] coords;
    private ImageView image;
    private TextView cHeading;
    private TextView tvHeading;
    private TextView city;
    private TextView country;
    private TextView lat;
    private TextView lon;
    private TextView ut;
    private TextView speed;
    private TextView trueHeading;

    private DataManager mBearingProvider;
    private ScheduledExecutorService exec;
    private boolean used = false;
    private Location lastLoc;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.compass_activity);

        image = findViewById(R.id.imageViewCompass);
        cHeading = findViewById(R.id.cHeading);
        tvHeading = findViewById(R.id.tvHeading);
        city = findViewById(R.id.city);
        country = findViewById(R.id.country);
        lat = findViewById(R.id.lat);
        lon = findViewById(R.id.lon);
        ut = findViewById(R.id.ut);
        speed = findViewById(R.id.speed);
        trueHeading = findViewById(R.id.trueHeading);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getBaseContext());

        if(!isNetworkAvailable())
        {
            city.setText("No network connection");
            country.setText("");
        }

        mBearingProvider = new DataManager(this);
        mBearingProvider.setChangeEventListener(this);

        //Set up admob
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        Runnable periodic = new Runnable() {
            @Override
            public void run() {
                used = false;
            }
        };

        exec = Executors.newScheduledThreadPool(1);
        exec.scheduleAtFixedRate(periodic, 0, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onTrueBearingChanged(double bearing)
    {
        int intVal = (int) ((bearing < 0) ? Math.floor(bearing + 360) : Math.floor(bearing));
        trueHeading.setText(intVal + "º" + getDirection(intVal));
    }

    @Override
    public void onMagneticBearingChanged(int heading)
    {
        setSensorData("rv", heading);
    }

    @Override
    public void onMagneticFieldChanged(double gmf)
    {
        if(!used) {
            setSensorData("m", (float)gmf);
            used = true;
        }
    }

    @Override
    public void onCurrentLocationChanged(Location loc)
    {
        getLocation(loc);
    }

    public void setCoordData(LatLng latLng, String[] coords, Address address, double spd)
    {
        String sUnits = "";
        String sUnitSystem = sharedPreferences.getString("speed", "Metric");
        String cUnitSystem = sharedPreferences.getString("coords", "Degrees");

        try
        {
            if(isNetworkAvailable() && !address.getLocality().isEmpty()) {
                city.setText(address.getLocality());
                country.setText(address.getCountryName());
            }
            else
            {
                city.setText("No network connection");
                country.setText("");
            }
        }
        catch(Exception e)
        {

        }

        if(cUnitSystem.equals("degrees"))
        {
            this.address = address;
            this.coords = coords;
            lat.setText("Lat. " + coords[0]);
            lon.setText("Lon. " + coords[1]);
        }
        else
        {
            lat.setText("Lat. " + latLng.latitude);
            lon.setText("Lon. " + latLng.longitude);
        }

        if(sUnitSystem.equals("metric"))
        {
            sUnits = "km/h";
        }
        else
        {
            sUnits = "mph";
            spd = spd / 1.60934; //km/h to mph
        }

        speed.setText(Math.round(spd) + sUnits);
    }

    public void setSensorData(String sensor, float val)
    {
        if(sensor.equals("rv")) {
            int intVal = (int) val;
            RotateAnimation ra = new RotateAnimation(
                    currentDegree,
                    -intVal,
                    Animation.RELATIVE_TO_SELF, 0.5f,
                    Animation.RELATIVE_TO_SELF,
                    0.5f);

            ra.setDuration(250);

            ra.setFillAfter(true);

            image.startAnimation(ra);
            currentDegree = -intVal;

            cHeading.setText(getDirection(intVal));

            tvHeading.setText(intVal + "º");
            currentDegree = -intVal;
        }
        else
        {

            ut.setText(String.format("%.1f μT", val));
        }
    }

    private String getDirection(int val)
    {
        String direction = "";

        if (val >= 337.5 || val < 22.5) {
            direction = "N";
        } else if (val >= 292.5 && val <= 337.25) {
            direction = "NW";
        } else if (val >= 247.5 && val <= 292.5) {
            direction = "W";
        } else if (val >= 202.5 && val <= 247.5) {
            direction = "SW";
        } else if (val >= 157.5 && val <= 202.5) {
            direction = "S";
        } else if (val >= 112.5 && val <= 157.5) {
            direction = "SE";
        } else if (val >= 67.5 && val <= 112.5) {
            direction = "E";
        } else if (val >= 22.5 && val <= 67.5) {
            direction = "NE";
        }

        return direction;
    }

    public void getLocation(Location loc)
    {
        try {
            if(loc != null) {
                LatLng latlng = new LatLng(loc.getLatitude(), loc.getLongitude());
                Address address = null;

                Geocoder gcd = new Geocoder(getBaseContext(), Locale.getDefault());
                List<Address> addresses;
                try {
                    addresses = gcd.getFromLocation(loc.getLatitude(),
                            loc.getLongitude(), 1);
                    if (addresses.size() > 0) {
                        address = addresses.get(0);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String[] coords = ddToDms(latlng.latitude, latlng.longitude).split(" ");
                double calculatedSpeed = 0.0;

                if (lastLoc != null) {
                    double elapsedTime = (loc.getTime() - lastLoc.getTime()) / 36000; // Convert milliseconds to hours
                    float distanceKm = lastLoc.distanceTo(loc) / 1000; //convert to km
                    calculatedSpeed = distanceKm / elapsedTime;
                }

                lastLoc = loc;

                double speed = loc.hasSpeed() ? loc.getSpeed() : calculatedSpeed;

                setCoordData(latlng, coords, address, speed);
            }
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public String ddToDms(double lat, double lng) {

        String latResult, lngResult, dmsResult;

        latResult = (lat >= 0)? "N" : "S";

        // Call to getDms(lat) function for the coordinates of Latitude in DMS.
        // The result is stored in latResult variable.
        latResult += getDms(lat);

        lngResult = (lng >= 0)? "E" : "W";

        // Call to getDms(lng) function for the coordinates of Longitude in DMS.
        // The result is stored in lngResult variable.
        lngResult += getDms(lng);

        // Joining both variables and separate them with a space.
        dmsResult = latResult + ' ' + lngResult;

        // Return the resultant string
        return dmsResult;
    }

    private String getDms(double val) {

        double valDeg, valMin, valSec;
        String result;

        val = Math.abs(val);

        valDeg = Math.floor(val);
        result = valDeg + "º";

        valMin = Math.floor((val - valDeg) * 60);
        result += valMin + "'";

        valSec = Math.round((val - valDeg - valMin / 60) * 3600 * 1000) / 1000;
        result += valSec + '"';

        return result;
    }

    public void onDetailsClick(View v)
    {
        Intent intent = new Intent(this, DetailsActivity.class);
        startActivity(intent);

    }

    public void onMapClick(View v) {
        new AlertDialog.Builder(this)
                .setTitle("Open Google Maps?")
                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                })
                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String geoUri = "http://maps.google.com/maps?";
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(geoUri ));
                        startActivity(intent);
                    }
                })
                .create()
                .show();
    }

    public void onSettingsClick(View v) {
        Intent settings = new Intent(getApplicationContext(), SettingsActivity.class);
        startActivity(settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBearingProvider.start();
        registerReceiver(networkStateReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

        getLocation(lastLoc);
    }

    @Override
    protected void onPause() {
        mBearingProvider.stop();
        unregisterReceiver(networkStateReceiver);
        super.onPause();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Forward results to EasyPermissions
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @AfterPermissionGranted(1)
    public void requestLocationPermission() {
        String[] perms = {Manifest.permission.ACCESS_FINE_LOCATION};
        if(!EasyPermissions.hasPermissions(this, perms)) {
            EasyPermissions.requestPermissions(this, "Please grant the location permission", 1, perms);
        }
    }

    private BroadcastReceiver networkStateReceiver=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            Handler handler = new Handler();

            final Runnable r = new Runnable() {
                public void run() {
                    getLocation(lastLoc);
                }
            };
            handler.postDelayed(r, 5000);
        }
    };
}