package com.atakmap.android.healthmonitor.plugin;


import android.app.Activity;

import com.atak.plugins.impl.AbstractPlugin;
import com.atak.plugins.impl.PluginContextProvider;
import com.atakmap.android.healthmonitor.HealthMonitorMapComponent;
import com.atakmap.android.maps.MapView;

import gov.tak.api.plugin.IServiceController;

public class HealthMonitorLifecycle extends AbstractPlugin {
    public HealthMonitorLifecycle(IServiceController serviceController) {
        super(serviceController, new com.atakmap.android.healthmonitor.plugin.HealthMonitorTool(serviceController.getService(PluginContextProvider.class).getPluginContext()), new HealthMonitorMapComponent());

    }

    public static Activity getActivity() {
        return (Activity) MapView.getMapView().getContext();
    }
}