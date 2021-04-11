package com.example.MDP_Android.ui.main;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.MDP_Android.MainActivity;
import com.example.MDP_Android.R;

import org.json.JSONException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

// Map fragment to control map updates
public class MapTabFragment extends Fragment {

    //    Initialise variables
    public static boolean manualUpdateRequest = false;

    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "MapFragment";
    private PageViewModel pageViewModel;

    static final int REQUEST_IMAGE_CAPTURE = 1;

    Button resetMapBtn, updateButton;
    ImageButton directionChangeImageBtn, exploredImageBtn, obstacleImageBtn, clearImageBtn, mapCaptureBtn;
    ToggleButton setStartPointToggleBtn, setWayPointToggleBtn;
    Switch manualAutoToggleBtn;
    GridMap gridMap;
    File capturedImage;

    public static MapTabFragment newInstance(int index) {
        MapTabFragment fragment = new MapTabFragment();
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

    //    Setup page variables
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = null;
        if (MainActivity.colorState == 0) {
            root = inflater.inflate(R.layout.activity_map, container, false);
        } else {
            root = inflater.inflate(R.layout.activity_map_state2, container, false);
        }

        gridMap = MainActivity.getGridMap();
        final DirectionFragment directionFragment = new DirectionFragment();

        resetMapBtn = root.findViewById(R.id.resetMapBtn);
        setStartPointToggleBtn = root.findViewById(R.id.setStartPointToggleBtn);
        setWayPointToggleBtn = root.findViewById(R.id.setWaypointToggleBtn);
        directionChangeImageBtn = root.findViewById(R.id.directionChangeImageBtn);
        exploredImageBtn = root.findViewById(R.id.exploredImageBtn);
        obstacleImageBtn = root.findViewById(R.id.obstacleImageBtn);
        clearImageBtn = root.findViewById(R.id.clearImageBtn);
        mapCaptureBtn = root.findViewById(R.id.mapCaptureBtn);
        manualAutoToggleBtn = root.findViewById(R.id.manualAutoToggleBtn);
        updateButton = root.findViewById(R.id.updateButton);

        resetMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked resetMapBtn");
                showToast("Resetting map...");
                gridMap.resetMap();
            }
        });

        setStartPointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked setStartPointToggleBtn");
                if (setStartPointToggleBtn.getText().equals("STARTING POINT"))
                    showToast("Cancelled selecting starting point");
                else if (setStartPointToggleBtn.getText().equals("CANCEL") && !gridMap.getAutoUpdate()) {
                    showToast("Please select starting point");
                    gridMap.setStartCoordinateStatus(true);
                    gridMap.toggleCheckedBtn("setStartPointToggleBtn");
                } else
                    showToast("Please select manual mode");
                showLog("Exiting setStartPointToggleBtn");
            }
        });

        setWayPointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked setWayPointToggleBtn");
                if (setWayPointToggleBtn.getText().equals("WAYPOINT"))
                    showToast("Cancelled selecting waypoint");
                else if (setWayPointToggleBtn.getText().equals("CANCEL")) {
                    showToast("Please select waypoint");
                    gridMap.setWayPointStatus(true);
                    gridMap.toggleCheckedBtn("setWayPointToggleBtn");
                } else
                    showToast("Please select manual mode");
                showLog("Exiting setWayPointToggleBtn");
            }
        });

        directionChangeImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked directionChangeImageBtn");
                directionFragment.show(getActivity().getFragmentManager(), "Direction Fragment");
                showLog("Exiting directionChangeImageBtn");
            }
        });

        exploredImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked exploredImageBtn");
                if (!gridMap.getExploredStatus()) {
                    showToast("Please check cell");
                    gridMap.setExploredStatus(true);
                    gridMap.toggleCheckedBtn("exploredImageBtn");
                } else if (gridMap.getExploredStatus())
                    gridMap.setSetObstacleStatus(false);
                showLog("Exiting exploredImageBtn");
            }
        });

        obstacleImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked obstacleImageBtn");
                if (!gridMap.getSetObstacleStatus()) {
                    showToast("Please plot obstacles");
                    gridMap.setSetObstacleStatus(true);
                    gridMap.toggleCheckedBtn("obstacleImageBtn");
                } else if (gridMap.getSetObstacleStatus())
                    gridMap.setSetObstacleStatus(false);
                showLog("Exiting obstacleImageBtn");
            }
        });

        clearImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked clearImageBtn");
                if (!gridMap.getUnSetCellStatus()) {
                    showToast("Please remove cells");
                    gridMap.setUnSetCellStatus(true);
                    gridMap.toggleCheckedBtn("clearImageBtn");
                } else if (gridMap.getUnSetCellStatus())
                    gridMap.setUnSetCellStatus(false);
                showLog("Exiting clearImageBtn");
            }
        });

        manualAutoToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked manualAutoToggleBtn");
                if (manualAutoToggleBtn.getText().equals("MANUAL")) {
                    try {
                        gridMap.setAutoUpdate(true);
                        gridMap.toggleCheckedBtn("None");
                        updateButton.setClickable(false);
                        updateButton.setTextColor(Color.GRAY);
                        ControlFragment.getCalibrateButton().setClickable(false);
                        ControlFragment.getCalibrateButton().setTextColor(Color.GRAY);
                        manualAutoToggleBtn.setText("AUTO");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("AUTO mode");
                } else if (manualAutoToggleBtn.getText().equals("AUTO")) {
                    try {
                        gridMap.setAutoUpdate(false);
                        gridMap.toggleCheckedBtn("None");
                        updateButton.setClickable(true);
                        updateButton.setTextColor(Color.BLACK);
                        ControlFragment.getCalibrateButton().setClickable(true);
                        ControlFragment.getCalibrateButton().setTextColor(Color.BLACK);
                        manualAutoToggleBtn.setText("MANUAL");
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("MANUAL mode");
                }
                showLog("Exiting manualAutoToggleBtn");
            }
        });

        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked updateButton");
                GridMap.clearObstacleCoordinate();
                MainActivity.printMessage("sendArena");
                manualUpdateRequest = true;
                showLog("Exiting updateButton");
            }
        });

        mapCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked mapCaptureBtn");
                Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                capturedImage = tempImageFile();
                Uri photoURI = FileProvider.getUriForFile(getContext(), "com.example.android.fileprovider", capturedImage);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                MainActivity.printMessage("Image Captured");
                showLog("Released mapCaptureBtn");
            }
        });

        return root;
    }

    private File tempImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "PNG_" + timeStamp + "_";
        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = null;
        try {
            imageFile = File.createTempFile(imageFileName, ".png", storageDir);
        } catch (IOException e) {
            e.printStackTrace();
        }
        showLog("Image Saved Source: " + imageFile.getAbsolutePath());
        return imageFile;
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}