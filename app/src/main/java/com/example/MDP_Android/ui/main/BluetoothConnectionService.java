package com.example.MDP_Android.ui.main;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

public class BluetoothConnectionService {

    //     Declare Variables
    private static final String TAG = "BTConnectionServiceTag";
    private static final String appName = "MDP_Group_15";
    public static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private final BluetoothAdapter bluetoothAdapter;

    Context context;

    private AcceptThread mInsecureAcceptThread;
    private ConnectThread connectThread;
    private BluetoothDevice bluetoothDevice;
    private UUID deviceUUID;

    ProgressDialog progressDialog;
    Intent connectionStatus;

    public static boolean BluetoothConnectionStatus = false;
    private static ConnectedThread mConnectedThread;

    //    Initialise new bluetooth connection service
    public BluetoothConnectionService(Context context) {
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.context = context;
        startAcceptThread();
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket ServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            try {
                tmp = bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, myUUID);
                Log.d(TAG, "Accept Thread: Setting up Server using: " + myUUID);
            } catch (IOException e) {
                Log.e(TAG, "Accept Thread: IOException: " + e.getMessage());
            }
            ServerSocket = tmp;
        }

        public void run() {
            Log.d(TAG, "run: AcceptThread Running... ");
            BluetoothSocket socket = null;
            try {
                Log.d(TAG, "run: RFC server socket start here...");

                socket = ServerSocket.accept();
                Log.d(TAG, "run: RFC server socket accepted connection!");
            } catch (IOException e) {
                Log.e(TAG, "run: IOException: " + e.getMessage());
            }
            if (socket != null) {
                connected(socket, socket.getRemoteDevice());
            }
            Log.i(TAG, "END AcceptThread");
        }

        public void cancel() {
            Log.d(TAG, "cancel: Cancelling AcceptThread");
            try {
                ServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Failed to close AcceptThread ServerSocket " + e.getMessage());
            }
        }
    }

    private class ConnectThread extends Thread {
        private BluetoothSocket mSocket;

        public ConnectThread(BluetoothDevice device, UUID u) {
            Log.d(TAG, "ConnectThread: started.");
            bluetoothDevice = device;
            deviceUUID = u;
        }

        public void run() {
            BluetoothSocket tmp = null;
            Log.d(TAG, "RUN: connectThread");

            try {
                Log.d(TAG, "ConnectThread: Trying to create InsecureRFCSocket using UUID: " + myUUID);
                tmp = bluetoothDevice.createRfcommSocketToServiceRecord(deviceUUID);
            } catch (IOException e) {
                Log.e(TAG, "ConnectThread: Could not create InsecureRFCSocket " + e.getMessage());
            }
            mSocket = tmp;
            bluetoothAdapter.cancelDiscovery();

            try {
                mSocket.connect();

                Log.d(TAG, "RUN: connectThread connected.");

                connected(mSocket, bluetoothDevice);

            } catch (IOException e) {
                try {
                    mSocket.close();
                    Log.d(TAG, "RUN: connectThread socket closed.");
                } catch (IOException e1) {
                    Log.e(TAG, "RUN: connectThread: Unable to close connection in socket." + e1.getMessage());
                }
                Log.d(TAG, "RUN: connectThread: could not connect to UUID." + myUUID);
                try {
                    BluetoothDeviceConnector mBluetoothDeviceConnectorActivity = (BluetoothDeviceConnector) context;
                    mBluetoothDeviceConnectorActivity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(context, "Failed to connect to the Device.", Toast.LENGTH_LONG).show();
                        }
                    });
                } catch (Exception z) {
                    z.printStackTrace();
                }

            }
            try {
                progressDialog.dismiss();
            } catch (NullPointerException e) {
                e.printStackTrace();
            }
        }

        public void cancel() {
            Log.d(TAG, "cancel: Closing Client Socket");
            try {
                mSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "cancel: Failed to close ConnectThread mSocket " + e.getMessage());
            }
        }
    }

    public synchronized void startAcceptThread() {
        Log.d(TAG, "start");

        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
        if (mInsecureAcceptThread == null) {
            mInsecureAcceptThread = new AcceptThread();
            mInsecureAcceptThread.start();
        }
    }

    public void startClientThread(BluetoothDevice device, UUID uuid) {
        Log.d(TAG, "startClient: Started.");

        try {
            progressDialog = ProgressDialog.show(context, "Connecting Bluetooth", "Please Wait...", true);
        } catch (Exception e) {
            Log.d(TAG, "StartClientThread Dialog show failure");
        }

        connectThread = new ConnectThread(device, uuid);
        connectThread.start();
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mSocket;
        private final InputStream inStream;
        private final OutputStream outStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "ConnectedThread: Starting.");

            connectionStatus = new Intent("ConnectionStatus");
            connectionStatus.putExtra("Status", "connected");
            connectionStatus.putExtra("Device", bluetoothDevice);
            LocalBroadcastManager.getInstance(context).sendBroadcast(connectionStatus);
            BluetoothConnectionStatus = true;

            this.mSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = mSocket.getInputStream();
                tmpOut = mSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            inStream = tmpIn;
            outStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (true) {
                try {
                    bytes = inStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG, "InputStream: " + incomingMessage);

                    Intent incomingMessageIntent = new Intent("incomingMessage");
                    incomingMessageIntent.putExtra("receivedMessage", incomingMessage);

                    LocalBroadcastManager.getInstance(context).sendBroadcast(incomingMessageIntent);
                } catch (IOException e) {
                    Log.e(TAG, "Error reading input stream. " + e.getMessage());

                    connectionStatus = new Intent("ConnectionStatus");
                    connectionStatus.putExtra("Status", "disconnected");
                    connectionStatus.putExtra("Device", bluetoothDevice);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(connectionStatus);
                    BluetoothConnectionStatus = false;

                    break;
                }
            }
        }

        public void write(byte[] bytes) {
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, "write: Writing to output stream: " + text);
            try {
                outStream.write(bytes);
            } catch (IOException e) {
                Log.e(TAG, "Error writing to output stream: " + e.getMessage());
            }
        }

    }

    private void connected(BluetoothSocket mSocket, BluetoothDevice device) {
        Log.d(TAG, "connected: Starting...");
        bluetoothDevice = device;
        if (mInsecureAcceptThread != null) {
            mInsecureAcceptThread.cancel();
            mInsecureAcceptThread = null;
        }

        mConnectedThread = new ConnectedThread(mSocket);
        mConnectedThread.start();
    }

    public static void write(byte[] out) {
        Log.d(TAG, "write: Write is called.");
        mConnectedThread.write(out);
    }
}
