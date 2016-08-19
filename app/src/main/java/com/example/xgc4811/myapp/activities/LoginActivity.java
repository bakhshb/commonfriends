package com.example.xgc4811.myapp.activities;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.xgc4811.myapp.R;
import com.example.xgc4811.myapp.helper.AppHelper;
import com.example.xgc4811.myapp.helper.CustomRequestQueue;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    //System status
    static final int STATE_NONE=0;
    static final int STATE_LOGGING_IN = 1;

    // API
    private static final String LOGIN_URL = "http://bakhshb.pythonanywhere.com/api/login/facebook/";

    private AppHelper mAppHelper;
    private TextView mTextView;
    private ProgressDialog mProgressDialog;

    //Facebook
    private CallbackManager callbackManager;
    private FacebookCallback<LoginResult> facebookCallBack = new FacebookCallback<LoginResult>() {
        @Override
        public void onSuccess(LoginResult loginResult) {
            // Login to backend
            AccessToken token = loginResult.getAccessToken();
            if (token.getToken() != null) {
                mHandler.obtainMessage( STATE_LOGGING_IN, token.getToken() ).sendToTarget();
            }
        }
        @Override
        public void onCancel() {

        }
        @Override
        public void onError(FacebookException error) {
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        FacebookSdk.sdkInitialize(this.getApplicationContext());
        callbackManager = CallbackManager.Factory.create();
        setContentView( R.layout.activity_login );
        this.setTitle( "Common Friends Login" );

        init(  );

        if (mAppHelper.isLoggedIn()){
            Intent mIntentMainAcivity = new Intent( getApplicationContext(), MainActivity.class );
            startActivity( mIntentMainAcivity );
            finish();

        }
    }

    public void init (){

        LoginButton loginButton = (LoginButton) findViewById(R.id.facebook_login_button);
        loginButton.setReadPermissions( Arrays.asList("public_profile"));
        loginButton.registerCallback(callbackManager, facebookCallBack);
        mTextView = (TextView) findViewById(R.id.user_detail);
        mTextView.setText( "Please Login With Facebook" );
        mAppHelper = new AppHelper( this );
        mProgressDialog = new ProgressDialog (this); //display an invisible overlay dialog to prevent user interaction and pressing back
        mProgressDialog.setCancelable(false);
        mProgressDialog.setProgressStyle( mProgressDialog.STYLE_SPINNER );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult( requestCode, resultCode, data );

        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    private final Handler mHandler = new Handler(  ){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case STATE_NONE:
                    Log.i( TAG, "handleMessage: Clear" );
                    break;
                case STATE_LOGGING_IN:
                    if (turnBluetooth( true ) == true) {
                        loginRequest( (String) msg.obj );
                    }
                    Log.i( TAG, "handleMessage: Login in to server" );
                    break;
            }
        }
    };

    private boolean turnBluetooth (boolean bluetooth){
        BluetoothAdapter mBluetoothAdapter;
        if (bluetooth == true) {
            // turn bluetooth off
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled()) {
            }else{
                mBluetoothAdapter.enable();
            }
            return true;
        } else {
            // turn bluetooth off
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();
            }
            return false;
        }
    }


    private void loginRequest (String facebookToken){
        mProgressDialog.setMessage("Login ...");
        showDialog();
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("access_token", facebookToken);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest( Request.Method.POST, LOGIN_URL, new JSONObject( params ), new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                hideDialog();
                try {
                    mAppHelper.createLoginSession( response.getString( "sessionToken" ) );
                    //save bluetooth mac address
                    mAppHelper.setDeviceAddress( android.provider.Settings.Secure.getString(getContentResolver(), "bluetooth_address") );
                    Intent mIntentMainAcivity = new Intent( getApplicationContext(), MainActivity.class );
                    startActivity( mIntentMainAcivity );
                    finish();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                hideDialog();
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
        CustomRequestQueue.getInstance( this ).addToRequestQueue( jsonObjectRequest );
    }

    private void showDialog() {
        if (!mProgressDialog.isShowing())
            mProgressDialog.show();
    }

    private void hideDialog() {
        if (mProgressDialog.isShowing())
            mProgressDialog.dismiss();
    }
}
