package com.example.xgc4811.myapp.activities;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.xgc4811.myapp.R;

public class ResultsActivity extends AppCompatActivity {

    private static final String TAG = "ResultsAcitivty";

    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_bluetooth_results );
        this.setTitle( "Bluetooth Result" );
        Bundle mBundle = getIntent().getExtras();
        mTextView = (TextView) findViewById( R.id.bluetoothResults );
        mTextView.setText( "" );
        if (mBundle != null){
            Log.d( TAG, "" + mBundle.get( "msg" )  );
            mTextView.setText( mBundle.getString( "msg" ) );
        }
    }
}
