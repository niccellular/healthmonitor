package com.atakmap.android.healthmonitor.plugin;

import android.content.Context;

import com.atak.plugins.impl.AbstractPluginTool;
import com.atakmap.android.healthmonitor.plugin.PluginNativeLoader;
import com.atakmap.android.healthmonitor.plugin.R;

import gov.tak.api.util.Disposable;

public class HealthMonitorTool extends AbstractPluginTool implements Disposable {

    public HealthMonitorTool(Context context) {
        super(context,
                context.getString(R.string.app_name),
                context.getString(R.string.app_name),
                context.getResources().getDrawable(R.drawable.ic_launcher),
                "com.atakmap.android.healthmonitor.SHOW_PLUGIN");
        PluginNativeLoader.init(context);
    }

    @Override
    public void dispose() {
    }

}