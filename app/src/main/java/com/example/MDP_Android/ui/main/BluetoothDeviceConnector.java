package com.example.MDP_Android.ui.main;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.MDP_Android.R;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothDeviceConnector extends AppCompatActivity {

    //    Initialise Variables
    private static final String TAG = "BluetoothPopUp";
    private static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private String connStatus;

    public static BluetoothDevice btDevice;
    public ArrayList<BluetoothDevice> newBTDevices;
    public ArrayList<BluetoothDevice> pairedBTDevices;
    public DeviceListAdapter newDeviceListAdapter;
    public DeviceListAdapter pairedDeviceListAdapter;

    BluetoothAdapter bluetoothAdapter;
    TextView connStatusTextView;
    ListView otherDevicesListView;
    ListView pairedDevicesListView;
    Button connectBtn;
    ProgressDialog progressDialog;
    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;
    BluetoothConnectionService bluetoothConnection;
    boolean retryConnection = false;
    Handler reconnectionHandler = new Handler();

    // Update bluetooth connection status if reconnection successful
    Runnable reconnectionRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                if (BluetoothConnectionService.BluetoothConnectionStatus == false) {
                    startBTConnection(btDevice, myUUID);
                    Toast.makeText(BluetoothDeviceConnector.this, "Reconnection Success", Toast.LENGTH_SHORT).show();

                }
                reconnectionHandler.removeCallbacks(reconnectionRunnable);
                retryConnection = false;
            } catch (Exception e) {
                Toast.makeText(BluetoothDeviceConnector.this, "Failed to reconnect, trying in 5 second", Toast.LENGTH_SHORT).show();
            }
        }
    };

    //    Setup page variables
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.bluetooth_pop_up_window);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Switch bluetoothSwitch = findViewById(R.id.bluetoothSwitch);
        if (bluetoothAdapter.isEnabled()) {
            bluetoothSwitch.setChecked(true);
            bluetoothSwitch.setText("ON");
        }

        otherDevicesListView = findViewById(R.id.otherDevicesListView);
        pairedDevicesListView = findViewById(R.id.pairedDevicesListView);
        newBTDevices = new ArrayList<>();
        pairedBTDevices = new ArrayList<>();
        connectBtn = findViewById(R.id.connectBtn);

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(broadcastReceiver4, filter);
        IntentFilter filter2 = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver5, filter2);

        otherDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                pairedDevicesListView.setAdapter(pairedDeviceListAdapter);

                String deviceName = newBTDevices.get(i).getName();
                String deviceAddress = newBTDevices.get(i).getAddress();
                Log.d(TAG, "onItemClick: A device is selected.");
                Log.d(TAG, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(TAG, "onItemClick: DEVICE ADDRESS: " + deviceAddress);

                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    Log.d(TAG, "onItemClick: Initiating pairing with " + deviceName);
                    newBTDevices.get(i).createBond();

                    bluetoothConnection = new BluetoothConnectionService(BluetoothDeviceConnector.this);
                    btDevice = newBTDevices.get(i);
                }
            }
        });

        pairedDevicesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                otherDevicesListView.setAdapter(newDeviceListAdapter);

                String deviceName = pairedBTDevices.get(i).getName();
                String deviceAddress = pairedBTDevices.get(i).getAddress();
                Log.d(TAG, "onItemClick: A device is selected.");
                Log.d(TAG, "onItemClick: DEVICE NAME: " + deviceName);
                Log.d(TAG, "onItemClick: DEVICE ADDRESS: " + deviceAddress);

                bluetoothConnection = new BluetoothConnectionService(BluetoothDeviceConnector.this);
                btDevice = pairedBTDevices.get(i);
            }
        });

        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                Log.d(TAG, "onChecked: Switch button toggled. Enabling/Disabling Bluetooth");
                if (isChecked) {
                    compoundButton.setText("ON");
                } else {
                    compoundButton.setText("OFF");
                }

                if (bluetoothAdapter == null) {
                    Log.d(TAG, "enableDisableBT: Device does not support Bluetooth capabilities!");
                    Toast.makeText(BluetoothDeviceConnector.this, "Device Does Not Support Bluetooth capabilities!", Toast.LENGTH_LONG).show();
                    compoundButton.setChecked(false);
                } else {
                    if (!bluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "enableDisableBT: enabling Bluetooth");
                        Log.d(TAG, "enableDisableBT: Making device discoverable for 600 seconds.");

                        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 600);
                        startActivity(discoverableIntent);

                        compoundButton.setChecked(true);

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(broadcastReceiver1, BTIntent);

                        IntentFilter discoverIntent = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
                        registerReceiver(broadcastReceiver2, discoverIntent);
                    }
                    if (bluetoothAdapter.isEnabled()) {
                        Log.d(TAG, "enableDisableBT: disabling Bluetooth");
                        bluetoothAdapter.disable();

                        IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(broadcastReceiver1, BTIntent);
                    }
                }
            }
        });

        connectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (btDevice == null) {
                    Toast.makeText(BluetoothDeviceConnector.this, "Please Select a Device before connecting.", Toast.LENGTH_LONG).show();
                } else {
                    startConnection();
                }
            }
        });

        connStatus = "Disconnected";
        sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        if (sharedPreferences.contains("connStatus"))
            connStatus = sharedPreferences.getString("connStatus", "");

        connStatusTextView = (TextView) findViewById(R.id.connStatusTextView);
        connStatusTextView.setText(connStatus);

        ImageButton backBtn = findViewById(R.id.backBtn);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor = sharedPreferences.edit();
                editor.putString("connStatus", connStatusTextView.getText().toString());
                editor.commit();
                finish();
            }
        });

        progressDialog = new ProgressDialog(BluetoothDeviceConnector.this);
        progressDialog.setMessage("Waiting for other device to reconnect...");
        progressDialog.setCancelable(false);
        progressDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
    }

    //    Scan for new devices
    @RequiresApi(api = Build.VERSION_CODES.M)
    public void toggleButtonScan(View view) {
        Log.d(TAG, "toggleButton: Scanning for unpaired devices.");
        newBTDevices.clear();
        if (bluetoothAdapter != null) {
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(BluetoothDeviceConnector.this, "Please turn on Bluetooth first!", Toast.LENGTH_SHORT).show();
            }
            if (bluetoothAdapter.isDiscovering()) {
                bluetoothAdapter.cancelDiscovery();
                Log.d(TAG, "toggleButton: Cancelling Discovery.");

                checkBTPermissions();

                bluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(broadcastReceiver3, discoverDevicesIntent);
            } else if (!bluetoothAdapter.isDiscovering()) {
                checkBTPermissions();

                bluetoothAdapter.startDiscovery();
                IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
                registerReceiver(broadcastReceiver3, discoverDevicesIntent);
            }
            pairedBTDevices.clear();
            Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
            Log.d(TAG, "toggleButton: Number of paired devices found: " + pairedDevices.size());
            for (BluetoothDevice d : pairedDevices) {
                Log.d(TAG, "Paired Devices: " + d.getName() + " : " + d.getAddress());
                pairedBTDevices.add(d);
                pairedDeviceListAdapter = new DeviceListAdapter(this, R.layout.device_adapter_view, pairedBTDevices);
                pairedDevicesListView.setAdapter(pairedDeviceListAdapter);
            }
        }
    }

    //    Checking bluetooth permission if higher than android lollipop
    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkBTPermissions() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            permissionCheck += this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION");
            if (permissionCheck != 0) {
                this.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001);
            }
        } else {
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");

        }
    }

    //    Receiver to updated Bluetooth adapter state
    private final BroadcastReceiver broadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(bluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");

                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };

    //    Receiver to updated Bluetooth adapter discoverability
    private final BroadcastReceiver broadcastReceiver2 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(bluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);

                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting...");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };

    //    Receiver to update new discovered bluetooth devices list
    private BroadcastReceiver broadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                newBTDevices.add(device);
                Log.d(TAG, "onReceive: " + device.getName() + " : " + device.getAddress());
                newDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, newBTDevices);
                otherDevicesListView.setAdapter(newDeviceListAdapter);

            }
        }
    };

    //    Receiver to update when new device becomes paired device
    private BroadcastReceiver broadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BOND_BONDED.");
                    Toast.makeText(BluetoothDeviceConnector.this, "Successfully paired with " + mDevice.getName(), Toast.LENGTH_SHORT).show();
                    btDevice = mDevice;
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BOND_BONDING.");
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BOND_NONE.");
                }
            }
        }
    };

    //    Receiver to update when device becomes connected/disconnected
    private BroadcastReceiver broadcastReceiver5 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
            editor = sharedPreferences.edit();

            if (status.equals("connected")) {
                try {
                    progressDialog.dismiss();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "broadcastReceiver5: Device now connected to " + mDevice.getName());
                Toast.makeText(BluetoothDeviceConnector.this, "Device now connected to " + mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Connected to " + mDevice.getName());
                connStatusTextView.setText("Connected to " + mDevice.getName());
            } else if (status.equals("disconnected") && retryConnection == false) {
                Log.d(TAG, "broadcastReceiver5: Disconnected from " + mDevice.getName());
                Toast.makeText(BluetoothDeviceConnector.this, "Disconnected from " + mDevice.getName(), Toast.LENGTH_LONG).show();
                bluetoothConnection = new BluetoothConnectionService(BluetoothDeviceConnector.this);

                sharedPreferences = getApplicationContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
                editor = sharedPreferences.edit();
                editor.putString("connStatus", "Disconnected");
                TextView connStatusTextView = findViewById(R.id.connStatusTextView);
                connStatusTextView.setText("Disconnected");
                editor.commit();

                try {
                    progressDialog.show();
                } catch (Exception e) {
                    Log.d(TAG, "BluetoothPopUp: mBroadcastReceiver5 Dialog show failure");
                }
                retryConnection = true;
                reconnectionHandler.postDelayed(reconnectionRunnable, 5000);

            }
            editor.commit();
        }
    };

    public void startConnection() {
        startBTConnection(btDevice, myUUID);
    }

    public void startBTConnection(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startBTConnection: Initializing RFC Bluetooth Connection");

        bluetoothConnection.startClientThread(device, uuid);
    }

    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called");
        super.onDestroy();
        try {
            unregisterReceiver(broadcastReceiver1);
            unregisterReceiver(broadcastReceiver2);
            unregisterReceiver(broadcastReceiver3);
            unregisterReceiver(broadcastReceiver4);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver5);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause: called");
        super.onPause();
        try {
            unregisterReceiver(broadcastReceiver1);
            unregisterReceiver(broadcastReceiver2);
            unregisterReceiver(broadcastReceiver3);
            unregisterReceiver(broadcastReceiver4);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver5);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void finish() {
        Intent data = new Intent();
        data.putExtra("btDevice", btDevice);
        data.putExtra("myUUID", myUUID);
        setResult(RESULT_OK, data);
        super.finish();
    }
}
