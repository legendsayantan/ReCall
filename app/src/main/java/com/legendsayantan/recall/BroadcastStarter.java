package com.legendsayantan.recall;

import static com.legendsayantan.recall.SettingsActivity.activity;
import static com.legendsayantan.recall.SettingsActivity.getContacts;
import static com.legendsayantan.recall.SettingsActivity.sharedPreferences;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class BroadcastStarter extends Service {
    static List<ContactModel> allContacts = new ArrayList<>();
    static SharedPreferences preferences;
    static String contactPref = "all" ,wake;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(new Intent(getApplicationContext(),NotificationListener.class));
        }else startService(new Intent(getApplicationContext(),NotificationListener.class));
        //LocalBroadcastManager.getInstance(this).registerReceiver(onNotice, new IntentFilter("Msg"));
        allContacts=SettingsActivity.allContacts;

        preferences= getApplicationContext().getSharedPreferences("preferences", Activity.MODE_PRIVATE);
    }
    private static BroadcastReceiver onNotice= new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Intent intent2 = new Intent();
            intent2.setClass(context.getApplicationContext(), Floating.class);
            intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            System.out.println("Broadcast received ");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.getApplicationContext().startForegroundService(intent2);
            }else context.getApplicationContext().startService(intent2);
        }
    };
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        startTimer();
        return START_STICKY;
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
    private Timer timer;
    private TimerTask timerTask;
    public void startTimer() {
        timer = new Timer();
        timerTask = new TimerTask() {
            public void run() {
            }
        };
        timer.schedule(timerTask, 1000, 1000); //
    }
    public static String parseContact(String contactString, Context context){
        for(int i = 0; i<allContacts.size();i++){
            if(allContacts.get(i).name.equals(contactString)){
                return allContacts.get(i).mobileNumber;
            }
        }
        if(preferences.getInt("reply",0)==1)return contactString;
        else {
            Toast.makeText(context, "ERROR: Could not identify number to call", Toast.LENGTH_SHORT).show();
            return "00";
        }
    }
    public static void callNow(String number,Context context){
        if (number.equals("") || number.equals("00")) {
            Toast.makeText(activity.getApplicationContext(),"Could not connect Call",Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setData(Uri.parse("tel:" + number));
        context.startActivity(intent);
    }
    public static boolean checkToCall(String title, String text,Context context){
        preferences= context.getSharedPreferences("preferences", Activity.MODE_PRIVATE);
        text.trim();
        String rawTitle = title;
        title="("+title+")";
        System.out.println(text+" "+preferences.getInt("reply",0));
        switch (preferences.getInt("reply",0)){
            case 1:
                if(text.equals(preferences.getString("wakeword","Recallme"))){
                    System.out.println("Message from everyone");
                    return true;
                }
            case 2:
                if(text.equals(preferences.getString("wakeword","Recallme"))){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!(context.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED))
                            if (allContacts.size() == 0) {
                                allContacts.clear();
                                allContacts = getContacts(context.getApplicationContext());
                            }
                    }
                    else if (allContacts.size() == 0) {
                        allContacts.clear();
                        allContacts = getContacts(context.getApplicationContext());
                    }
                    for(int i =0;i<allContacts.size();i++) {
                        if(allContacts.get(i).name.equals(rawTitle)){
                            System.out.println("Message from a contact");
                            return true;
                        }
                    }
                }
            case 3:
                if(text.equals(preferences.getString("wakeword","Recallme"))){
                    if(PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getString("specifiedcontact","").contains(title)){
                        System.out.println("Message from specified contact");
                        return true;
                    }
                }
        }
        return false;
    }

    @Override
    public void onDestroy() {
        stopService(new Intent(getApplicationContext(),NotificationListener.class));
        stopSelf();
        super.onDestroy();
    }
}