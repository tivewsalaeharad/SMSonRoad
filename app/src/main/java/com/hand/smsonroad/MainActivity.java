package com.hand.smsonroad;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener,
        ActivityCompat.OnRequestPermissionsResultCallback,
        PermissionDialog.GetPermissionListener{

    public static final int CODE_SETTINGS = 0;
    public static final int CODE_PERMISSION = 1;

    public static final String KEY_FIRST_START = "First start";
    public static final String KEY_NAME = "Name";
    public static final String KEY_PHONE = "Phone";
    public static final String KEY_VEHICLE_ID = "Vehicle number";
    public static final String KEY_VEHICLE_MARK = "Vehicle mark";
    public static final String KEY_VEHICLE_MODEL = "Vehicle model";
    public static final String KEY_SMS_BODY = "sms_body";

    public static final String TAG_PERMISSION_DIALOG = "PermissionDialog";

    public static final int LOCATION_UPDATE_FREQUENCY = 5000;

    private static final String SMS_TO_DEFAULT_PHONE_NUMBER = "sms:+35795112244";
    private static final String DIAL_DEFAULT_PHONE_NUMBER = "tel:+35795112244";
    //private static final String template_reserved = "NEED HELP ON ROAD!\nGoogle location: https://www.google.com/maps?daddr=%f,%f\n2GIS location: https://2gis.ru/geo/%f,%f?queryState=center/%f,%f/zoom/16\nName: %s\nPhone: %s\nReg. number: %s\nBrand: %s\nModel: %s";
    private static final String template = "NEED HELP ON ROAD!\nGoogle location: https://www.google.com/maps?daddr=%f,%f\n2GIS location: dgis://2gis.ru/routeSearch/rsType/car/to/%f,%f\nName: %s\nPhone: %s\nReg. number: %s\nBrand: %s\nModel: %s";

    private static Boolean firstStart;
    private static Boolean gpsEnabled;

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
    private View mainLayout;
    private ProgressBar progressBar;
    private Button circle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainLayout = findViewById(R.id.main_layout);
        progressBar = findViewById(R.id.progress_bar);
        circle = findViewById(R.id.circle);
        loadPreferences();
        if (firstStart) {
            openSettings();
            firstStart = false;
        }
        gpsEnabled = checkGPSEnabled();
        if(!gpsEnabled) {
            PermissionDialog fragment = new PermissionDialog();
            fragment.show(getFragmentManager(), TAG_PERMISSION_DIALOG);
        }
        else waitMessage();
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        if (timer != null) timer.cancel();
        timer = new Timer();
        MyTimerTask timerTask = new MyTimerTask();
        timerTask.activity = this;
        timer.schedule(timerTask, LOCATION_UPDATE_FREQUENCY, LOCATION_UPDATE_FREQUENCY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            if (gpsChanged()) {
                gpsEnabled = !gpsEnabled;
                if (gpsEnabled) waitMessage();
            }
            if (!gpsEnabled) gpsTurnedOffMessage();
        }
    }

    @Override
    public void onGetPermissionFromDialog(boolean confirmed) {
        if (confirmed) startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
        else gpsTurnedOffMessage();
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
                String sms = String.format(Locale.US, template, latitude, longitude, longitude, latitude, varName, varPhone, varVehicleID, varVehicleMark, varVehicleModel);
                //sendSMS(DEFAULT_PHONE_NUMBER, sms);
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionUtils.CODE_PERMISSION_LOCATION) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Snackbar.make(mainLayout, "Location permission was granted.", Snackbar.LENGTH_SHORT) .show();
                setLocation();
            } else {
                Snackbar.make(mainLayout, "Location permission request was denied.", Snackbar.LENGTH_SHORT).show();
            }
        }
    }

    class MyTimerTask extends TimerTask {

        MainActivity activity;
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(gpsEnabled) activity.setLocation();
                    else {
                        progressBar.setVisibility(View.GONE);
                        circle.setEnabled(false);
                    }
                }
            });
        }
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissionWithExplanation();
            return;
        }
        mFusedLocationClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    latitude = location.getLatitude();
                    longitude = location.getLongitude();
                    circle.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                } else {
                    circle.setEnabled(false);
                    progressBar.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void requestLocationPermissionWithExplanation() {
        if (PermissionUtils.shouldAskLocationPermission(this)) {
            Snackbar.make(mainLayout, "Required access to determine location",
                    Snackbar.LENGTH_INDEFINITE).setAction(R.string.str_ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            PermissionUtils.requestLocationPermissions(MainActivity.this);
                        }
                    }).show();
        } else {
            PermissionUtils.requestLocationPermissions(this);
        }
    }

    private boolean checkGPSEnabled(){
        String provider = Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
        return provider.contains("gps");
    }

    private boolean gpsChanged() {
        return gpsEnabled != checkGPSEnabled();
    }

    private void waitMessage() {
        Snackbar.make(mainLayout, R.string.wait_message, Snackbar.LENGTH_LONG).setAction(R.string.str_ok, null).show();
        progressBar.setVisibility(View.VISIBLE);
    }

    private void gpsTurnedOffMessage() {
        Snackbar.make(mainLayout, R.string.gps_turned_off, Snackbar.LENGTH_SHORT).setAction(R.string.str_ok, null).show();
        circle.setEnabled(false);
        progressBar.setVisibility(View.GONE);
    }

}




