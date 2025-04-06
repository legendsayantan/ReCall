package com.legendsayantan.recall;


import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class Floating extends Service {
    public static ViewGroup floatView;
    private int LAYOUT_TYPE;
    public static WindowManager.LayoutParams floatWindowLayoutParam;
    public static WindowManager windowManager;
    public static ImageView closeBtn, acceptBtn;
    public static String title;
    public static TextView textView;
    public static ImageView imageView;
    Intent intentx;
    public static Bitmap photo;
    static long time;
    static boolean toStop = false;
    static SharedPreferences sharedPreferences;
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // TODO Auto-generated method stub
        startTimer();
        return START_STICKY;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // TODO Auto-generated method stub
        Intent restartService = new Intent(getApplicationContext(),
                this.getClass());
        restartService.setPackage(getPackageName());
        PendingIntent restartServicePI = PendingIntent.getService(
                getApplicationContext(), 1, restartService,
                PendingIntent.FLAG_ONE_SHOT);

        //Restart the service once it has been killed android


        AlarmManager alarmService = (AlarmManager) getApplicationContext().getSystemService(Context.ALARM_SERVICE);
        alarmService.set(AlarmManager.ELAPSED_REALTIME, SystemClock.elapsedRealtime() + 100, restartServicePI);

    }
    @Override

    public void onCreate() {

        super.onCreate();

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O)
            startMyOwnForeground();
        else
            startForeground(1, new Notification());

        toStop=false;
        title=NotificationListener.caller;
        photo=NotificationListener.image;
        time= NotificationListener.time;

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        if(photo!=null) {
            Bitmap emptyBitmap = Bitmap.createBitmap(photo.getWidth(), photo.getHeight(), photo.getConfig());
            if (photo.sameAs(emptyBitmap))
                photo = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                        R.drawable.defaultlogo);
        }else photo = BitmapFactory.decodeResource(getApplicationContext().getResources(),
                R.drawable.defaultlogo);

        LayoutInflater inflater = (LayoutInflater) getBaseContext().getSystemService(LAYOUT_INFLATER_SERVICE);

        floatView = (ViewGroup) inflater.inflate(R.layout.floating, null);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // If API Level is more than 26, we need TYPE_APPLICATION_OVERLAY
            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;

        } else {

            // If API Level is lesser than 26, then we can

            // use TYPE_SYSTEM_ERROR,

            // TYPE_SYSTEM_OVERLAY, TYPE_PHONE, TYPE_PRIORITY_PHONE.

            // But these are all

            // deprecated in API 26 and later. Here TYPE_TOAST works best.

            LAYOUT_TYPE = WindowManager.LayoutParams.TYPE_TOAST;

        }


        // Now the Parameter of the floating-window layout is set.

        // 3) Layout_Type is already set.

        // 4) Next Parameter is Window_Flag. Here FLAG_NOT_FOCUSABLE is used. But

        // problem with this flag is key inputs can't be given to the EditText.

        // This problem is solved later.

        // 5) Next parameter is Layout_Format. System chooses a format that supports

        // translucency by PixelFormat.TRANSLUCENT
        DisplayMetrics metrics = getApplicationContext().getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        floatWindowLayoutParam = new WindowManager.LayoutParams(
                width,
                height,
                LAYOUT_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        // The Gravity of the Floating Window is set.

        // The Window will appear in the center of the screen

        floatWindowLayoutParam.gravity = Gravity.CENTER;
        Timer timer2 = new Timer();
        Timer timer = new Timer();

        floatWindowLayoutParam.x = 0;
        floatWindowLayoutParam.y = 0;

        sharedPreferences= getApplicationContext().getSharedPreferences("preferences", Activity.MODE_PRIVATE);

        Date resultdate = new Date(time);
        SimpleDateFormat localDateFormat = new SimpleDateFormat("HH:mm");
        String time2x = localDateFormat.format(resultdate);

        TextView timetext = floatView.findViewById(R.id.timetext);
        timetext.setText(time2x);

        Uri notification = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(),RingtoneManager.TYPE_RINGTONE);
        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
        AudioManager am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            r.setLooping(true);
        }
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        windowManager.addView(floatView, floatWindowLayoutParam);
        imageView=floatView.findViewById(R.id.image);
        imageView.setImageBitmap(photo);

        textView = floatView.findViewById(R.id.name);
        textView.setText(title);
        acceptBtn = floatView.findViewById(R.id.accept);
        acceptBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toStop =true;
                try{
                    r.stop();
                }catch (Exception e){ }try{
                    timer.cancel();
                }catch (Exception e){ }try{
                    timer2.cancel();
                }catch (Exception e){ }
                BroadcastStarter.callNow(BroadcastStarter.parseContact(title, getApplicationContext()), getApplicationContext());
                stopService(new Intent(getApplicationContext(), Floating.class));
            }
        });
        closeBtn = floatView.findViewById(R.id.reject);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try{
                    r.stop();
                }catch (Exception e){ }try{
                    timer.cancel();
                }catch (Exception e){ }try{
                    timer2.cancel();
                }catch (Exception e){ }
                stopService(new Intent(getApplicationContext(), Floating.class));
            }
        });
        if(sharedPreferences.getBoolean("autoaccept",false)){
            TimerTask timerTask2= new TimerTask() {
                @Override
                public void run() {
                    BroadcastStarter.callNow(BroadcastStarter.parseContact(title, getApplicationContext()), getApplicationContext());
                    try{
                        r.stop();
                    }catch (Exception e){ }
                    try{
                        timer.cancel();
                    }catch (Exception e){ }
                    stopService(new Intent(getApplicationContext(), Floating.class));
                }
            };
            timer2.schedule(timerTask2,10000);
        }
        if(!sharedPreferences.getBoolean("silent",false)&&am.getRingerMode()==AudioManager.RINGER_MODE_NORMAL) {
            r.play();
        }else if(am.getRingerMode()==AudioManager.RINGER_MODE_SILENT){

        }else {
            TimerTask timerTask= new TimerTask() {
                @Override
                public void run() {
                    if (Build.VERSION.SDK_INT >= 26) {
                        vibrator.vibrate(VibrationEffect.createOneShot(1000, VibrationEffect.DEFAULT_AMPLITUDE));
                    } else {
                        vibrator.vibrate(1000);
                    }
                }
            };
            timer.scheduleAtFixedRate(timerTask,0,1800);
            Timer timer1 = new Timer();
            TimerTask timerTask1 = new TimerTask() {
                @Override
                public void run() {
                    try{
                        r.stop();
                    }catch (Exception e){ }
                    try{
                        timer.cancel();
                    }catch (Exception e){ }
                }
            };timer1.schedule(timerTask1,45000);
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private void startMyOwnForeground()
    {
        String NOTIFICATION_CHANNEL_ID = "com.legendsayantan.recall.call";
        String channelName = "Call Requested";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID);
        Notification notification = notificationBuilder.setOngoing(true)
                .setContentTitle(title+" requested a call from you")
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



    // It is called when stopService()
    // method is called in MainActivity
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopSelf();
        // Window is removed from the screen
        windowManager.removeView(floatView);
    }
}

