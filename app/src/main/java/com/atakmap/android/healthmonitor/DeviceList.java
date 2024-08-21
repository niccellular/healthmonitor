package com.atakmap.android.healthmonitor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DeviceList {
    private static final String TAG = "DeviceList";

    private final Context mAtakContext;
    private List<String[]> devices;

    public DeviceList(Context atakContext) {
        mAtakContext = atakContext;
    }


    @SuppressLint("MissingPermission")
    public List<String[]> getDevices() {
        devices = new ArrayList<>();
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        @SuppressLint("MissingPermission") Set<BluetoothDevice> bluetoothDevices = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : bluetoothDevices) {
            if (device.getName() != null ) {
                devices.add(new String[] {device.getName(), device.getAddress()});
            }
        }

        return devices;
    }
}
