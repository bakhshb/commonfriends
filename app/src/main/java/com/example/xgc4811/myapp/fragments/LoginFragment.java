package com.example.xgc4811.myapp.fragments;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.xgc4811.myapp.services.BluetoothService;
import com.example.xgc4811.myapp.helper.CustomRequestQueue;
import com.example.xgc4811.myapp.R;
import com.example.xgc4811.myapp.activities.BluetoothActivity;
import com.example.xgc4811.myapp.helper.AppHelper;
import com.facebook.AccessToken;
import com.facebook.AccessTokenTracker;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.Profile;
import com.facebook.ProfileTracker;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;


/**
 * A simple {@link Fragment} subclass.
 */
public class LoginFragment extends Fragment implements View.OnClickListener {

    static final String TAG = "LoginFragment";
    //System status
    static final int STATE_NONE=0;
    static final int STATE_LOGGING_IN = 1;
    static final int STATE_ACQUIRE_DATA=2;
    static final int STATE_SEND_BLUETOOTH = 3;
    static final int STATE_START_SERVICE = 4 ;
    static final int STATE_LOGGING_OUT = 5;

    static int mState;

    // API
    private static final String LOGIN_URL = "https://commonfriends.herokuapp.com/api/login/facebook/";
    private static final String LOGOUT_URL = "https://commonfriends.herokuapp.com/api/logout/facebook/";
    private static final String BLUETOOTH_USER_URL = "https://commonfriends.herokuapp.com/api/bluetooth/user/";
    private AppHelper mAppHelper;
    //Fragment
    UserAccountFragment userAccountFragment = null;
    // UI
    private ProgressDialog mProgressDialog;
    private TextView mTextView;
    private Button mButtonMutualFriend;
    private Button mButtonAccount;
    //Facebook
    private CallbackManager callbackManager;
    private FacebookCallback<LoginResult> facebookCallBack = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            // Login to backend
            AccessToken token = loginResult.getAccessToken();
            if (token.getToken() != null) {
                mHandler.obtainMessage( STATE_LOGGING_IN, token.getToken() ).sendToTarget();
                mState=STATE_LOGGING_IN;
            }
        }

        @Override
        public void onCancel() {

        }
        @Override
        public void onError(FacebookException error) {
        }
    };

    public LoginFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FacebookSdk.sdkInitialize(getContext().getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        // Facebook TokenTraker
        AccessTokenTracker tokenTraker = new AccessTokenTracker() {
            @Override
            protected void onCurrentAccessTokenChanged(AccessToken oldAccessToken, AccessToken currentAccessToken) {
                if (currentAccessToken == null){
                    mHandler.obtainMessage(STATE_LOGGING_OUT).sendToTarget();
                    mState= STATE_LOGGING_OUT;
                    return;
                }
            }
        };
        // Facebook ProfileTracker
        ProfileTracker profileTracker = new ProfileTracker() {
            @Override
            protected void onCurrentProfileChanged(Profile oldProfile, Profile currentProfile) {
                if (currentProfile != null){
                    if (mState ==STATE_LOGGING_IN){
                        mHandler.obtainMessage(STATE_ACQUIRE_DATA,currentProfile).sendToTarget();
                        mState = STATE_ACQUIRE_DATA;
                    }
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate( R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init( view );
    }

    public void init (View view){
        mProgressDialog = new ProgressDialog (getContext()); //display an invisible overlay dialog to prevent user interaction and pressing back
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle( mProgressDialog.STYLE_SPINNER );
        mProgressDialog.dismiss();

        LoginButton loginButton = (LoginButton) view.findViewById(R.id.login_button);
        loginButton.setReadPermissions(Arrays.asList("public_profile"));
        loginButton.setFragment(this);
        loginButton.registerCallback(callbackManager, facebookCallBack);
        mTextView = (TextView) view.findViewById(R.id.user_detail);
        mButtonMutualFriend = (Button) view.findViewById(R.id.button_mutual_friend);
        mButtonMutualFriend.setOnClickListener(this);
        mButtonAccount = (Button) view.findViewById(R.id.button_account);
        mButtonAccount.setOnClickListener(this);
        mAppHelper = new AppHelper( getContext() );
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle( "CommonFriends" );
        if (!mAppHelper.isInternetAvailable()){
            Toast.makeText( getContext(),"No Internet Connection", Toast.LENGTH_SHORT ).show();
        }
        if (!mAppHelper.isLoggedIn()) {
            if (mState != STATE_LOGGING_IN) {
                LoginManager.getInstance().logOut();
                mHandler.obtainMessage( STATE_NONE ).sendToTarget();
                mState = STATE_NONE;
            }
        } else{
            Profile profile = Profile.getCurrentProfile();
            if (profile != null ) {
                mHandler.obtainMessage( STATE_ACQUIRE_DATA, profile ).sendToTarget();
                mState = STATE_ACQUIRE_DATA;
                mHandler.obtainMessage( STATE_START_SERVICE ).sendToTarget();
            }
        }
    }

    public void displayMessage(Profile profile){
            mTextView.setText("Welcome " + profile.getName());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.button_mutual_friend:
                Intent mIntentBluetooth = new Intent( getContext().getApplicationContext(), BluetoothActivity.class );
                startActivity( mIntentBluetooth );
                break;
            case R.id.button_account:
                if (userAccountFragment !=null) {
                    getFragmentManager().beginTransaction().replace(R.id.container_layout, userAccountFragment).addToBackStack(null).commit();
                }
                break;
        }
    }

    private void loginRequest (String facebookToken){
        mProgressDialog.show();
        mProgressDialog.setMessage( "Logging in..." );
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", facebookToken);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest( Request.Method.POST, LOGIN_URL, new JSONObject( params ), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    mProgressDialog.dismiss();
                    mAppHelper.createLoginSession( response.getString( "sessionToken" ) );
                    //save bluetooth mac address
                    mAppHelper.setDeviceAddress( android.provider.Settings.Secure.getString(getContext().getContentResolver(), "bluetooth_address") );
                    mHandler.obtainMessage(STATE_SEND_BLUETOOTH).sendToTarget();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mProgressDialog.dismiss();
            }
        } ){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                headers.put("Content-Type","application/json");
                return headers;
            }
        };

        jsonObjectRequest.setRetryPolicy( new DefaultRetryPolicy( 3000,DefaultRetryPolicy.DEFAULT_MAX_RETRIES, DefaultRetryPolicy.DEFAULT_BACKOFF_MULT ) );
        CustomRequestQueue.getInstance( getContext() ).addToRequestQueue( jsonObjectRequest );
    }

    private void logoutRequest(){
        HashMap<String, String > user = mAppHelper.getUserDetails();
        final String token = user.get( AppHelper.USER_TOKEN );
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest( Request.Method.POST, LOGOUT_URL, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
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

        mAppHelper.logoutUser();
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
                    mHandler.obtainMessage(STATE_START_SERVICE).sendToTarget();
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


    public void showAll(){
        mButtonAccount.setVisibility(View.VISIBLE);
        mButtonMutualFriend.setVisibility( View.VISIBLE );
    }

    public void clearAll (){
        mTextView.setText("Login with Facebook");
        mButtonAccount.setVisibility(View.GONE);
        mButtonMutualFriend.setVisibility( View.GONE );
    }

    private final Handler mHandler = new Handler(  ){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case STATE_NONE:
                    clearAll();
                    Log.i( TAG, "handleMessage: Clear" );
                    break;
                case STATE_LOGGING_IN:
                    if (turnBluetooth( true ) == true) {
                        loginRequest( (String) msg.obj );
                    }
                    Log.i( TAG, "handleMessage: Login in to server" );
                    break;
                case STATE_ACQUIRE_DATA:
                    displayMessage( (Profile) msg.obj );
                    userAccountFragment = UserAccountFragment.newInstance( (Profile) msg.obj );
                    showAll();
                    Log.i( TAG, "handleMessage: Get profile from Facebook");
                    break;
                case STATE_SEND_BLUETOOTH:
                    bluetoothUserRequest();
                    // Making the device discoverable
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    enableBtIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
                    startActivityForResult(enableBtIntent, 0);
                    Log.i( TAG, "handleMessage: Sending Bluetooth Mac Address");
                    break;
                case STATE_START_SERVICE:
                    bluetoothService(true);
                    Log.i( TAG, "handleMessage: Calling Bluetooth Service");
                    break;
                case STATE_LOGGING_OUT:
                    logoutRequest();
                    clearAll();
                    bluetoothService(false);
                    // turn bluetooth off
                    turnBluetooth(false);
                    Log.i( TAG, "handleMessage: Logout from server " );
                    break;
            }
        }
    };

    private void bluetoothService(boolean bluetoothStatus){
        // Start bluetooth service using AlarmManager
        Calendar cal = Calendar.getInstance();
        cal.add( Calendar.SECOND, 10 );
        Intent mintent = new Intent( getContext(), BluetoothService.class );
        PendingIntent pintent = PendingIntent.getService( getContext(), 0, mintent, PendingIntent.FLAG_UPDATE_CURRENT );
        AlarmManager alarm = (AlarmManager) getContext().getSystemService( Context.ALARM_SERVICE );
        if (bluetoothStatus) {
            //for 30 mint 60*60*1000
            alarm.setRepeating( AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), 30 * 1000, pintent );
           // bluetoothUserRequest();
        }else if (!bluetoothStatus){
            alarm.cancel( pintent );
            getContext().stopService( new Intent( getContext(), BluetoothService.class ) );
        }
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

}
