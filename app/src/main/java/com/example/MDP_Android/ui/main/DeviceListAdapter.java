package com.example.MDP_Android.ui.main;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.example.MDP_Android.R;

import java.util.ArrayList;

public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {

    // Initialise variables
    private LayoutInflater layoutInflater;
    private ArrayList<BluetoothDevice> myDevices;
    private int viewResourceID;

    //    Setup page variables
    public DeviceListAdapter(Context context, int tvResourceId, ArrayList<BluetoothDevice> devices) {
        super(context, tvResourceId, devices);
        this.myDevices = devices;
        layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        viewResourceID = tvResourceId;
    }

    //    Bluetooth devices information
    public View getView(int position, View convertView, ViewGroup parent) {
        Log.d("DeviceListAdapter", "Getting View");
        convertView = layoutInflater.inflate(viewResourceID, null);
        BluetoothDevice device = myDevices.get(position);

        if (device != null) {
            TextView deviceName = (TextView) convertView.findViewById(R.id.deviceName);
            TextView deviceAddress = (TextView) convertView.findViewById(R.id.deviceAddress);
            if (deviceName != null) {
                deviceName.setText(device.getName());
            }
            if (deviceAddress != null) {
                deviceAddress.setText(device.getAddress());
            }
        }

        return convertView;
    }
}