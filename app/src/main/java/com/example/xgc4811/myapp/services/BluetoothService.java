package com.example.xgc4811.myapp.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.xgc4811.myapp.helper.CustomRequestQueue;
import com.example.xgc4811.myapp.R;
import com.example.xgc4811.myapp.activities.ResultsActivity;
import com.example.xgc4811.myapp.helper.AppHelper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Created by bakhs on 20/07/2016.
 */
public class BluetoothService extends Service {

    private static final String TAG = "BluetoothService";
    private static final String BLUETOOTH_SEARCH_URL = "http://bakhshb.pythonanywhere.com/api/bluetooth/search/";
    // Handler
    private static final int SEND_REQUEST = 1;
    private static final int SEND_RESULTS_ALREADY_FRIEND = 2;
    private static final int SEND_RESULTS_MUTUAL_FRIEND = 3;
    private static final int SEND_RESULTS_SUGGEST_FRIEND=4;

    //Response friend status
    private static final int NO_MATCH = 0;
    private static final int ALREADY_FRIEND = 1;
    private static final int FOUND_MUTUAL = 2;
    private static final int SUGGEST_FRIEND=3;

    private BluetoothAdapter mBluetoothAdapter;
    private IntentFilter mIntentFilter;
    private AppHelper mAppHelper;

    private Set<String> devicesCommonFriends;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mAppHelper = new AppHelper( getApplicationContext() );
        devicesCommonFriends = new HashSet<String>(  );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i( TAG, "onStartCommand: Bluetooth Service Started" );
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
        mIntentFilter = new IntentFilter( BluetoothDevice.ACTION_FOUND );

        getApplicationContext().registerReceiver(mBroadcastReceiver,mIntentFilter );
        return super.onStartCommand( intent, flags, startId );
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getApplicationContext().unregisterReceiver( mBroadcastReceiver );
        NotificationManager mNotificationManager = mNotificationManager();
        mNotificationManager.cancelAll();
        Log.i( TAG, "onDestroy: Bluetooth Service Stopped" );
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            BluetoothDevice device = null;
            if (BluetoothDevice.ACTION_FOUND.equals( action )){
                device= intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
                int  rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE);
                // Devices not paired
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    // Devices found in the server
                    Log.i( TAG, "onReceive:  RSSI: " + rssi + "dBm " + device.getAddress() + " " + device.getName() );
                    Message mMessage = mHandler.obtainMessage( SEND_REQUEST );
                    Bundle mBundle = new Bundle();
                    mBundle.putString( "bluetooth_address", device.getAddress() );
                    mBundle.putInt( "rssi", rssi );
                    mMessage.setData( mBundle );
                    mHandler.sendMessage( mMessage );

                }
            }
        }
    };


    private void sendNotification(String msg) {
        NotificationManager mNotificationManager = mNotificationManager();
        Intent mIntent = new Intent( this.getApplicationContext(), ResultsActivity.class );
        Bundle mBundle = new Bundle(  );
        mBundle.putString( "msg", msg );
        mIntent.putExtras( mBundle );


        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                mIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon( R.drawable.ic_stat_name)
                        .setContentTitle("Bluetooth Device Next To You")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg)
                        .setDefaults( Notification.DEFAULT_SOUND ).setAutoCancel( true );
        mBuilder.setContentIntent(contentIntent);
        Random random = new Random();
        int m = random.nextInt(9999 - 1000) + 1000;
        mNotificationManager.notify(m, mBuilder.build());
    }

    private NotificationManager mNotificationManager (){
       return (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    // calculate bluetooth distance
    private String calculateDistance (int rssi){
        if (rssi > -45){
            return "0 Meter";
        } else if (-45 > rssi && rssi >= -47){
            return "1 Meter";
        } else if (-47 > rssi && rssi >= -51){
            return "2 Meters";
        } else if(-51 > rssi && rssi >= -54){
            return "3 Meters";
        } else if (-54 > rssi && rssi >= -58){
            return "4 Meters";
        } else if (-58 > rssi && rssi >= -61){
            return "5 Meters";
        }
        return "more than 5 Meters";
    }

    private void bluetoothSearchRequest(final String bluetooth_address, final int rssi){
        HashMap<String, String > user = mAppHelper.getUserDetails();
        final String token = user.get( AppHelper.USER_TOKEN );
        final String currentDevice = user.get( AppHelper.BLUETOOTH_ADDRESS );
        //
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("bluetooth", bluetooth_address);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest( Request.Method.POST, BLUETOOTH_SEARCH_URL, new JSONObject( params ), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    int count = 0;
                    Log.d( TAG, "onResponse: " + response.getString( "status" ) +" " + response.getInt( "friend_status") );
                    if (response.getInt( "friend_status" ) == ALREADY_FRIEND){
                        Message mMessage = mHandler.obtainMessage(SEND_RESULTS_ALREADY_FRIEND);
                        Bundle mBundle = new Bundle(  );
                        mBundle.putString( "user", response.getString( "user" ) );
                        mBundle.putInt( "rssi", rssi );
                        mMessage.setData( mBundle );
                        mHandler.sendMessage( mMessage );
                    }else if (response.getInt( "friend_status" ) == FOUND_MUTUAL) {
                        JSONArray mJsonArray = response.getJSONArray( "friend" );
                        for (int i = 0; i < mJsonArray.length(); i++) {
                            count++;
                        }

                        Message mMessage = mHandler.obtainMessage(SEND_RESULTS_MUTUAL_FRIEND);
                        Bundle mBundle = new Bundle(  );
                        mBundle.putString( "user", response.getString( "user" ) );
                        mBundle.putInt( "count", count );
                        mBundle.putInt( "rssi", rssi );
                        mMessage.setData( mBundle );
                        mHandler.sendMessage( mMessage );
                        if (!devicesCommonFriends.contains(bluetooth_address)) {
                            devicesCommonFriends.add("Found " + count + " Mutual Friend with Bluetooth Address " + bluetooth_address + " Name " + response.getString("user"));
                            mAppHelper.setCommonFriends(devicesCommonFriends);
                        }
                    } else if (response.getInt( "friend_status" ) == SUGGEST_FRIEND){
                        Message mMessage = mHandler.obtainMessage(SEND_RESULTS_SUGGEST_FRIEND);
                        Bundle mBundle = new Bundle(  );
                        mBundle.putString( "user", response.getString( "user" ) );
                        mBundle.putInt( "rssi", rssi );
                        mMessage.setData( mBundle );
                        mHandler.sendMessage( mMessage );
                    }


                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d( TAG, "onErrorResponse: " +  bluetooth_address);
            }
        } ){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization","Token "+ token);
                headers.put("Content-Type","application/json");
                return headers;
            }
        };
        jsonObjectRequest.setRetryPolicy( new DefaultRetryPolicy( 3000,DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT ) );
        CustomRequestQueue.getInstance( getApplicationContext() ).addToRequestQueue(jsonObjectRequest);
    }

    private final Handler mHandler = new Handler(  ){
        @Override
        public void handleMessage(Message msg) {
            String user = null;
            int rssi = 0;
            switch (msg.what){
                case SEND_REQUEST:
                    bluetoothSearchRequest( msg.getData().getString( "bluetooth_address" ), msg.getData().getInt( "rssi" ) );
                    break;
                case SEND_RESULTS_ALREADY_FRIEND:
                    user = msg.getData().getString( "user" );
                    rssi = msg.getData().getInt( "rssi" );
                    Log.d( TAG, "handleMessage: " + rssi );
                    Log.d( TAG, "handleMessage: " + user );
                    sendNotification( "Your Friend "+ user + "\n Proximate Distance: " + calculateDistance( rssi ) );
                    break;
                case SEND_RESULTS_MUTUAL_FRIEND:
                    user = msg.getData().getString( "user" );
                    int count = msg.getData().getInt( "count" );
                    rssi = msg.getData().getInt( "rssi" );
                    Log.d( TAG, "handleMessage: " + rssi );
                    Log.d( TAG, "handleMessage: " + user );
                    if (count <=1) {
                        sendNotification( "You have  " + count + " mutual friend with " + user + "\n Proximate Distance: " + calculateDistance( rssi ) );
                    }else{
                        sendNotification( "You have  " + count + " mutual friends with " + user + "\n Proximate Distance: " + calculateDistance( rssi ) );
                    }
                    break;
                case SEND_RESULTS_SUGGEST_FRIEND:
                    user = msg.getData().getString( "user" );
                    rssi = msg.getData().getInt( "rssi" );
                    Log.d( TAG, "handleMessage: " + rssi );
                    Log.d( TAG, "handleMessage: " + user );
                    sendNotification( "Friend Suggestion "+ user + "\n Proximate Distance: " + calculateDistance( rssi ) );
                    break;
            }
        }
    };

}
