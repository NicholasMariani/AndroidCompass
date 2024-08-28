package com.admin.androidcompass.main;

import android.hardware.Sensor;
import android.location.Location;
import android.os.Bundle;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.admin.androidcompass.DataManager;
import com.admin.androidcompass.DetailsActivity;
import com.admin.androidcompass.R;

public class CalibrationFragment extends Fragment implements DataManager.ChangeEventListener {

    private DetailsActivity context;
    private View inf;
    private DataManager dm;
    private ProgressBar pb;
    private TextView accel;
    private TextView mag;
    private TextView mf;


    public CalibrationFragment(DetailsActivity context)
    {
        super();
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true);

        inf = inflater.inflate(R.layout.fragment_calibration, container, false);

        accel = inf.findViewById(R.id.accel_info);
        mag = inf.findViewById(R.id.mag_info);
        pb = inf.findViewById(R.id.prog);
        mf = inf.findViewById(R.id.mf_info);

        pb.setRotation(270);

        dm = new DataManager(this.context);
        dm.setChangeEventListener(this);

        return inf;
    }

    @Override
    public void onResume() {
        super.onResume();
        dm.start();
    }

    @Override
    public void onPause() {
        dm.stop();
        super.onPause();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_location, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_location) {
            Toast.makeText(getActivity(), "Clicked on " + item.getTitle(), Toast.LENGTH_SHORT)
                    .show();
        }
        return true;
    }

    @Override
    public void onTrueBearingChanged(double bearing) {

    }

    @Override
    public void onMagneticBearingChanged(int bearing) {

    }

    @Override
    public void onMagneticFieldChanged(double geoMagneticField) {
        String acc = dm.getAccuracyMap().get(Sensor.TYPE_ACCELEROMETER);
        String magnet = dm.getAccuracyMap().get(Sensor.TYPE_MAGNETIC_FIELD);

        if(acc != null) {
            accel.setText(Html.fromHtml(acc));
        }

        if(magnet != null) {
            mag.setText(Html.fromHtml(magnet));
        }

        pb.setProgress((int)geoMagneticField, true);

        mf.setText(String.format("%.1f μT", geoMagneticField));
    }

    @Override
    public void onCurrentLocationChanged(Location loc) {

    }
}