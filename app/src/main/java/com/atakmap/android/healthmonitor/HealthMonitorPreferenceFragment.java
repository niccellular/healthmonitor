package com.atakmap.android.healthmonitor;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.util.Log;

import com.atakmap.android.gui.PanListPreference;
import com.atakmap.android.gui.PanPreference;
import com.atakmap.android.healthmonitor.DeviceList;
import com.atakmap.android.healthmonitor.plugin.R;
import com.atakmap.android.preference.PluginPreferenceFragment;

import org.achartengine.tools.Pan;

import java.util.List;
import java.util.Locale;


public class HealthMonitorPreferenceFragment extends PluginPreferenceFragment {
    private static String TAG = "HealthMonitorPreferenceFragment";
    private static Context sPluginContext;
    private static DeviceList sDevicesList;

    private PanListPreference commDevicePreference;

    public HealthMonitorPreferenceFragment() {
        super(sPluginContext, R.xml.preferences);
    }

    @SuppressLint("ValidFragment")
    public HealthMonitorPreferenceFragment(final Context pluginContext, final DeviceList devicesList) {
        super(pluginContext, R.xml.preferences);
        this.sPluginContext = pluginContext;
        this.sDevicesList = devicesList;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        commDevicePreference = (PanListPreference) findPreference("plugin_health_monitor_key_set_comm_device");
        updateCommDevice(commDevicePreference, sDevicesList.getDevices());

        ((PanPreference) findPreference("plugin_health_monitor_key_refresh_comm_devices")).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                updateCommDevice(commDevicePreference, sDevicesList.getDevices());
                return true;
            }
        });

    }

    private void updateCommDevice(PanListPreference commDevicePreference, List<String[]> devices) {
        String[] k = new String[devices.size()];
        String[] v = new String[devices.size()];
        for (int i=0; i<devices.size(); i++) {
            k[i] = devices.get(i)[0];
            v[i] = devices.get(i)[1];
            Log.d(TAG, String.format(Locale.US, "Device: %s MAC: %s", k[i],v[i]));
        }
        commDevicePreference.setEntries(k);
        commDevicePreference.setEntryValues(v);
    }

    @Override
    public String getSubTitle() {
        return getSubTitle("Tool Preferences", "Health Monitor Preferences");
    }
}
