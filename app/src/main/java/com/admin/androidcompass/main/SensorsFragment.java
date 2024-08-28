package com.admin.androidcompass.main;

import static android.content.Context.SENSOR_SERVICE;
import static android.graphics.Typeface.SERIF;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.GridLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;

import com.admin.androidcompass.DetailsActivity;
import com.admin.androidcompass.R;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SensorsFragment extends Fragment implements SensorEventListener {

    private DetailsActivity context;
    private SensorManager sensorManager;
    private ScrollView scrollView;
    private ConstraintLayout cl;
    private GridLayout gridLayout;
    private RelativeLayout relativeLayout;
    private List<Sensor> sensors;
    private Map<Integer, String> accMap;

    public SensorsFragment(DetailsActivity context)
    {
        super();
        this.context = context;

        sensors = new LinkedList<>();
        accMap = new HashMap<>();
        sensorManager = (SensorManager) this.context.getSystemService(SENSOR_SERVICE);

        for (Sensor value : sensorManager.getSensorList(Sensor.TYPE_ALL)) {
            if(sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(value.getType()),
                    SensorManager.SENSOR_DELAY_UI))
            {
                sensors.add(value);
                accMap.put(value.getId(), "No Data");
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        setHasOptionsMenu(true);

        View inf = inflater.inflate(R.layout.fragment_sensors, container, false);

        createComponents(inf);

        return inf;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void createComponents(View inf)
    {
        scrollView = inf.findViewById(R.id.scrollview);
        scrollView.setBackgroundColor(Color.BLACK);
        scrollView.setLayoutParams(new ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT));

//        cl = new ConstraintLayout(inf.getContext());
//        cl.setLayoutParams(new Constraints.LayoutParams(Constraints.LayoutParams.MATCH_PARENT,
//                Constraints.LayoutParams.MATCH_PARENT));

        TextView title = new TextView(inf.getContext());
        title.setId(99);
        title.setText("SENSOR INFORMATION");
        title.setLayoutParams(new WindowManager.LayoutParams(ViewPager.LayoutParams.WRAP_CONTENT,
            ViewPager.LayoutParams.WRAP_CONTENT));
        title.setGravity(Gravity.LEFT);
        title.setTextSize(24f);
        title.setPadding(16, 24, 0,16);
        title.setTextColor(Color.WHITE);
        title.setTypeface(SERIF);

        gridLayout = new GridLayout(inf.getContext());
        gridLayout.setLayoutParams(new WindowManager.LayoutParams(ViewPager.LayoutParams.MATCH_PARENT,
                ViewPager.LayoutParams.WRAP_CONTENT));
        gridLayout.setColumnCount(2);

        for (int i = 0; i < sensors.size(); i++) {
            RelativeLayout relativeLayout = new RelativeLayout(inf.getContext());
            relativeLayout.setId(i + 100);
            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(GridLayout.spec(
                    GridLayout.UNDEFINED, GridLayout.FILL, 1f),
                    GridLayout.spec(GridLayout.UNDEFINED, GridLayout.FILL, 1f));
            layoutParams.height = dpToPx(150);
            layoutParams.width = dpToPx(100);
            layoutParams.setMargins(dpToPx(5), dpToPx(10), dpToPx(5), 0);
            relativeLayout.setLayoutParams(layoutParams);
            relativeLayout.setHorizontalGravity(Gravity.CENTER_HORIZONTAL);
            relativeLayout.setVerticalGravity(Gravity.CENTER_VERTICAL);
            relativeLayout.setBackgroundColor(Color.DKGRAY);

            TextView header = new TextView(inf.getContext());
            header.setId(i * -27);
            header.setText(sensors.get(i).getName() + "\n\n\n Vendor: " +sensors.get(i).getVendor());
            header.setLayoutParams(new WindowManager.LayoutParams(ViewPager.LayoutParams.WRAP_CONTENT,
                    ViewPager.LayoutParams.WRAP_CONTENT));
            header.setGravity(Gravity.CENTER_HORIZONTAL | Gravity.TOP);
            header.setPadding(0, 20, 0, 0);
            header.setTextSize(16f);
            header.setTextColor(Color.WHITE);
            header.setTypeface(SERIF);

            TextView accTb = new TextView(inf.getContext());
            accTb.setId(sensors.get(i).getType());
            accTb.setLayoutParams(new WindowManager.LayoutParams(ViewPager.LayoutParams.WRAP_CONTENT,
                    ViewPager.LayoutParams.WRAP_CONTENT));
            accTb.setText("Accuracy: " + accMap.get(sensors.get(i).getId()));
            accTb.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            accTb.setPadding(0, 0, 0, 20);
            accTb.setTextSize(16f);
            accTb.setTextColor(Color.WHITE);
            accTb.setTypeface(SERIF);

            relativeLayout.addView(header);
            relativeLayout.addView(accTb);

            gridLayout.addView(relativeLayout);
        }

        relativeLayout = inf.findViewById(R.id.rel);
        relativeLayout.addView(title);
        RelativeLayout.LayoutParams rlp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        rlp.addRule(RelativeLayout.BELOW, title.getId());
        relativeLayout.addView(gridLayout, rlp);

//        cl.addView(gridLayout);
//
//        scrollView.addView(cl);
    }

    public int dpToPx(int dp) {
        float density = context.getResources()
                .getDisplayMetrics()
                .density;
        return Math.round((float) dp * density);
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
    public void onSensorChanged(SensorEvent event) {

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
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

        System.out.println(sensor.getName() + " " + acc);

        TextView accTb = context.findViewById(sensor.getType());
        accTb.setText(Html.fromHtml("Accuracy: " + acc));

    }
}