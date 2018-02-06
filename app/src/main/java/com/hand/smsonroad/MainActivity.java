package com.hand.smsonroad;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, PermissionDialog.GetPermissionListener {

    public static final int CODE_SETTINGS = 0;

    public static final String KEY_NAME = "Имя";
    public static final String KEY_PHONE = "Телефон";
    public static final String KEY_VEHICLE_ID = "Номер машины";
    public static final String KEY_VEHICLE_MARK = "Марка машины";

    public static final String PERMISSION_DIALOG = "PermissionDialog";

    private static final String DEFAULT_PHONE_NUMBER = "+35795112244";

    public static String fieldName;
    public static String fieldPhone;
    public static String fieldVehicleID;
    public static String fieldVehicleMark;

    public static double latitude;
    public static double longitude;

    private SharedPreferences sp;
    private FusedLocationProviderClient mFusedLocationClient;
    private Timer timer;

    Button btnPhone;
    Button btnCircle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadPreferences();
        btnPhone = findViewById(R.id.btn_phone);
        btnPhone.setText(fieldPhone);
        btnCircle = findViewById(R.id.circle);
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (timer!=null) timer.cancel();
        timer = new Timer();
        MyTimerTask timerTask = new MyTimerTask();
        timerTask.activity = this;
        timer.schedule(timerTask, 5000, 5000);
    }

    class MyTimerTask extends TimerTask {
        MainActivity activity;
        @Override
        public void run() {
            runOnUiThread(new Runnable() { @Override public void run() { activity.setLocation(); } });
        }
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.btn_settings:
                intent = new Intent(this, SettingsActivity.class);
                startActivityForResult(intent, CODE_SETTINGS);
                break;
            case R.id.btn_site:
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://newautolife.com/"));
                startActivity(intent);
                break;
            case R.id.btn_phone:
                intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + DEFAULT_PHONE_NUMBER));
                startActivity(intent);
                break;
            case R.id.circle:
                PermissionDialog fragment = new PermissionDialog();
                fragment.show(getFragmentManager(), PERMISSION_DIALOG);
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if ((data == null) || (resultCode == RESULT_CANCELED)) {
            return;
        }
        switch (requestCode) {
            case CODE_SETTINGS:
                fieldName = data.getStringExtra(KEY_NAME);
                fieldPhone = data.getStringExtra(KEY_PHONE);
                fieldVehicleID = data.getStringExtra(KEY_VEHICLE_ID);
                fieldVehicleMark = data.getStringExtra(KEY_VEHICLE_MARK);
                break;
        }
    }

    @Override
    public void onStop() {
        sp = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putString(KEY_NAME, fieldName);
        ed.putString(KEY_PHONE, fieldPhone);
        ed.putString(KEY_VEHICLE_ID, fieldVehicleID);
        ed.putString(KEY_VEHICLE_MARK, fieldVehicleMark);
        ed.apply();
        super.onStop();
    }

    @Override
    public void onGetPermissionFromDialog() {
        String template = getString(R.string.sms_template);
        String sms = String.format(template, latitude, longitude, fieldName, fieldPhone, fieldVehicleID, fieldVehicleMark);
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + DEFAULT_PHONE_NUMBER));
        intent.putExtra("sms_body", sms);
        startActivity(intent);
    }

    private void loadPreferences() {
        sp = getPreferences(MODE_PRIVATE);
        fieldName = sp.getString(KEY_NAME, "");
        fieldPhone = sp.getString(KEY_PHONE, "");
        fieldVehicleID = sp.getString(KEY_VEHICLE_ID, "");
        fieldVehicleMark = sp.getString(KEY_VEHICLE_MARK, "");
    }

    private void setLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return;
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        if (location != null) {
                            latitude = location.getLatitude();
                            longitude = location.getLongitude();
                            btnCircle.setEnabled(true);
                        }
                    }
                });
    }

}
