
package com.atakmap.android.healthmonitor;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.view.Gravity;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.atakmap.android.cot.MarkerDetailHandler;
import com.atakmap.android.cot.detail.CotDetailHandler;
import com.atakmap.android.cot.detail.CotDetailManager;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;

import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.dropdown.DropDownMapComponent;

import com.atakmap.android.healthmonitor.plugin.HealthMonitorLifecycle;
import com.atakmap.app.preferences.ToolsPreferenceFragment;
import com.atakmap.comms.CommsMapComponent;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.log.Log;
import com.atakmap.android.healthmonitor.plugin.R;

public class HealthMonitorMapComponent extends DropDownMapComponent {

    private static final String TAG = "HealthMonitorMapComponent";

    private Context pluginContext;

    private HealthMonitorDropDownReceiver ddr;

    private CotDetailHandler healthDetailHandler;

    public void onCreate(final Context context, Intent intent, final MapView view) {

        context.setTheme(R.style.ATAKPluginTheme);
        super.onCreate(context, intent, view);
        pluginContext = context;

        ddr = new HealthMonitorDropDownReceiver(view, context);

        Log.d(TAG, "registering the plugin filter");
        DocumentedIntentFilter ddFilter = new DocumentedIntentFilter();
        ddFilter.addAction(HealthMonitorDropDownReceiver.SHOW_PLUGIN);
        registerDropDownReceiver(ddr, ddFilter);

        DeviceList devicesList = new DeviceList(view.getContext());
        ToolsPreferenceFragment
                .register(
                        new ToolsPreferenceFragment.ToolPreference(
                                "Health Monitor Preferences",
                                "Preferences for the Health Monitor Plugin",
                                "healthMonitorPreference",
                                context.getResources().getDrawable(R.drawable.ic_launcher, null),
                                new HealthMonitorPreferenceFragment(context, devicesList)));


        CotDetailManager.getInstance().registerHandler(
            healthDetailHandler = new CotDetailHandler("__health") {
                private final String TAG = "HealthCotDetailHandler";
                private int getInt(String val, int fallback) {
                    try {
                        return Integer.parseInt(val);
                    } catch (NumberFormatException e) {
                        return fallback;
                    }
                }
                @Override
                public CommsMapComponent.ImportResult toItemMetadata(MapItem mapItem, CotEvent event, CotDetail detail) {
                    int bpm = getInt(detail.getAttribute("bpm"),0);
                    boolean needToAddView = false;
                    mapItem.setMetaInteger("bpm", bpm);
                    String callsign = event.findDetail("contact").getAttribute("callsign");
                    event.setType("a-f-G-U-C-I-h-h");
                    TableRow row = ddr.healthTable.findViewById(callsign.hashCode());
                    TextView callsignTV;
                    TextView BPM;
                    TextView space;
                    if (row == null) {
                        Log.d(TAG, "Row not found for: " + callsign);
                        needToAddView = true;

                        row = new TableRow(pluginContext);
                        row.setId(callsign.hashCode());
                        TableRow.LayoutParams lp = new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.WRAP_CONTENT);
                        row.setLayoutParams(lp);


                        callsignTV = new TextView(pluginContext);
                        callsignTV.setId(callsign.hashCode()+1);
                        callsignTV.setText("Callsign: " + callsign);
                        row.addView(callsignTV);

                        space = new TextView(pluginContext);
                        space.setText("\t");
                        row.addView(space);

                        BPM = new TextView(pluginContext);
                        BPM.setId(callsign.hashCode()+2);
                        BPM.setText("BPM: " + String.valueOf(bpm));
                        BPM.setGravity(Gravity.RIGHT);
                        row.addView(BPM);
                    } else {
                        BPM = row.findViewById(callsign.hashCode()+2);
                        BPM.setText("BPM: " + String.valueOf(bpm));
                    }

                    if (needToAddView) {
                        TableRow finalRow = row;
                        HealthMonitorLifecycle.getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                ddr.healthTable.addView(finalRow, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.WRAP_CONTENT));
                            }
                        });
                    }
                    return CommsMapComponent.ImportResult.SUCCESS;
                }
                @Override
                public boolean toCotDetail(MapItem item, CotEvent event, CotDetail root) {
                    CotDetail health = new CotDetail("__health");
                    health.setAttribute("bpm", String.valueOf(item.getMetaInteger("bpm", 0)));
                    root.addChild(health);
                    return true;
                }
            });
    }

    @Override
    protected void onDestroyImpl(Context context, MapView view) {
        super.onDestroyImpl(context, view);
        CotDetailManager.getInstance().unregisterHandler(healthDetailHandler);
    }

}
