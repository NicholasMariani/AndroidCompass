package com.admin.androidcompass.main;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.admin.androidcompass.DataManager;
import com.admin.androidcompass.DetailsActivity;
import com.admin.androidcompass.R;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class LocationFragment extends Fragment implements DataManager.ChangeEventListener{

    private DetailsActivity context;
    private DataManager dm;
    private Location lastLoc;
    private View inf;
    private TextView lat;
    private TextView lon;
    private TextView addr;
    private TextView alt;
    private TextView tbrg;
    private TextView mbrg;
    private TextView magInc;
    private TextView magDec;
    private TextView lastUpdate;

    public LocationFragment(DetailsActivity context)
    {
        super();
        this.context = context;
        dm = new DataManager(this.context);
        dm.setChangeEventListener(this);
    }

    @Override
    public void onTrueBearingChanged(double bearing)
    {
        int intVal = (int) ((bearing < 0) ? Math.floor(bearing + 360) : Math.floor(bearing));
        tbrg.setText(intVal + "º");
    }

    @Override
    public void onMagneticBearingChanged(int heading)
    {
        mbrg.setText(heading + "º");
    }

    @Override
    public void onMagneticFieldChanged(double gmf)
    {
        if(lastLoc != null) {
            magInc.setText(dm.getGeomagneticField(lastLoc).getInclination() + "º");
            magDec.setText(dm.getGeomagneticField(lastLoc).getDeclination() + "º");
        }
    }

    @Override
    public void onCurrentLocationChanged(Location loc)
    {
        getLocation(loc);
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
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true);

        inf = inflater.inflate(R.layout.fragment_location, container, false);

        lat = inf.findViewById(R.id.lat_info);
        lon = inf.findViewById(R.id.lon_info);
        addr = inf.findViewById(R.id.address_info);
        alt = inf.findViewById(R.id.altitude_info);
        tbrg = inf.findViewById(R.id.tbearing_info);
        mbrg = inf.findViewById(R.id.mbearing_info);
        magDec = inf.findViewById(R.id.mag_dec_info);
        magInc = inf.findViewById(R.id.mag_inc_info);
        lastUpdate = inf.findViewById(R.id.last_update_info);

        return inf;
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

    private void getLocation(Location loc)
    {
        lat.setText(loc.getLatitude() + "");
        lon.setText(loc.getLongitude() + "");

        Address address = null;

        Geocoder gcd = new Geocoder(context, Locale.getDefault());
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

        if(address != null) {
            addr.setText(address.getAddressLine(0));
        }

        alt.setText(loc.getAltitude() + " m");

        lastLoc = loc;

        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yy @ HH:mm");
        Date dt = new Date(loc.getTime());
        String date = sdf.format(dt);
        lastUpdate.setText(date);
    }
}