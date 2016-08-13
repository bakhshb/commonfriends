package com.example.xgc4811.myapp.receivers;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;
import android.widget.Toast;

import com.example.xgc4811.myapp.services.BluetoothService;
import com.example.xgc4811.myapp.helper.AppHelper;

/**
 * Created by bakhs on 20/07/2016.
 */
public class NetworksReceiver extends BroadcastReceiver {
    private static final String TAG="NetworksReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d("BroadcastActions", "Action "+action+" received");
        int state;
        BluetoothDevice bluetoothDevice;
        AppHelper mAppHelper = new AppHelper( context );
        switch(action)
        {
            case BluetoothAdapter.ACTION_STATE_CHANGED:
                state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                if (state == BluetoothAdapter.STATE_OFF)
                {
                    context.stopService( new Intent( context, BluetoothService.class ) );
                    Toast.makeText(context, "Bluetooth is off", Toast.LENGTH_SHORT).show();
                    Log.d("BroadcastActions", "Bluetooth is off");
                }
                else if(state == BluetoothAdapter.STATE_ON)
                {
                    Log.d("BroadcastActions", "Bluetooth is on");
                }
                break;

            case BluetoothDevice.ACTION_ACL_CONNECTED:
                bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(context, "Connected to "+ bluetoothDevice.getName(),
                        Toast.LENGTH_SHORT).show();
                Log.d("BroadcastActions", "Connected to "+ bluetoothDevice.getName());
                break;

            case BluetoothDevice.ACTION_ACL_DISCONNECTED:
                bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Toast.makeText(context, "Disconnected from "+bluetoothDevice.getName(),
                        Toast.LENGTH_SHORT).show();
                break;
        }

        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm.getActiveNetworkInfo() == null){
            Toast.makeText( context,"No Internet Connection", Toast.LENGTH_SHORT ).show();
        }
    }
}
