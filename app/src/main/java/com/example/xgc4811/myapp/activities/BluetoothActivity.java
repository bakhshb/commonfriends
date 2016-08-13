package com.example.xgc4811.myapp.activities;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.xgc4811.myapp.services.BluetoothChatService;
import com.example.xgc4811.myapp.Constants;
import com.example.xgc4811.myapp.R;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {

    // Debugging
    private static final String TAG = "BluetoothChat";
    private static final boolean D = true;
    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Arrays
    ArrayAdapter<String> listAdapter;
    ArrayList<String> pairedDevices;
    ArrayList<BluetoothDevice> devices;
    // UI
    TextView mTextView;
    ListView mListView ;
    ProgressDialog mProgressDialog;
    Button mButton;


    // Bluethooth
    BluetoothAdapter mBluetoothAdapter;
    Set<BluetoothDevice> devicesArray;
    IntentFilter mIntentFilter;
    BroadcastReceiver mBroadcastReceiver;
    public static final UUID MY_UUID = UUID.fromString( "8ce255c0-200a-11e0-ac64-0800200c9a66" );


    BluetoothChatService mBluetoothChatService ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_bluetooth );
        this.setTitle( "Search Bluetooth" );

        init();

        if (mBluetoothAdapter == null){
            Toast.makeText(getApplicationContext(), "No Bluethooth detected", 0).show();
            finish();
        }else{
            if (!mBluetoothAdapter.isEnabled()){
                turnBluetoothOn();
            }

            getPairedDevices();
            startDiscovery();
        }


    }

    private void startDiscovery() {
        if (mBluetoothAdapter.isDiscovering()) {
            mBluetoothAdapter.cancelDiscovery();
        }
        mBluetoothAdapter.startDiscovery();
    }

    private void turnBluetoothOn() {
        Intent intent = new Intent( BluetoothAdapter.ACTION_REQUEST_ENABLE );
        startActivityForResult( intent,1 );
    }

    private void getPairedDevices() {
        devicesArray = mBluetoothAdapter.getBondedDevices();
        if (devicesArray.size() >0){
            for (BluetoothDevice device : devicesArray){
                pairedDevices.add( device.getName());
            }
        }
    }

    private void init(){
        mProgressDialog = new ProgressDialog( this );
        mProgressDialog.setMessage( "Searching for devices" );
        mProgressDialog.setProgressStyle( mProgressDialog.STYLE_SPINNER );
        mProgressDialog.setCancelable( false );

        mTextView = (TextView) findViewById(R.id.response_message);
        mListView = (ListView) findViewById( R.id.listView );
        mListView.setOnItemClickListener( this );
        mButton = (Button) findViewById( R.id.button );

        pairedDevices = new ArrayList<String>( );
        devices = new ArrayList<BluetoothDevice>(  );
        listAdapter = new ArrayAdapter<String>( this, android.R.layout.simple_list_item_1,0 );
        mListView.setAdapter( listAdapter );



        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mIntentFilter = new IntentFilter( BluetoothDevice.ACTION_FOUND );

        mBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals( action )){
                    BluetoothDevice device = intent.getParcelableExtra( BluetoothDevice.EXTRA_DEVICE );
                    devices.add( device );
                    listAdapter.add( device.getName()+"" + "\n"+device.getAddress());
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED .equals( action )){
                    mProgressDialog.show();
                    devices.clear();
                    listAdapter.clear();
                }
                else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED .equals( action )){
                    mProgressDialog.dismiss();
                    mTextView.setText( "Available Devices" );
                }
                else if (BluetoothAdapter.ACTION_STATE_CHANGED .equals( action )){
                    if (mBluetoothAdapter.getState() == mBluetoothAdapter.STATE_OFF){
                        turnBluetoothOn();
                    }
                }
            }
        };
        registerReceiver(mBroadcastReceiver,mIntentFilter );

        mIntentFilter =  new IntentFilter( BluetoothAdapter.ACTION_DISCOVERY_STARTED );
        registerReceiver(mBroadcastReceiver,mIntentFilter );
        mIntentFilter =  new IntentFilter( BluetoothAdapter.ACTION_DISCOVERY_FINISHED );
        registerReceiver(mBroadcastReceiver,mIntentFilter );
        mIntentFilter =  new IntentFilter( BluetoothAdapter.ACTION_STATE_CHANGED );
        registerReceiver(mBroadcastReceiver,mIntentFilter );

        mBluetoothChatService = new BluetoothChatService( this, mHandler );

        mButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "Hello";
                byte[] send = message.getBytes();
                mBluetoothChatService.write( send );
            }
        } );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult( requestCode, resultCode, data );
        if (resultCode == RESULT_CANCELED){
            Toast.makeText(getApplicationContext(), "Bluethooth must be enabled to carry on", Toast.LENGTH_SHORT ).show();
            finish();
        }else{
            Intent discoverableIntent = new
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
            startDiscovery();
            mBluetoothChatService.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter quitFilter = new IntentFilter();
        quitFilter.addAction(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mBroadcastReceiver, quitFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mBluetoothAdapter = null;
        unregisterReceiver( mBroadcastReceiver );
        if (mBluetoothChatService != null){
            mBluetoothChatService.stop();
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
        }
        BluetoothDevice selectedDevice = devices.get( position );
        if (mBluetoothChatService != null){
            mBluetoothChatService.stop();
        }
        mBluetoothChatService.start();
        mBluetoothChatService.connect( selectedDevice );


    }

    private Handler mHandler = new Handler(  ){
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    Log.d( TAG, "handleMessage: " + writeMessage );
                    Toast.makeText(getApplicationContext(), writeMessage,
                            Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    Toast.makeText(getApplicationContext(), readMessage,
                            Toast.LENGTH_SHORT).show();
                    Log.d( TAG, "handleMessage: " + readMessage );
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;
            }
            }
    };
}
