
package com.atakmap.android.healthmonitor;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.atak.plugins.impl.PluginLayoutInflater;
import com.atakmap.android.cot.CotMapComponent;
import com.atakmap.android.importexport.CotEventFactory;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.healthmonitor.plugin.HealthMonitorLifecycle;
import com.atakmap.android.healthmonitor.plugin.R;
import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;

import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class HealthMonitorDropDownReceiver extends DropDownReceiver implements
        OnStateListener {

    public static final String TAG = "HealthMonitorDropDownReceiver";

    public static final String SHOW_PLUGIN = "com.atakmap.android.healthmonitor.SHOW_PLUGIN";
    private final View templateView;
    private final Context pluginContext;
    private Button scan;
    private TextView heartRateTV, name;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic;
    public final BluetoothGattCallback mGattCallback;
    private BroadcastReceiver gattUpdateReceiver;
    public static String HEART_RATE = "0000180d-0000-1000-8000-00805f9b34fb";
    public static String HEART_RATE_MEASUREMENT = "00002a37-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    public TableLayout healthTable;


    /**************************** CONSTRUCTOR *****************************/

    @SuppressLint("MissingPermission")
    public HealthMonitorDropDownReceiver(final MapView mapView,
                                          final Context context) {
        super(mapView);
        this.pluginContext = context;

        // Remember to use the PluginLayoutInflator if you are actually inflating a custom view
        // In this case, using it is not necessary - but I am putting it here to remind
        // developers to look at this Inflator
        templateView = PluginLayoutInflater.inflate(context,
                R.layout.main_layout, null);

        heartRateTV = templateView.findViewById(R.id.heartRate);
        healthTable = templateView.findViewById(R.id.table);
        name = templateView.findViewById(R.id.name);

        final int[] count = {0};
        this.mGattCallback = new BluetoothGattCallback() {
            @Override  // android.bluetooth.BluetoothGattCallback
            public void onCharacteristicChanged(BluetoothGatt arg2, BluetoothGattCharacteristic characteristic) {
                int flag = characteristic.getProperties();
                int format = -1;
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                }
                final int heartRate = characteristic.getIntValue(format, 1);

                HealthMonitorLifecycle.getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        heartRateTV.setText(String.format(Locale.US, "%d bpm", heartRate));
                    }
                });
                if (count[0]++ % 15 == 0) {
                    Marker self = getMapView().getSelfMarker();
                    self.setMetaInteger("bpm", heartRate);
                    self.setType("a-f-G-U-C-I-h-h");
                    CotEvent saveMarker = CotEventFactory.createCotEvent(self);
                    CotMapComponent.getExternalDispatcher().dispatch(saveMarker);
                }
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onCharacteristicRead(BluetoothGatt arg1, BluetoothGattCharacteristic characteristic, int arg3) {
                Log.d(TAG, "onCharacteristicRead " + arg3);
                if(arg3 == 0) {
                }
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onCharacteristicWrite(BluetoothGatt arg2, BluetoothGattCharacteristic arg3, int arg4) {
            }

            @SuppressLint("ResourceAsColor")
            @Override  // android.bluetooth.BluetoothGattCallback
            public void onConnectionStateChange(BluetoothGatt arg5, int arg6, int arg7) {
                Log.d(TAG, "onConnectionStateChange status: " + arg6);
                Log.d(TAG, "onConnectionStateChange newState: " + arg7);
               if(arg7 == 0) {
                    Log.i(TAG, "Disconnected from GATT server.");
                    HealthMonitorLifecycle.getActivity().runOnUiThread(() -> {
                        scan.setBackgroundResource(R.color.red);
                        scan.setText("*NOT* Connected");
                        scan.setVisibility(View.VISIBLE);
                    });

                }

               if(arg7 == 2) {
                   HealthMonitorLifecycle.getActivity().runOnUiThread(() -> {
                       scan.setBackgroundResource(R.color.green);
                       scan.setText("Connected");
                       scan.setVisibility(View.VISIBLE);
                   });

                   mBluetoothGatt.discoverServices();
               }

                super.onConnectionStateChange(arg5, arg6, arg7);
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onDescriptorRead(BluetoothGatt arg1, BluetoothGattDescriptor arg2, int arg3) {
                super.onDescriptorRead(arg1, arg2, arg3);
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onDescriptorWrite(BluetoothGatt arg3, BluetoothGattDescriptor arg4, int arg5) {
                Intent i = new Intent("com.atakmap.app.civ.READ_HEART_RATE");
                mapView.getContext().sendBroadcast(i);
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onMtuChanged(BluetoothGatt arg2, int arg3, int arg4) {
                super.onMtuChanged(arg2, arg3, arg4);
                if(arg4 == 0) {
                    boolean v2 = mBluetoothGatt.discoverServices();
                    Log.i(TAG, "5.0 attempting to start service discovery: " + v2);
                    return;
                }

                Log.i(TAG, "Failed to set MTU.");
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onReadRemoteRssi(BluetoothGatt arg2, int arg3, int arg4) {
                Log.i(TAG, "rssi = " + arg3);
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onReliableWriteCompleted(BluetoothGatt arg1, int arg2) {
                super.onReliableWriteCompleted(arg1, arg2);
            }

            @Override  // android.bluetooth.BluetoothGattCallback
            public void onServicesDiscovered(BluetoothGatt arg3, int arg4) {
                Log.w(TAG, "onServicesDiscovered received: " + arg4);

                BluetoothGattService s = mBluetoothGatt.getService(UUID.fromString(HEART_RATE));
                if (s == null) {
                    mBluetoothGatt.disconnect();
                    return;
                }
                mBluetoothGattCharacteristic = s.getCharacteristic(UUID.fromString(HEART_RATE_MEASUREMENT));
                mBluetoothGatt.setCharacteristicNotification(mBluetoothGattCharacteristic, true);
                BluetoothGattDescriptor d = mBluetoothGattCharacteristic.getDescriptor(UUID.fromString(CLIENT_CHARACTERISTIC_CONFIG));
                d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGatt.writeDescriptor(d);
            }
        };

        gattUpdateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final String action = intent.getAction();
                if (action.equals("com.atakmap.app.civ.READ_HEART_RATE")) {
                    mBluetoothGatt.readCharacteristic(mBluetoothGattCharacteristic);
                }
            }
        };
        IntentFilter f = new IntentFilter();
        f.addAction("com.atakmap.app.civ.READ_HEART_RATE");
        mapView.getContext().registerReceiver(gattUpdateReceiver, f, Context.RECEIVER_EXPORTED);

        scan = templateView.findViewById(R.id.Scan);

        scan.setOnClickListener(v -> {
            SharedPreferences sharedPreference = PreferenceManager.getDefaultSharedPreferences(mapView.getContext());
            String key = sharedPreference.getString("plugin_health_monitor_key_set_comm_device", "");
            if (key.isEmpty()) {
                Toast toast = Toast.makeText(context, "No BLE device set", Toast.LENGTH_LONG);
                toast.show();
                return;
            }
            if(!scan.getText().toString().contains("*NOT*")) {
                Toast toast = Toast.makeText(context, "Already connected", Toast.LENGTH_LONG);
                toast.show();
                return;
            }
            /*
            if(!scan.getText().toString().contains("*NOT*")) {
                mBluetoothGatt.disconnect();
                scan.setBackgroundResource(R.color.red);
                scan.setText("Scan BLE *NOT* Connected");
                scan.setVisibility(View.VISIBLE);
                name.setText("Connected to:");
                return;
            }
            */

            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

            Set<BluetoothDevice> bluetoothDevices = bluetoothAdapter.getBondedDevices();
            for (BluetoothDevice device : bluetoothDevices) {
                if (device.getName() != null) {
                   if (device.getAddress().equals(key)) {
                       mBluetoothGatt = device.connectGatt(mapView.getContext(), true, mGattCallback);
                       mBluetoothGatt.connect();
                       HealthMonitorLifecycle.getActivity().runOnUiThread(() -> name.setText("Connected to: " + device.getName()));
                   }
                }
            }
        });
    }

    /**************************** PUBLIC METHODS *****************************/

    public void disposeImpl() {
        getMapView().getContext().unregisterReceiver(gattUpdateReceiver);
    }

    /**************************** INHERITED METHODS *****************************/

    @Override
    public void onReceive(Context context, Intent intent) {

        final String action = intent.getAction();
        if (action == null)
            return;

        if (action.equals(SHOW_PLUGIN)) {

            Log.d(TAG, "showing plugin drop down");
            showDropDown(templateView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, false, this);
        }
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    @Override
    public void onDropDownVisible(boolean v) {
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
    }

    @Override
    public void onDropDownClose() {
    }

}
