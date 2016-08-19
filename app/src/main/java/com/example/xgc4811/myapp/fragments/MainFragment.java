package com.example.xgc4811.myapp.fragments;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.xgc4811.myapp.R;
import com.example.xgc4811.myapp.activities.LoginActivity;
import com.example.xgc4811.myapp.activities.MainActivity;
import com.example.xgc4811.myapp.helper.AppHelper;
import com.example.xgc4811.myapp.helper.CustomRequestQueue;
import com.example.xgc4811.myapp.services.BluetoothService;
import com.facebook.Profile;
import com.facebook.login.LoginManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class MainFragment extends Fragment implements View.OnClickListener {

    private static final String TAG = "MainFragment";
    // API
    private static final String LOGOUT_URL = "http://bakhshb.pythonanywhere.com/api/logout/facebook/";
    private static final String BLUETOOTH_USER_URL = "http://bakhshb.pythonanywhere.com/api/bluetooth/user/";
    private AppHelper mAppHelper;
    //Fragment
    UserAccountFragment userAccountFragment = null;

    private TextView mTextView;
    private Button mButtonTurnDiscovery;
    private Button mButtonCommonFriends;
    private Button mButtonAccount;
    private Button mButtonLogout;

    public MainFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate( R.layout.fragment_main, container, false );
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated( view, savedInstanceState );
        init( view );
        if (!mAppHelper.isLoggedIn()){
            bluetoothService( false );
            turnBluetooth( false );
            Intent mIntentMainAcivity = new Intent( this.getContext(), MainActivity.class );
            startActivity( mIntentMainAcivity );
            getActivity().finish();
        }else{
            BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
            if(bAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                // device is discoverable & connectable
            } else {
                // device is not discoverable & connectable
                // Making the device discoverable
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                enableBtIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                startActivityForResult(enableBtIntent, 0);
                bluetoothUserRequest();
                bluetoothService( true );
            }
        }

    }

    public void init (View view){
        mAppHelper = new AppHelper( getContext() );

        mTextView = (TextView) view.findViewById(R.id.user_detail);
        mButtonTurnDiscovery = (Button) view.findViewById(R.id.button_turndiscovery);
        mButtonTurnDiscovery.setOnClickListener( this );
        mButtonCommonFriends = (Button) view.findViewById(R.id.button_commonfriends);
        mButtonCommonFriends.setOnClickListener(this);
        mButtonAccount = (Button) view.findViewById(R.id.button_account1);
        mButtonAccount.setOnClickListener(this);
        mButtonLogout = (Button) view.findViewById(R.id.button_logout);
        mButtonLogout.setOnClickListener( this );
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_turndiscovery:
                BluetoothAdapter bAdapter = BluetoothAdapter.getDefaultAdapter();
                if(bAdapter.getScanMode() == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
                    // device is discoverable & connectable
                    turnBluetooth( false );
                    bluetoothService( false );
                } else {
                    // device is not discoverable & connectable
                    // Making the device discoverable
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    enableBtIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                    startActivityForResult(enableBtIntent, 0);

                    bluetoothService( true );
                }
                break;
            case R.id.button_commonfriends:

                break;
            case R.id.button_account1:
                if (userAccountFragment !=null) {
                    getFragmentManager().beginTransaction().replace(R.id.container_layout, userAccountFragment).addToBackStack(null).commit();
                }
                break;
            case R.id.button_logout:
                turnBluetooth( false );
                bluetoothService( false );
                LoginManager.getInstance().logOut();
                logoutRequest();
                break;
        }
    }

    private void bluetoothService(boolean bluetoothStatus){
        // Start bluetooth service using AlarmManager
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.SECOND, 10 );
        Intent mintent = new Intent( getContext(), BluetoothService.class );
        PendingIntent pintent = PendingIntent.getService( getContext(), 0, mintent, PendingIntent.FLAG_UPDATE_CURRENT );
        AlarmManager alarm = (AlarmManager) getContext().getSystemService( Context.ALARM_SERVICE );
        if (bluetoothStatus) {
            //for 30 mint 60*60*1000
            alarm.setRepeating( AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 120 * 1000, pintent );
            // bluetoothUserRequest();
        }else if (!bluetoothStatus){
            alarm.cancel( pintent );
            getContext().stopService( new Intent( getContext(), BluetoothService.class ) );
        }
    }

    private void bluetoothUserRequest(){
        HashMap<String, String > user = mAppHelper.getUserDetails();
        final String token = user.get( AppHelper.USER_TOKEN );
        final String bluetooth_address = user.get( AppHelper.BLUETOOTH_ADDRESS );
        Log.d( TAG, "bluetoothUserRequest: " + bluetooth_address );
        //
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("user_bluetooth", bluetooth_address);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest( Request.Method.POST, BLUETOOTH_USER_URL, new JSONObject( params ), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    Log.d( TAG, "onResponse: " + response.getString( "status" ) );
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
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
        CustomRequestQueue.getInstance( getContext() ).addToRequestQueue(jsonObjectRequest);
    }

    private void logoutRequest(){
        HashMap<String, String > user = mAppHelper.getUserDetails();
        final String token = user.get( AppHelper.USER_TOKEN );
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest( Request.Method.POST, LOGOUT_URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                mAppHelper.logoutUser();
                Intent mIntentMainAcivity = new Intent( getContext(), LoginActivity.class );
                startActivity( mIntentMainAcivity );
                getActivity().finish();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
            }
        } ){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Authorization","Token "+ token);
                return headers;
            }
        };
        jsonObjectRequest.setRetryPolicy( new DefaultRetryPolicy( 3000,DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT ) );
        CustomRequestQueue.getInstance( getContext() ).addToRequestQueue(jsonObjectRequest);
    }

    private boolean turnBluetooth (boolean bluetooth){
        if (bluetooth == true) {
            // turn bluetooth off
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled()) {
            }else{
                mBluetoothAdapter.enable();
            }
            return true;
        } else {
            // turn bluetooth off
            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();
            }
            return false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Profile profile = Profile.getCurrentProfile();
        if (profile != null) {
            userAccountFragment = UserAccountFragment.newInstance( profile );
            mTextView.setText( "Welcome " + profile.getFirstName() +" "+ profile.getLastName() );
        }

    }
}
