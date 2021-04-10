package com.example.MDP_Android.ui.main;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;

import com.example.MDP_Android.MainActivity;
import com.example.MDP_Android.R;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Locale;

// Communication fragment to view messages received and send messages
public class CommFragment extends Fragment {

    //    Initialise variables
    public static final Integer RecordAudioRequestCode = 1;
    private static final String ARG_SECTION_NUMBER = "section_number";
    private static final String TAG = "CommFragment";
    private static GridMap gridMap;
    private static TextView messageReceivedTextView;
    private EditText typeBoxEditText;
    private SpeechRecognizer speechRecognizer;
    private PageViewModel pageViewModel;

    SharedPreferences sharedPreferences;
    FloatingActionButton sendFAB, voiceFAB;

    //    Setup page variables
    public static CommFragment newInstance(int index) {
        CommFragment fragment = new CommFragment();
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
        if (ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
            checkPermission();
        pageViewModel.setIndex(index);
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.RECORD_AUDIO}, RecordAudioRequestCode);
        }
    }

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View root = null;
        if (MainActivity.colorState == 0) {
            root = inflater.inflate(R.layout.activity_comms, container, false);
        } else {
            root = inflater.inflate(R.layout.activity_comms_state2, container, false);
        }
        gridMap = MainActivity.getGridMap();

//         Initialise buttons for send and voice
        sendFAB = (FloatingActionButton) root.findViewById(R.id.messageButton);
        voiceFAB = (FloatingActionButton) root.findViewById(R.id.voiceButton);

//         Initialise communication Box
        messageReceivedTextView = (TextView) root.findViewById(R.id.messageReceivedTextView);
        messageReceivedTextView.setMovementMethod(new ScrollingMovementMethod());
        typeBoxEditText = (EditText) root.findViewById(R.id.typeBoxEditText);

//         Initialise speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(getActivity());
        final Intent speechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        speechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());

//         Retrieve shared preferences
        sharedPreferences = getActivity().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);

//        Handles voice recognizer
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle bundle) {
            }

            @Override
            public void onRmsChanged(float v) {
            }

            @Override
            public void onBufferReceived(byte[] bytes) {
            }

            @Override
            public void onEndOfSpeech() {
            }

            @Override
            public void onError(int i) {
            }

            @Override
            public void onPartialResults(Bundle bundle) {
            }

            @Override
            public void onEvent(int i, Bundle bundle) {
            }

            @Override
            public void onBeginningOfSpeech() {
                typeBoxEditText.setHint("Listening...");
            }

            @Override
            public void onResults(Bundle bundle) {
                ArrayList<String> data = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String voiceText = data.get(0);
                showToast("Voice: " + voiceText);
                showLog("Voice: " + voiceText);

                switch (voiceText) {
                    case ("move forward"):
                        gridMap.moveRobot("forward");
                        MainActivity.printMessage("w");
                        break;
                    case ("turn right"):
                        gridMap.moveRobot("right");
                        MainActivity.printMessage("d");
                        break;
                    case ("turn left"):
                        gridMap.moveRobot("left");
                        MainActivity.printMessage("a");
                        break;
                    default:
                        MainActivity.printMessage("invalid voice command");
                        break;
                }
                typeBoxEditText.setHint("Type something...");
            }
        });

//        Sends message to bluetooth sending service
        sendFAB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked sendTextBtn");
                String sentText = "" + typeBoxEditText.getText().toString();

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("message", sharedPreferences.getString("message", "") + '\n' + sentText);
                editor.commit();
                messageReceivedTextView.setText(sharedPreferences.getString("message", ""));
                typeBoxEditText.setText("");

                if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
                    byte[] bytes = sentText.getBytes(Charset.defaultCharset());
                    BluetoothConnectionService.write(bytes);
                }
                showLog("Exiting sendTextBtn");
            }
        });

        voiceFAB.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    showLog("voiceFAB Action Up");
                    speechRecognizer.stopListening();
                }
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    showLog("voiceFAB Action Down");
                    speechRecognizer.startListening(speechRecognizerIntent);
                }
                return false;
            }
        });

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        speechRecognizer.destroy();
    }

    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    public static TextView getMessageReceivedTextView() {
        return messageReceivedTextView;
    }

    private void showToast(String message) {
        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == RecordAudioRequestCode && grantResults.length > 0) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                showToast("Permission Granted");
        }
    }
}