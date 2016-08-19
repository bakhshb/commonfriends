package com.example.xgc4811.myapp.helper;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;

import java.util.HashMap;
import java.util.Set;

/**
 * Created by bakhs on 23/07/2016.
 */
public class AppHelper {
    // Shared Preferences
    private SharedPreferences pref;

    // Editor for Shared preferences
    private SharedPreferences.Editor editor;

    // Context
    private Context mContext;

    // Shared pref mode
    private static final int PRIVATE_MODE = 0;

    // Sharedpref file name
    private static final String PREF_NAME = "CommonFriendPref";

    // All Shared Preferences Keys
    private static final String IS_LOGIN = "IsLoggedIn";

    // User name (make variable public to access from outside)
    public static final String USER_TOKEN = "token";

    public static final String BLUETOOTH_ADDRESS = "BluetoothAddress";

    public static final String COMMON_FRIENDS = "CommonFriends";

    public AppHelper(Context context){
        this.mContext = context;
        pref = mContext.getSharedPreferences(PREF_NAME, PRIVATE_MODE);
        editor = pref.edit();
    }

    /**
     * Create login session
     * */
    public void createLoginSession(String token){
        // Storing login value as TRUE
        editor.putBoolean(IS_LOGIN, true);
        // Storing token in pref
        editor.putString( USER_TOKEN, token);
        // commit changes
        editor.commit();
    }

    /**
     * Get stored session data
     * */
    public HashMap<String, String> getUserDetails(){
        HashMap<String, String> user = new HashMap<String, String>();
        // user name
        user.put(USER_TOKEN, pref.getString(USER_TOKEN, null));
        user.put( BLUETOOTH_ADDRESS, pref.getString( BLUETOOTH_ADDRESS, null ));
        // return user
        return user;
    }

    /**
     * Clear session details
     * */
    public void logoutUser(){
        // Clearing all data from Shared Preferences
        editor.clear();
        editor.commit();
    }

    /**
     * Quick check for login
     * **/
    // Get Login State
    public boolean isLoggedIn(){
        return pref.getBoolean(IS_LOGIN, false);
    }

    public boolean isInternetAvailable(){
        ConnectivityManager cm = (ConnectivityManager) mContext.getSystemService(mContext.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null;
    }

    public void setDeviceAddress (String bluetoothAddress){
        editor.putString( BLUETOOTH_ADDRESS,  bluetoothAddress);
        editor.commit();
    }

    public void setCommonFriends(Set<String> commonFriends){
        editor.putStringSet(COMMON_FRIENDS, commonFriends);
        editor.commit();
    }

    public HashMap<String, Set<String>> getCommonFriends (){
        HashMap<String, Set<String>> user = new HashMap<String, Set<String>>();
        user.put(COMMON_FRIENDS, pref.getStringSet(COMMON_FRIENDS, null));


        return user;

    }
}
