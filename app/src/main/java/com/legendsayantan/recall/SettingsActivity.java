package com.legendsayantan.recall;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorSpace;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceActivity;
import android.preference.RingtonePreference;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.text.TextUtils;
import android.util.ArraySet;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceDataStore;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.stephentuso.welcome.WelcomeHelper;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class SettingsActivity extends AppCompatActivity {
static SharedPreferences sharedPreferences;
static SharedPreferences.Editor editor;
static List<ContactModel> allContacts = new ArrayList<>();
static Activity activity;
    private static PrintWriter out;
    private static BufferedReader in;
static boolean overlay;
static String input = "";
    private static DataOutputStream dos;
    WelcomeHelper welcomeScreen;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        sharedPreferences = getSharedPreferences("preferences", Activity.MODE_PRIVATE);
        editor=sharedPreferences.edit();
        PreferenceManager.setDefaultValues(this, R.xml.root_preferences, false);
        reqPermissions(1,this);
        if(sharedPreferences.getBoolean("service",false)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(new Intent(getApplicationContext(),BroadcastStarter.class));
            }else startService(new Intent(getApplicationContext(),BroadcastStarter.class));

        }
        activity=this;
        welcomeScreen = new WelcomeHelper(this, MyWelcomeScreen.class);
        welcomeScreen.show(savedInstanceState);
        connect();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NonNull Thread t, @NonNull Throwable e) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    ClipboardManager c = getSystemService(ClipboardManager.class);
                    c.setPrimaryClip(ClipData.newPlainText("Error",t.toString()));
                }
            }
        });
    }
    public static void getOverlayPerm(Context context) {
        checkOverlay(context);
        if (!overlay) {
            context.startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION));
            Toast.makeText(context.getApplicationContext(), "Find ReCall and enable overlays", Toast.LENGTH_LONG).show();
        }
        checkOverlay(context);
    }

    public static void checkOverlay(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            overlay = Settings.canDrawOverlays(context.getApplicationContext());
        } else overlay = true;
    }


    public static void reqPermissions(int requestcode, Activity activity) {
        switch (requestcode) {
            case 1:
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.READ_CONTACTS},
                        requestcode);
                break;
            case 2:
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.CALL_PHONE},
                        requestcode);
                break;
            case 3:
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.FOREGROUND_SERVICE},
                        requestcode);
            case 4:
                ActivityCompat.requestPermissions(activity,
                        new String[]{Manifest.permission.VIBRATE},
                        requestcode);
            case 5:
                getOverlayPerm(activity);
            case 6:
                getBackService(activity.getApplicationContext());
            default:
                System.out.println("Permission matter complete");
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            System.out.println(permissions.toString()+" granted");
            // permission was granted, yay! Do the
            // contacts-related task you need to do.
        } else {
            System.out.println(permissions.toString()+" denied");
            // permission denied, boo! Disable the
            // functionality that depends on this permission.
        }
        reqPermissions(requestCode+1,this);
        return;
    }

    @SuppressLint("Range")
    public static List<ContactModel> getContacts(Context ctx) {
        List<ContactModel> list = new ArrayList<>();
        ContentResolver contentResolver = ctx.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                if (cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor cursorInfo = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                    InputStream inputStream = ContactsContract.Contacts.openContactPhotoInputStream(ctx.getContentResolver(),
                            ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(id)));

                    Uri person = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, new Long(id));
                    Uri pURI = Uri.withAppendedPath(person, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);

                    while (cursorInfo.moveToNext()) {
                        ContactModel info = new ContactModel();
                        info.id = id;
                        info.name = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                        info.mobileNumber = cursorInfo.getString(cursorInfo.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        if(!info.mobileNumber.startsWith("+"))info.mobileNumber= "+"+getCountryDialCode(ctx.getApplicationContext())+info.mobileNumber;
                        info.photoURI= pURI;
                        list.add(info);
                    }
                    cursorInfo.close();
                }
            }
            cursor.close();
        }
        return list;
    }
    public static String getCountryDialCode(Context context){
        String contryId;
        String contryDialCode = null;
        TelephonyManager telephonyMngr = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        contryId = telephonyMngr.getSimCountryIso().toUpperCase();
        String[] arrContryCode= context.getResources().getStringArray(R.array.DialingCountryCode);
        for(int i=0; i<arrContryCode.length; i++){
            String[] arrDial = arrContryCode[i].split(",");
            if(arrDial[1].trim().equals(contryId.trim())){
                contryDialCode = arrDial[0];
                break;
            }
        }
        return contryDialCode;
    }




    public static void getBackService(Context context) {
        if(checkBackService(context))return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent();
            String packageName = context.getPackageName();
            PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                intent.setAction(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }

    public static boolean checkBackService(Context context) {
        PowerManager pm = (PowerManager) context.getSystemService(POWER_SERVICE);
        String packageName = context.getPackageName();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return pm.isIgnoringBatteryOptimizations(packageName);
        }
        return true;
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SwitchPreference serviceup = findPreference("service");;
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            EditTextPreference wakeword = (EditTextPreference) findPreference("wakeword");;
            ListPreference reply = findPreference("reply");
            MultiSelectListPreference specified = findPreference("specified");
            SwitchPreference service = findPreference("service");
            SwitchPreference silent = findPreference("silent");
            SwitchPreference autoaccept = findPreference("autoaccept");

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!(activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED))
                    if (allContacts.size() == 0) {
                        allContacts.clear();
                        allContacts = getContacts(getContext());
                    }
            }else if (allContacts.size() == 0) {
                allContacts.clear();
                allContacts = getContacts(getContext());
            }
            List<String> contact = new ArrayList<>();
            List<String> contactValues = new ArrayList<>();
            for(int i =0;i<allContacts.size();i++){
                String mobile= removeWhite(allContacts.get(i).mobileNumber);
                if(!contact.contains(allContacts.get(i).name+" "+mobile)) {
                    contact.add(allContacts.get(i).name + " " + mobile);
                    contactValues.add("(" + allContacts.get(i).name + ") (" + mobile + ") ");
                }
            }
            CharSequence[] charSequences,charSequences2;
            charSequences = contact.toArray(new CharSequence[contact.size()]);
            charSequences2 = contactValues.toArray(new CharSequence[contactValues.size()]);
            specified.setEntries(charSequences);
            specified.setEntryValues(charSequences2);
            wakeword.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    editor.putString("wakeword",newValue.toString().trim());
                    editor.apply();
                    System.out.println(sharedPreferences.getString("wakeword","Recallme"));
                    reqPermissions(1,getActivity());
                    refreshService();
                    return true;
                }
            });
            reply.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if(newValue.toString().equals("all"))editor.putInt("reply",1);
                    if(newValue.toString().equals("contacts"))editor.putInt("reply",2);
                    if(newValue.toString().equals("specified"))editor.putInt("reply",3);
                    editor.apply();
                    reqPermissions(1,getActivity());
                    System.out.println(sharedPreferences.getInt("reply",0));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (!(activity.checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_DENIED))
                            if (allContacts.size() == 0) {
                                allContacts.clear();
                                allContacts = getContacts(getContext());
                            }
                    }else if (allContacts.size() == 0) {
                        allContacts.clear();
                        allContacts = getContacts(getContext());
                    }
                    List<String> contact = new ArrayList<>();
                    List<String> contactValues = new ArrayList<>();
                    for(int i =0;i<allContacts.size();i++){
                        String mobile= removeWhite(allContacts.get(i).mobileNumber);
                        if(!contact.contains(allContacts.get(i).name+" "+mobile)) {
                            contact.add(allContacts.get(i).name + " " + mobile);
                            contactValues.add("(" + allContacts.get(i).name + ") (" + mobile + ") ");
                        }
                    }
                    CharSequence[] charSequences,charSequences2;
                    charSequences = contact.toArray(new CharSequence[contact.size()]);
                    charSequences2 = contactValues.toArray(new CharSequence[contactValues.size()]);
                    specified.setEntries(charSequences);
                    specified.setEntryValues(charSequences2);
                    refreshService();
                    return true;
                }
            });
            specified.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    editor.putString("specifiedcontact",newValue.toString());
                    editor.apply();
                    System.out.println(sharedPreferences.getString("specified",""));
                    reqPermissions(1,getActivity());
                    refreshService();
                    return true;
                }
            });
            service.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    if(checkNotificationEnabled(getContext())) {
                        if(sharedPreferences.getInt("reply",0)==0){
                            Toast.makeText(getContext(),"Please select accept call request from whom",Toast.LENGTH_SHORT).show();
                            return false;
                        }
                        editor.putBoolean("service", !service.isChecked());
                        editor.apply();
                        System.out.println(sharedPreferences.getBoolean("service", false));
                        reqPermissions(1,getActivity());
                        if(sharedPreferences.getBoolean("service",false)) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                getContext().startForegroundService(new Intent(getContext(),BroadcastStarter.class));
                            }else getContext().startService(new Intent(getContext(),BroadcastStarter.class));
                        }else getContext().stopService(new Intent(getContext(),BroadcastStarter.class));
                        return true;
                    }else {
                        if(Build.VERSION.SDK_INT>Build.VERSION_CODES.LOLLIPOP_MR1){
                            startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                        }else {
                            startActivity(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"));
                        }
                        reqPermissions(1,getActivity());
                        return false;
                    }

                }
            });
            silent.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    editor.putBoolean("silent",!silent.isChecked());
                    editor.apply();
                    System.out.println(sharedPreferences.getBoolean("silent",false));
                    reqPermissions(1,getActivity());
                    refreshService();
                    return true;
                }
            });
            autoaccept.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    editor.putBoolean("autoaccept",!autoaccept.isChecked());
                    editor.apply();
                    System.out.println(sharedPreferences.getBoolean("autoaccept",false));
                    reqPermissions(1,getActivity());
                    refreshService();
                    return true;
                }
            });

        }
        public void refreshService(){
            if(sharedPreferences.getBoolean("service",false)){
                try{
                    getActivity().stopService(new Intent(getContext(),BroadcastStarter.class));
                    serviceup.setChecked(false);
                }catch (Exception e){ }
                try{
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        getActivity().startForegroundService(new Intent(getContext(),BroadcastStarter.class));
                        serviceup.setChecked(true);
                    }else {
                        getActivity().startService(new Intent(getContext(),BroadcastStarter.class));
                        serviceup.setChecked(true);
                    }
                }catch (Exception e){try{
                    serviceup.setChecked(false);}catch (Exception e1){}
                }
            }
        }
        public static String removeWhite(String s) {
            // Creating a pattern for whitespaces
            Pattern patt = Pattern.compile("[\\s]");
            // Searching patt in s.
            Matcher mat = patt.matcher(s);
            // Replacing
            return mat.replaceAll("");
        }
        public static boolean checkNotificationEnabled(Context context) {
            try{
                if(Settings.Secure.getString(context.getContentResolver(),
                        "enabled_notification_listeners").contains(context.getPackageName()))
                {
                    return true;
                } else {
                    return false;
                }
            }catch(Exception e) {
                System.out.println("Crash at checkNotificationEnabled, SettingsActivity");
                e.printStackTrace();
                return false;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
    public void social(View view) {
        Intent extLink = new Intent(Intent.ACTION_VIEW);
        switch(view.getId()){
            case R.id.fb:
                extLink.setData(Uri.parse("https://www.facebook.com/LegendSayantan"));
                break;
            case R.id.ig:
                extLink.setData(Uri.parse("https://www.instagram.com/LegendSayantan"));
                break;
            case R.id.git:
                extLink.setData(Uri.parse("https://github.com/legendsayantan"));
                break;
            case R.id.reddit:
                extLink.setData(Uri.parse("https://www.reddit.com/user/LegendSayantan"));
                break;
        }
        startActivity(extLink);
    }
    public static void startConnection(String ip, int port) throws IOException {
        Socket clientSocket = new Socket(ip, port);
        dos = new DataOutputStream(clientSocket.getOutputStream());
        in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
    }
    public static void sendMessage(String msg) throws IOException {
        String[] resp = new String[1];
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(dos!=null){
                        dos.writeUTF(msg);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    public static void connect(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startConnection("192.168.13.1", 65432);
                } catch (IOException e) {
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println(e.getCause());
                            System.out.println(e.getMessage());
                        }
                    });
                }
            }
        });
        thread.start();
    }
}