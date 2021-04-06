package com.example.MDP_Android.ui.main;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.content.Context;
import android.hardware.Camera;
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
import android.widget.ImageView;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

// Map fragment to control map updates
public class MapTabFragment extends Fragment {

    //    Initialise variables
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "MapFragment";
    static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int RESULT_OK = 2;

    private PageViewModel pageViewModel;

    Button resetMapBtn, updateButton;
    ImageButton directionChangeImageBtn, exploredImageBtn, obstacleImageBtn, clearImageBtn, mapCaptureBtn;
    ToggleButton setStartPointToggleBtn, setWaypointToggleBtn;
    Switch manualAutoToggleBtn;
    GridMap gridMap;
    ImageView imageCaptureView;
    private static boolean autoUpdate = false;
    public static boolean manualUpdateRequest = false;

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
        View root = inflater.inflate(R.layout.activity_map, container, false);

        gridMap = MainActivity.getGridMap();
        final DirectionFragment directionFragment = new DirectionFragment();

        resetMapBtn = root.findViewById(R.id.resetMapBtn);
        setStartPointToggleBtn = root.findViewById(R.id.setStartPointToggleBtn);
        setWaypointToggleBtn = root.findViewById(R.id.setWaypointToggleBtn);
        directionChangeImageBtn = root.findViewById(R.id.directionChangeImageBtn);
        exploredImageBtn = root.findViewById(R.id.exploredImageBtn);
        obstacleImageBtn = root.findViewById(R.id.obstacleImageBtn);
        clearImageBtn = root.findViewById(R.id.clearImageBtn);
        mapCaptureBtn = root.findViewById(R.id.mapCaptureBtn);
        manualAutoToggleBtn = root.findViewById(R.id.manualAutoToggleBtn);
        updateButton = root.findViewById(R.id.updateButton);
        imageCaptureView = root.findViewById(R.id.imageCaptureView);

        resetMapBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked resetMapBtn");
                showToast("Reseting map...");
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
                    gridMap.setStartCoordStatus(true);
                    gridMap.toggleCheckedBtn("setStartPointToggleBtn");
                } else
                    showToast("Please select manual mode");
                showLog("Exiting setStartPointToggleBtn");
            }
        });

        setWaypointToggleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked setWaypointToggleBtn");
                if (setWaypointToggleBtn.getText().equals("WAYPOINT"))
                    showToast("Cancelled selecting waypoint");
                else if (setWaypointToggleBtn.getText().equals("CANCEL")) {
                    showToast("Please select waypoint");
                    gridMap.setWaypointStatus(true);
                    gridMap.toggleCheckedBtn("setWaypointToggleBtn");
                } else
                    showToast("Please select manual mode");
                showLog("Exiting setWaypointToggleBtn");
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
                        autoUpdate = true;
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
                        autoUpdate = false;
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
                GridMap.clearObstacleCoord();
                MainActivity.printMessage("sendArena");
                manualUpdateRequest = true;
                showLog("Exiting updateButton");
            }
        });

        mapCaptureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked resetMapBtn");
                Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                try {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                } catch (ActivityNotFoundException e) {
                    // display error state to the user
                }
                // Ensure that there's a camera activity to handle the intent
//                if (takePictureIntent.resolveActivity(getContext().getPackageManager()) != null) {
//                    // Create the File where the photo should go
//                    File photoFile = null;
//                    try {
//                        photoFile = createImageFile();
//                    } catch (IOException ex) {
//                        ex.printStackTrace();
//                    }
//                    // Continue only if the File was successfully created
//                    if (photoFile != null) {
//                        Uri photoURI = FileProvider.getUriForFile(getContext(),
//                                "com.example.android.fileprovider",
//                                photoFile);
//                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
//                        startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
//                        galleryAddPic();
//                    }
//                }
            }
        });

        return root;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            showLog("Reached activityResult");
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            showLog("imageBitmap: " + imageBitmap.toString());
            imageCaptureView.setImageBitmap(imageBitmap);

            File imageFile = createImageFile(imageBitmap);

            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = imageFile.getAbsolutePath();
            showLog("Image Saved Source: " + currentPhotoPath);
            showLog("Exiting activityResult");
        }
    }

    String currentPhotoPath;

    private File createImageFile(Bitmap imageBitmap) {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = null;
        try {
            imageFile = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".png",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.PNG, 100, bos);
        byte[] bitmapdata = bos.toByteArray();

        //write the bytes in file
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(imageFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            fos.write(bitmapdata);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return imageFile;
    }

    private void showLog(String message) {
        Log.d(TAG, message);
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
}