package com.legendsayantan.recall;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;


public class NotificationListener extends NotificationListenerService {
    Boolean main=false;
    static Bitmap image;
    static String caller;
    static long time;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
    }

    @Override
    public void onListenerConnected() {
        main=true;
        System.out.println("connected");
        super.onListenerConnected();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        try {
            Bundle extras = sbn.getNotification().extras;
            String title = extras.getString("android.title");
            String text = "";
            try {
                text = extras.getCharSequence("android.text").toString();
                SettingsActivity.sendMessage(text);
            }catch (Exception e){ }
            boolean verifyResult = BroadcastStarter.checkToCall(title, text, getApplicationContext());
            System.out.println("Verifying to call: " + verifyResult);
            if (verifyResult) {
                time = sbn.getPostTime();
                Intent msgrcv = new Intent("Msg");
                image = sbn.getNotification().largeIcon;
                caller=title;
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(msgrcv);
                Notification n = sbn.getNotification();
                if(n.actions!=null)
                    for(Notification.Action action : n.actions){
                        String actionname = action.title.toString();
                        System.out.println(actionname);
                        if(actionname.contains("Mark as",true)){
                            action.actionIntent.send();
                            break;
                        }
                }
                Intent intent2 = new Intent();
                intent2.setClass(getApplicationContext(), Floating.class);
                intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    getApplicationContext().startForegroundService(intent2);
                }else getApplicationContext().startService(intent2);
            }
        }catch (Exception e){
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground()
    {
        String NOTIFICATION_CHANNEL_ID = "com.legendsayantan.recall.background";
        String channelName = "Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle("App is running in background")
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .build();
        startForeground(2, notification);
    }
}