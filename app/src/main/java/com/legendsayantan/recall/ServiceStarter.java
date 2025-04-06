package com.legendsayantan.recall;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.PreferenceManager;

public class ServiceStarter extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if(PreferenceManager.getDefaultSharedPreferences(context).getBoolean("service",false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(new Intent(context, BroadcastStarter.class));
            } else {
                context.startService(new Intent(context, BroadcastStarter.class));
            }
        }
    }
}
