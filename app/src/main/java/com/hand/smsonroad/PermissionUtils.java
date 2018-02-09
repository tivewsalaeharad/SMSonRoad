package com.hand.smsonroad;

import android.Manifest;
import android.app.Activity;
import android.support.v4.app.ActivityCompat;

class PermissionUtils {

    static final int CODE_PERMISSION_LOCATION = 0;

    private static String[] PERMISSIONS_LOCATION = {Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION};

    static boolean shouldAskLocationPermission(Activity activity) {
        return ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    static void requestLocationPermissions(Activity activity) {
        ActivityCompat.requestPermissions(activity, PERMISSIONS_LOCATION, CODE_PERMISSION_LOCATION);
    }

}
