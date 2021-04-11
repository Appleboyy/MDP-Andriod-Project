package com.example.MDP_Android.ui.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.MDP_Android.MainActivity;
import com.example.MDP_Android.R;

import static android.content.Context.SENSOR_SERVICE;

// Control fragment to display controls for robot
public class ControlFragment extends Fragment implements SensorEventListener {

    // Initialise variables
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "ControlFragment";
    private static long exploreTimer, fastestTimer;
    private static GridMap gridMap;
    private PageViewModel pageViewModel;
    private Sensor sensor;
    private SensorManager sensorManager;

    static Button calibrateButton;
    static Handler timerHandler = new Handler();

    SharedPreferences sharedPreferences;
    ImageButton moveForwardImageBtn, turnRightImageBtn, moveBackImageBtn, turnLeftImageBtn, exploreResetButton, fastestResetButton;
    ToggleButton exploreButton, fastestButton, imageButton;
    TextView exploreTimeTextView, fastestTimeTextView, robotStatusTextView;
    Switch phoneTiltSwitch;

    Runnable timerRunnableExplore = new Runnable() {
        @Override
        public void run() {
            long millisExplore = System.currentTimeMillis() - exploreTimer;
            int secondsExplore = (int) (millisExplore / 1000);
            int minutesExplore = secondsExplore / 60;
            secondsExplore = secondsExplore % 60;

            exploreTimeTextView.setText(String.format("%02d:%02d", minutesExplore, secondsExplore));
            timerHandler.postDelayed(this, 500);
        }
    };

    Runnable timerRunnableFastest = new Runnable() {
        @Override
        public void run() {
            long millisFastest = System.currentTimeMillis() - fastestTimer;
            int secondsFastest = (int) (millisFastest / 1000);
            int minutesFastest = secondsFastest / 60;
            secondsFastest = secondsFastest % 60;

            fastestTimeTextView.setText(String.format("%02d:%02d", minutesFastest, secondsFastest));
            timerHandler.postDelayed(this, 500);
        }
    };

    //    Setup page variables
    public static ControlFragment newInstance(int index) {
        ControlFragment fragment = new ControlFragment();
        Bundle bundle = new Bundle();
        bundle.putInt(ARG_SECTION_NUMBER, index);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pageViewModel = ViewModelProviders.of(this).get(PageViewModel.class);
        int index = 1;
        if (getArguments() != null) {
            index = getArguments().getInt(ARG_SECTION_NUMBER);
        }
        pageViewModel.setIndex(index);
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = null;
        if (MainActivity.colorState == 0) {
            root = inflater.inflate(R.layout.activity_control, container, false);
        } else {
            root = inflater.inflate(R.layout.activity_control_state2, container, false);
        }

        sharedPreferences = getActivity().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

        moveForwardImageBtn = root.findViewById(R.id.forwardImageBtn);
        turnRightImageBtn = root.findViewById(R.id.rightImageBtn);
        moveBackImageBtn = root.findViewById(R.id.backImageBtn);
        turnLeftImageBtn = root.findViewById(R.id.leftImageBtn);
        exploreTimeTextView = root.findViewById(R.id.exploreTimeTextView);
        fastestTimeTextView = root.findViewById(R.id.fastestTimeTextView);
        exploreButton = root.findViewById(R.id.exploreToggleBtn);
        fastestButton = root.findViewById(R.id.fastestToggleBtn);
        imageButton = root.findViewById(R.id.imageToggleBtn);
        exploreResetButton = root.findViewById(R.id.exploreResetImageBtn);
        fastestResetButton = root.findViewById(R.id.fastestResetImageBtn);
        phoneTiltSwitch = root.findViewById(R.id.phoneTiltSwitch);
        calibrateButton = root.findViewById(R.id.calibrateButton);

        robotStatusTextView = MainActivity.getRobotStatusTextView();
        fastestTimer = 0;
        exploreTimer = 0;
        sensorManager = (SensorManager) getActivity().getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        gridMap = MainActivity.getGridMap();

        // Robot control button listeners
        moveForwardImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveForwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("forward");
                    if (gridMap.getValidPosition()) {
                        updateStatus("moving forward");
                        gridMap.printRobotStatus("moving forward");
                    } else {
                        updateStatus("Unable to move forward");
                        gridMap.printRobotStatus("holding position");
                    }
                    MainActivity.printMessage("w");
                } else
                    updateStatus("Please press 'SET STARTPOINT'");
                showLog("Exiting moveForwardImageBtn");
            }
        });

        turnRightImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked turnRightImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("right");
                    MainActivity.printMessage("d");
                    gridMap.printRobotStatus("turning right");
                } else
                    updateStatus("Please press 'SET STARTPOINT'");
                showLog("Exiting turnRightImageBtn");
            }
        });

        moveBackImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveBackwardImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("back");
                    if (gridMap.getValidPosition()) {
                        updateStatus("moving backward");
                        gridMap.printRobotStatus("reversing");
                    } else {
                        updateStatus("Unable to move backward");
                        gridMap.printRobotStatus("holding position");
                    }
                    MainActivity.printMessage("TAKE");
                } else
                    updateStatus("Please press 'SET STARTPOINT'");
                showLog("Exiting moveBackwardImageBtn");
            }
        });

        turnLeftImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked turnLeftImageBtn");
                if (gridMap.getAutoUpdate())
                    updateStatus("Please press 'MANUAL'");
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("left");
                    updateStatus("turning left");
                    MainActivity.printMessage("a");
                    gridMap.printRobotStatus("turning left");
                } else
                    updateStatus("Please press 'SET STARTPOINT'");
                showLog("Exiting turnLeftImageBtn");
            }
        });

        exploreButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked exploreToggleBtn");
                ToggleButton exploreToggleBtn = (ToggleButton) v;
                if (exploreToggleBtn.getText().equals("EXPLORE")) {
                    showToast("Exploration timer stop!");
                    robotStatusTextView.setText("Exploration Stopped");
                    timerHandler.removeCallbacks(timerRunnableExplore);
                } else if (exploreToggleBtn.getText().equals("STOP")) {
                    showToast("Exploration timer start!");
                    MainActivity.printMessage("EX_START");
                    robotStatusTextView.setText("Exploration Started");
                    exploreTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnableExplore, 0);
                } else {
                    showToast("Else statement: " + exploreToggleBtn.getText());
                }
                showLog("Exiting exploreToggleBtn");
            }
        });

        fastestButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked fastestToggleBtn");
                ToggleButton fastestToggleBtn = (ToggleButton) v;
                if (fastestToggleBtn.getText().equals("FASTEST")) {
                    showToast("Fastest timer stop!");
                    robotStatusTextView.setText("Fastest Path Stopped");
                    timerHandler.removeCallbacks(timerRunnableFastest);
                } else if (fastestToggleBtn.getText().equals("STOP")) {
                    showToast("Fastest timer start!");
                    MainActivity.printMessage("FP_START");
                    robotStatusTextView.setText("Fastest Path Started");
                    fastestTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(timerRunnableFastest, 0);
                } else
                    showToast(fastestToggleBtn.getText().toString());
                showLog("Exiting fastestToggleBtn");
            }
        });

        imageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked imageToggleBtn");
                ToggleButton imageToggleBtn = (ToggleButton) v;
                if (imageToggleBtn.getText().equals("IMAGE")) {
                    showToast("Fastest timer stop!");
                    robotStatusTextView.setText("Image Recognition Stopped");
                } else if (imageToggleBtn.getText().equals("STOP")) {
                    showToast("Image recognition start!");
                    MainActivity.printMessage("IR_START");
                    robotStatusTextView.setText("Image Recognition Started");
                } else
                    showToast(imageToggleBtn.getText().toString());
                showLog("Exiting imageToggleBtn");
            }
        });

        exploreResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked exploreResetImageBtn");
                showToast("Resetting exploration time...");
                exploreTimeTextView.setText("00:00");
                robotStatusTextView.setText("Not Available");
                if (exploreButton.isChecked())
                    exploreButton.toggle();
                timerHandler.removeCallbacks(timerRunnableExplore);
                showLog("Exiting exploreResetImageBtn");
            }
        });

        fastestResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked fastestResetImageBtn");
                showToast("Resetting fastest time...");
                fastestTimeTextView.setText("00:00");
                robotStatusTextView.setText("Not Available");
                if (fastestButton.isChecked())
                    fastestButton.toggle();
                timerHandler.removeCallbacks(timerRunnableFastest);
                showLog("Exiting fastestResetImageBtn");
            }
        });

//        Switch to activate gyroscope movement
        phoneTiltSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                if (gridMap.getAutoUpdate()) {
                    updateStatus("Please press 'MANUAL'");
                    phoneTiltSwitch.setChecked(false);
                } else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    if (phoneTiltSwitch.isChecked()) {
                        showToast("Tilt motion control: ON");
                        phoneTiltSwitch.setPressed(true);
                        sensorManager.registerListener(ControlFragment.this, sensor, sensorManager.SENSOR_DELAY_NORMAL);
                        sensorHandler.post(sensorDelay);
                    } else {
                        showToast("Tilt motion control: OFF");
                        showLog("unregistering Sensor Listener");
                        try {
                            sensorManager.unregisterListener(ControlFragment.this);
                        } catch (IllegalArgumentException e) {
                            e.printStackTrace();
                        }
                        sensorHandler.removeCallbacks(sensorDelay);
                    }
                } else {
                    updateStatus("Please press 'STARTING POINT'");
                    phoneTiltSwitch.setChecked(false);
                }
                if (phoneTiltSwitch.isChecked()) {
                    compoundButton.setText("TILT ON");
                } else {
                    compoundButton.setText("TILT OFF");
                }
            }
        });

        calibrateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked Calibrate Button");
                MainActivity.printMessage("CALIBRATE");
                MapTabFragment.manualUpdateRequest = true;
                showLog("Exiting Calibrate Button");
            }
        });

        return root;
    }

    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }

    boolean sensorFlag = false;
    Handler sensorHandler = new Handler();

    private final Runnable sensorDelay = new Runnable() {
        @Override
        public void run() {
            sensorFlag = true;
            sensorHandler.postDelayed(this, 1000);
        }
    };

    //    Sensor based movement
    @Override
    public void onSensorChanged(SensorEvent event) {
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        showLog("SensorChanged X: " + x);
        showLog("SensorChanged Y: " + y);
        showLog("SensorChanged Z: " + z);

        if (sensorFlag) {
            if (y < -2) {
                showLog("Sensor Move Forward Detected");
                gridMap.moveRobot("forward");
                MainActivity.printMessage("w");
            } else if (y > 2) {
                showLog("Sensor Move Backward Detected");
                gridMap.moveRobot("back");
                MainActivity.printMessage("s");
            } else if (x > 2) {
                showLog("Sensor Move Left Detected");
                gridMap.moveRobot("left");
                MainActivity.printMessage("a");
            } else if (x < -2) {
                showLog("Sensor Move Right Detected");
                gridMap.moveRobot("right");
                MainActivity.printMessage("d");
            }
        }
        sensorFlag = false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            sensorManager.unregisterListener(ControlFragment.this);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
    }

    private void updateStatus(String message) {
        Toast toast = Toast.makeText(getContext(), message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.show();
    }

    public static Button getCalibrateButton() {
        return calibrateButton;
    }
}
