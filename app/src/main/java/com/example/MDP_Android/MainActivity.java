package com.example.MDP_Android;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.viewpager.widget.ViewPager;

import com.example.MDP_Android.ui.main.BluetoothConnectionService;
import com.example.MDP_Android.ui.main.BluetoothPopUp;
import com.example.MDP_Android.ui.main.CommsFragment;
import com.example.MDP_Android.ui.main.GridMap;
import com.example.MDP_Android.ui.main.MapInformation;
import com.example.MDP_Android.ui.main.MapTabFragment;
import com.example.MDP_Android.ui.main.ReconfigureFragment;
import com.example.MDP_Android.ui.main.SectionsPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // Declare Variables
    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static Context context;

    private static GridMap gridMap;
    static TextView xAxisTextView, yAxisTextView, directionAxisTextView, robotStatusTextView;
    static Button f1, f2;
    Button reconfigure;
    ReconfigureFragment reconfigureFragment = new ReconfigureFragment();

    BluetoothDevice mBTDevice;
    private static UUID myUUID;
    ProgressDialog myDialog;

    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {

//        Initialization
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SectionsPagerAdapter sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        ViewPager viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(sectionsPagerAdapter);
        viewPager.setOffscreenPageLimit(9999);
        TabLayout tabs = findViewById(R.id.tabs);
        tabs.setupWithViewPager(viewPager);
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

//       Set up sharedPreferences
        MainActivity.context = getApplicationContext();
        this.sharedPreferences();
        editor.putString("message", "");
        editor.putString("direction", "None");
        editor.putString("connStatus", "Disconnected");
        editor.commit();

//       Initialise Button to print MDF String of present map
        Button printMDFStringButton = (Button) findViewById(R.id.printMDFString);
        printMDFStringButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = "Explored : " + GridMap.getPublicMDFExploration();
                editor = sharedPreferences.edit();
                editor.putString("message", CommsFragment.getMessageReceivedTextView().getText() + "\n" + message);
                editor.commit();
                refreshMessageReceived();
                message = "Obstacle : " + GridMap.getPublicMDFObstacle() + "0";
                editor.putString("message", CommsFragment.getMessageReceivedTextView().getText() + "\n" + message);
                editor.commit();
                refreshMessageReceived();
            }
        });

//       Initialise Button to Bluetooth Connectivity Page
        Button bluetoothButton = (Button) findViewById(R.id.bluetoothButton);
        bluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent popup = new Intent(MainActivity.this, BluetoothPopUp.class);
                startActivity(popup);
            }
        });

//       [CHECK]
        Button mapInformationButton = (Button) findViewById(R.id.mapInfoButton);
        mapInformationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editor.putString("mapJsonObject", String.valueOf(gridMap.getCreateJsonObject()));
                editor.commit();
                Intent popup = new Intent(MainActivity.this, MapInformation.class);
                startActivity(popup);
            }
        });


//       Initialise  Map
        gridMap = new GridMap(this);
        gridMap = findViewById(R.id.mapView);
//        Initialise TextView to show X & Y axis
        xAxisTextView = findViewById(R.id.xAxisTextView);
        yAxisTextView = findViewById(R.id.yAxisTextView);
//        Initialise TextView to show Direction
        directionAxisTextView = findViewById(R.id.directionAxisTextView);
//        Initialise TextView to show RobotStatus
        robotStatusTextView = findViewById(R.id.robotStatusTextView);

//        Initialise ProgressDialog for possible Bluetooth reconnection
        myDialog = new ProgressDialog(MainActivity.this);
        myDialog.setMessage("Waiting for other device to reconnect...");
        myDialog.setCancelable(false);
        myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

//        Initialise Buttons for F1, F2 and Reconfiguration of F1 & F2
        f1 = (Button) findViewById(R.id.f1ActionButton);
        f2 = (Button) findViewById(R.id.f2ActionButton);
        reconfigure = (Button) findViewById(R.id.configureButton);

//        Set saved F1 string to F1 button
        if (sharedPreferences.contains("F1")) {
            f1.setContentDescription(sharedPreferences.getString("F1", ""));
            showLog("setText for f1Btn: " + f1.getContentDescription().toString());
        }

//        Set saved F2 string to F2 button
        if (sharedPreferences.contains("F2")) {
            f2.setContentDescription(sharedPreferences.getString("F2", ""));
            showLog("setText for f2Btn: " + f2.getContentDescription().toString());
        }

//        On click of F1 button, only if it has a assigned value, it will be printed
        f1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked f1Btn");
                if (!f1.getContentDescription().toString().equals("empty"))
                    MainActivity.printMessage(f1.getContentDescription().toString());
                showLog("f1Btn value: " + f1.getContentDescription().toString());
                showLog("Exiting f1Btn");
            }
        });

//        On click of F2 button, only if it has a assigned value, it will be printed
        f2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked f2Btn");
                if (!f2.getContentDescription().toString().equals("empty"))
                    MainActivity.printMessage(f2.getContentDescription().toString());
                showLog("f2Btn value: " + f2.getContentDescription().toString());
                showLog("Exiting f2Btn");
            }
        });

//        On click of `Reconfigure`, the reconfigure menu will show
        reconfigure.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked reconfigureBtn");
                reconfigureFragment.show(getFragmentManager(), "Reconfigure Fragment");
                showLog("Exiting reconfigureBtn");
            }
        });
    }

    public static Button getF1() {
        return f1;
    }

    public static Button getF2() {
        return f2;
    }

    public static GridMap getGridMap() {
        return gridMap;
    }

    public static TextView getRobotStatusTextView() {
        return robotStatusTextView;
    }

    public static void sharedPreferences() {
        sharedPreferences = MainActivity.getSharedPreferences(MainActivity.context);
        editor = sharedPreferences.edit();
    }

    //       Send message via bluetooth
    public static void printMessage(String message) {
        showLog("Entering printMessage");
        editor = sharedPreferences.edit();

        if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }
        showLog(message);
        editor.putString("message", CommsFragment.getMessageReceivedTextView().getText() + "\n" + message);
        editor.commit();
        refreshMessageReceived();
        showLog("Exiting printMessage");
    }

    //        Send x, y coordinate for way point via bluetooth
    public static void printMessage(String name, int x, int y) throws JSONException {
        showLog("Entering printMessage");
        editor = sharedPreferences.edit();

        JSONObject jsonObject = new JSONObject();
        String message;

        switch (name) {
            case "waypoint":
                jsonObject.put(name, name);
                jsonObject.put("x", x);
                jsonObject.put("y", y);
                message = "WP:" + x + ":" + y;
                break;
            default:
                message = "Unexpected printMessage by: " + name;
                break;
        }
        editor.putString("message", CommsFragment.getMessageReceivedTextView().getText() + "\n" + message);
        editor.commit();
        if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
            byte[] bytes = message.getBytes(Charset.defaultCharset());
            BluetoothConnectionService.write(bytes);
        }
        showLog("Exiting printMessage");
    }

    //        Update communication box output
    public static void refreshMessageReceived() {
        CommsFragment.getMessageReceivedTextView().setText(sharedPreferences.getString("message", ""));
    }

    //        Update Direction
    public void refreshDirection(String direction) {
        gridMap.setRobotDirection(direction);
        directionAxisTextView.setText(sharedPreferences.getString("direction", ""));
        printMessage("Direction is set to " + direction);
    }

    //        Update X & Y axis label
    public static void refreshLabel() {
        xAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[0] - 1));
        yAxisTextView.setText(String.valueOf(gridMap.getCurCoord()[1] - 1));
        directionAxisTextView.setText(sharedPreferences.getString("direction", ""));
    }

    //        Update received messages to communication box
    public static void receiveMessage(String message) {
        showLog("Entering receiveMessage");
        sharedPreferences();
        editor.putString("message", sharedPreferences.getString("message", "") + "\n" + message);
        editor.commit();
        showLog("Exiting receiveMessage");
    }

    //        Method to simplify logging
    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    //    Retrieve shared preference storage
    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    //      Bluetooth connection listener
    private BroadcastReceiver bluetoothListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice mDevice = intent.getParcelableExtra("Device");
            String status = intent.getStringExtra("Status");
            sharedPreferences();

            if (status.equals("connected")) {
                try {
                    myDialog.dismiss();
                } catch (NullPointerException e) {
                    e.printStackTrace();
                }

                Log.d(TAG, "bluetoothListener: Device now connected to " + mDevice.getName());
                Toast.makeText(MainActivity.this, "Device now connected to " + mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Connected to " + mDevice.getName());
            } else if (status.equals("disconnected")) {
                Log.d(TAG, "bluetoothListener: Disconnected from " + mDevice.getName());
                Toast.makeText(MainActivity.this, "Disconnected from " + mDevice.getName(), Toast.LENGTH_LONG).show();
                editor.putString("connStatus", "Disconnected");
                myDialog.show();
            }
            editor.commit();
        }
    };

    //    Read messages received through bluetooth
    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");
            showLog("receivedMessage: message --- " + message + " ---");
            String[] messageSplit = message.split("\"");

//            Decode grid map sent from AMDToolkit
            try {
                if (messageSplit[1].equals("grid") && (gridMap.getAutoUpdate() || MapTabFragment.manualUpdateRequest)) {
                    String resultString = "";
                    String amdString = message.substring(11, message.length() - 2);
                    showLog("amdString: " + amdString);
                    BigInteger hexBigIntegerExplored = new BigInteger(amdString, 16);
                    String exploredString = hexBigIntegerExplored.toString(2);

                    while (exploredString.length() < 300)
                        exploredString = "0" + exploredString;

                    for (int i = 0; i < exploredString.length(); i = i + 15) {
                        int j = 0;
                        String subString = "";
                        while (j < 15) {
                            subString = subString + exploredString.charAt(j + i);
                            j++;
                        }
                        resultString = subString + resultString;
                    }
                    hexBigIntegerExplored = new BigInteger(resultString, 2);
                    resultString = hexBigIntegerExplored.toString(16);

                    JSONObject amdObject = new JSONObject();
                    amdObject.put("explored", "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff");
                    amdObject.put("length", amdString.length() * 4);
                    amdObject.put("obstacle", resultString);
                    JSONArray amdArray = new JSONArray();
                    amdArray.put(amdObject);
                    JSONObject amdMessage = new JSONObject();
                    amdMessage.put("map", amdArray);
                    message = String.valueOf(amdMessage);
                    gridMap.setReceivedJsonObject(amdMessage);
                    gridMap.updateMapInformation();
                    showLog("Executed for AMD message, message: " + message);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

//            Decode image id sent from rPI
            try {
                if (messageSplit[1].equals("image")) {
                    gridMap.drawImageNumberCell(Integer.parseInt(messageSplit[3]), Integer.parseInt(messageSplit[5]), Integer.parseInt(messageSplit[7]));
                    showLog("Image Added for index: " + Integer.parseInt(messageSplit[3]) + "," + Integer.parseInt(messageSplit[5]));
                }
            } catch (Exception e) {
                showLog("Adding Image Failed");
            }

//            Update grid map automatically or manually
            if (gridMap.getAutoUpdate() || MapTabFragment.manualUpdateRequest) {
                showLog("messageReceiver: update map request");
                try {
                    if (message.contains("ROBOT")) message = message.substring(8);
                    showLog(message);
                    gridMap.setReceivedJsonObject(new JSONObject(message));
                    showLog(gridMap.getReceivedJsonObject().toString());
                    gridMap.updateMapInformation();
                    MapTabFragment.manualUpdateRequest = false;
                    showLog("messageReceiver: try decode successful");
                } catch (JSONException e) {
                    showLog("messageReceiver: try decode unsuccessful");
                }
            } else {
//                Update status even if grid map is not updated [CHECK]
                if (messageSplit[1].equals("status")) {
                    try {
                        String[] splitStatusMsg = message.split("\"");
                        showLog("updateRobotStatus: " + splitStatusMsg.toString());
                        gridMap.printRobotStatus(splitStatusMsg[3]);
                        showLog("statusReceived: robot status updated");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

//            Retrieve and update the communication box
            sharedPreferences();
            String receivedText = sharedPreferences.getString("message", "") + "\n" + message;
            editor.putString("message", receivedText);
            editor.commit();
            refreshMessageReceived();
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                if (resultCode == Activity.RESULT_OK) {
                    mBTDevice = (BluetoothDevice) data.getExtras().getParcelable("mBTDevice");
                    myUUID = (UUID) data.getSerializableExtra("myUUID");
                }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(messageReceiver);
            LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothListener);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(bluetoothListener);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            IntentFilter filter = new IntentFilter("ConnectionStatus");
            LocalBroadcastManager.getInstance(this).registerReceiver(bluetoothListener, filter);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        showLog("Entering onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putString(TAG, "onSaveInstanceState");
        showLog("Exiting onSaveInstanceState");
    }
}