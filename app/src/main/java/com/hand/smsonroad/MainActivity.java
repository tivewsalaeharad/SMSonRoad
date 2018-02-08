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

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int CODE_SETTINGS = 0;
    public static final int CODE_PERMISSION = 1;

    public static final String KEY_FIRST_START = "Первый запуск";
    public static final String KEY_NAME = "Имя";
    public static final String KEY_PHONE = "Телефон";
    public static final String KEY_VEHICLE_ID = "Номер машины";
    public static final String KEY_VEHICLE_MARK = "Марка машины";
    public static final String KEY_VEHICLE_MODEL = "Модель машины";

    public static final String KEY_SMS_BODY = "sms_body";

    private static final String SMS_TO_DEFAULT_PHONE_NUMBER = "smsto:+35795112244";
    private static final String DIAL_DEFAULT_PHONE_NUMBER = "tel:+35795112244";

    private static Boolean firstStart;

    public static String fieldName;
    public static String fieldPhone;
    public static String fieldVehicleID;
    public static String fieldVehicleMark;
    public static String fieldVehicleModel;

    public static double latitude;
    public static double longitude;

    private SharedPreferences sp;
    private FusedLocationProviderClient mFusedLocationClient;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        loadPreferences();
        if (firstStart) {
            openSettings();
            firstStart = false;
        }
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
                openSettings();
                break;
            case R.id.btn_site:
                intent = new Intent(this, WebActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_phone:
                intent = new Intent(Intent.ACTION_DIAL, Uri.parse(DIAL_DEFAULT_PHONE_NUMBER));
                startActivity(intent);
                break;
            case R.id.circle:
                intent = new Intent(this, PermissionActivity.class);
                startActivityForResult(intent, CODE_PERMISSION);
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
                fieldVehicleModel = data.getStringExtra(KEY_VEHICLE_MODEL);
                break;
            case CODE_PERMISSION:
                String varName = data.getStringExtra(KEY_NAME);
                String varPhone = data.getStringExtra(KEY_PHONE);
                String varVehicleID = data.getStringExtra(KEY_VEHICLE_ID);
                String varVehicleMark = data.getStringExtra(KEY_VEHICLE_MARK);
                String varVehicleModel = data.getStringExtra(KEY_VEHICLE_MODEL);
                String template = getString(R.string.sms_template);
                String sms = String.format(Locale.US, template, latitude, longitude, longitude, latitude, longitude, latitude, varName, varPhone, varVehicleID, varVehicleMark, varVehicleModel);
                Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse(SMS_TO_DEFAULT_PHONE_NUMBER));
                intent.putExtra(KEY_SMS_BODY, sms);
                startActivity(intent);
                break;
        }
    }

    @Override
    public void onStop() {
        sp = getPreferences(MODE_PRIVATE);
        SharedPreferences.Editor ed = sp.edit();
        ed.putBoolean(KEY_FIRST_START, firstStart);
        ed.putString(KEY_NAME, fieldName);
        ed.putString(KEY_PHONE, fieldPhone);
        ed.putString(KEY_VEHICLE_ID, fieldVehicleID);
        ed.putString(KEY_VEHICLE_MARK, fieldVehicleMark);
        ed.putString(KEY_VEHICLE_MODEL, fieldVehicleModel);
        ed.apply();
        super.onStop();
    }

    private void loadPreferences() {
        sp = getPreferences(MODE_PRIVATE);
        firstStart = sp.getBoolean(KEY_FIRST_START, true);
        fieldName = sp.getString(KEY_NAME, "");
        fieldPhone = sp.getString(KEY_PHONE, "");
        fieldVehicleID = sp.getString(KEY_VEHICLE_ID, "");
        fieldVehicleMark = sp.getString(KEY_VEHICLE_MARK, "");
        fieldVehicleModel = sp.getString(KEY_VEHICLE_MODEL, "");
    }

    private void openSettings() {
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivityForResult(intent, CODE_SETTINGS);
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
                            findViewById(R.id.circle).setEnabled(true);
                        }
                    }
                });
    }

}
