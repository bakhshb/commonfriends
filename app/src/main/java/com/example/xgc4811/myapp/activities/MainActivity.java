package com.example.xgc4811.myapp.activities;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;

import com.example.xgc4811.myapp.fragments.LoginFragment;
import com.example.xgc4811.myapp.R;

public class MainActivity extends AppCompatActivity {

    // Request Location permission
    private static final int PERMISSION_REQUEST_CODE = 1;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView( R.layout.activity_main);
        if(savedInstanceState == null){
            LoginFragment fb_login = new LoginFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.container_layout,fb_login,"FB").commit();
        }

        // Check Location
        if (!checkPermission()){
            requestPermission();
        }
    }

    private boolean checkPermission(){
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION);
        if (result == PackageManager.PERMISSION_GRANTED){
            return true;
        } else {
            return false;
        }
    }

    private void requestPermission(){
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.ACCESS_COARSE_LOCATION)){
            Toast.makeText(getApplicationContext(),"GPS permission allows us to access location data. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText( getApplicationContext(), "Permission Granted, Now you can access location data", Toast.LENGTH_SHORT ).show();
                } else {
                    Toast.makeText( getApplicationContext(), "Permission Denied, You cannot access location data", Toast.LENGTH_SHORT ).show();
                }
                break;
        }
    }
}
