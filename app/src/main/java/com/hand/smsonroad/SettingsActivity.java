package com.hand.smsonroad;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;

public class SettingsActivity extends AppCompatActivity implements View.OnClickListener {

    EditText editName;
    EditText editPhone;
    EditText editVehicleID;
    EditText editVehicleMark;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        editName = findViewById(R.id.name);
        editPhone = findViewById(R.id.phone);
        editVehicleID = findViewById(R.id.vehicle_id);
        editVehicleMark = findViewById(R.id.vehicle_mark);
        getSettings();
    }

    @Override
    public void onClick(View v) {
        Intent intent = new Intent();
        switch (v.getId()) {
            case R.id.btn_ok:
                intent.putExtra(MainActivity.KEY_NAME, editName.getText().toString());
                intent.putExtra(MainActivity.KEY_PHONE, editPhone.getText().toString());
                intent.putExtra(MainActivity.KEY_VEHICLE_ID, editVehicleID.getText().toString());
                intent.putExtra(MainActivity.KEY_VEHICLE_MARK, editVehicleMark.getText().toString());
                setResult(RESULT_OK, intent);
                finish();
                return;
            case R.id.btn_cancel:
                setResult(RESULT_CANCELED, intent);
                finish();
        }
    }

    private void getSettings() {
        editName.setText(MainActivity.fieldName);
        editPhone.setText(MainActivity.fieldPhone);
        editVehicleID.setText(MainActivity.fieldVehicleID);
        editVehicleMark.setText(MainActivity.fieldVehicleMark);
    }
}
