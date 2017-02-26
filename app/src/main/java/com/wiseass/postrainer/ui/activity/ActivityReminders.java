package com.wiseass.postrainer.ui.activity;
//import vision.com.google.*;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.vision.barcode.Barcode;
import com.wiseass.postrainer.R;
import com.wiseass.postrainer.api.DeleteFromDatabase;
import com.wiseass.postrainer.api.ReadFromDatabase;
import com.wiseass.postrainer.api.ToggleReminderAlarmState;
import com.wiseass.postrainer.api.WriteToDatabase;
import com.wiseass.postrainer.model.database.AlarmDatabase;
import com.wiseass.postrainer.model.objects.Reminder;
import com.wiseass.postrainer.ui.fragment.FragmentDisclaimer;
import com.wiseass.postrainer.ui.fragment.FragmentManageReminder;
import com.wiseass.postrainer.ui.fragment.FragmentNavigationDrawer;
import com.wiseass.postrainer.ui.fragment.FragmentReminderList;
//import com.wiseass.postrainer.ui.activity.

import java.util.ArrayList;
import java.util.Calendar;

public class ActivityReminders extends AppCompatActivity
        implements FragmentDisclaimer.FragmentDisclaimerCallback,
        FragmentManageReminder.FragmentManageReminderCallback,
        FragmentReminderList.FragmentReminderListCallback {
    private static final String BUNDLE_ITEM = "BUNDLE_ITEM";
    private static final String VIBRATE_ONLY = "VIBRATE_ONLY";
    private static final String USER_ACCEPTED_DISCLAIMER = "USER_ACCEPTED_DISCLAIMER";
    private static final String FRAG_DISCLAIMER = "FRAG_DISCLAIMER";
    private static final String FRAG_REMINDER_LIST = "FRAG_REMINDER_LIST";
    private static final String FRAG_MANAGE_REMINDER = "FRAG_MANAGE_REMINDER";

    private ArrayList<Reminder> reminders;
    private AlarmManager alarmMgr;
    private FragmentManager manager;
    private Toolbar toolbar;
    private Reminder tempDeletedItem;
    private Animation scaleUp, scaleDown;


    public static final int REQUEST_CODE = 100;
    public static final int PERMISSION_REQUEST = 200;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminders);

        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        //Button button = (Button)findViewById(R.id.qrCodeScanner);
        Button scanbtn = (Button) findViewById(R.id.qrCodeScanner);
        //result = (TextView) findViewById(R.id.result);
        /*
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST);
        }
        */

        scanbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ActivityReminders.this, ScanActivity.class);
                startActivityForResult(intent, REQUEST_CODE);

            }
        });



        setSupportActionBar(toolbar);


        manager = getSupportFragmentManager();

        scaleUp = AnimationUtils.loadAnimation(this, R.anim.scale_up);
        scaleDown = AnimationUtils.loadAnimation(this, R.anim.scale_down);

        /* original code
        if (checkIfDisclaimerAccepted()) {
            setUpNavigationDrawer();
            loadCurrentAlarms();
        } else {
            Fragment DisclaimerFrag = FragmentDisclaimer.newInstance();
            FragmentTransaction transaction = manager.beginTransaction();
            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);

            transaction.replace(R.id.cont_reminder_fragments, DisclaimerFrag, FRAG_DISCLAIMER);
            transaction.commit();

            getSupportActionBar().setTitle(R.string.reminder_disclaimer_title);
        }
        */
        setUpNavigationDrawer();
        loadCurrentAlarms();

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK){
            if(data != null){
                final Barcode barcode = data.getParcelableExtra("barcode");

                String res = barcode.displayValue;
                setClipboard(this, res);
                makeSomeToast("Code copied to system clipboard");
                /*
                TextView result.post(new Runnable() {
                    @Override
                    public void run() {
                        result.setText(barcode.displayValue);
                    }
                });
                */

            }
        }
    }
    private void setClipboard(Context context, String text) {
        if(android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
            android.text.ClipboardManager clipboard = (android.text.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            clipboard.setText(text);
        } else {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Copied Text", text);
            clipboard.setPrimaryClip(clip);
        }
    }

    /**
     * Checks if user has seen and accepted disclaimer dialog. If yes, carry on. If no, show dialog
     *
     * @return result stored as SharedPref
     */
    /*
    private boolean checkIfDisclaimerAccepted() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean accepted = prefs.getBoolean(USER_ACCEPTED_DISCLAIMER, false);
        //SharedPreferences.Editor ed = prefs.edit();
        return accepted;
    }
    */

    private void setUpNavigationDrawer(){
        FragmentNavigationDrawer drawerFragment = (FragmentNavigationDrawer)
                getSupportFragmentManager().findFragmentById(R.id.fragment_navigation_drawer);

        drawerFragment.setUp(R.id.fragment_navigation_drawer, (DrawerLayout) findViewById(R.id.drawer_layout_reminders), toolbar);
    }

    /**
     * Loads all alarms from the Database. If at elast one alarm exists, check if any of said
     * alarms are active. Otherwise, load blank list/recyclerview
     */
    private void loadCurrentAlarms() {
        ReadFromDatabase reader = new ReadFromDatabase();
        reader.setQueryCompleteListener(new ReadFromDatabase.OnQueryComplete() {
            @Override
            public void setQueryComplete(ArrayList result) {
                if (result == null || result.size() == 0) {
                    reminders = new ArrayList<>();
                } else {
                    reminders = result;
                }

                Fragment listFrag = FragmentReminderList.newInstance(reminders);
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);
                toolbar.setVisibility(View.VISIBLE);
                toolbar.startAnimation(scaleUp);

                transaction.replace(R.id.cont_reminder_fragments, listFrag, FRAG_REMINDER_LIST);
                transaction.commit();

                getSupportActionBar().setTitle(R.string.reminder_list_title);

            }
        });
        reader.execute(AlarmDatabase.getInstance(getApplicationContext()));
    }
    public void triggerAlarm(Reminder reminder) {
        Log.d("AR", "Alarm Activated");
        if (alarmMgr == null) {
            alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        }
        Calendar alarm = Calendar.getInstance();

        //checkAlarm(alarm);

        Intent intent = new Intent(this, ActivityAlarmReceiver.class);
        intent.putExtra(VIBRATE_ONLY, reminder.isVibrateOnly());
        intent.putExtra(BUNDLE_ITEM, reminder);
        PendingIntent alarmIntent = PendingIntent.getActivity(this, reminder.getCreationDate(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (reminder.isRenewAutomatically()){
            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, alarm.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, alarmIntent);
        } else {
            alarmMgr.set(AlarmManager.RTC_WAKEUP, alarm.getTimeInMillis(),
                    alarmIntent);
        }

    }
    public void setAlarm(Reminder reminder) {
        /*
        Log.d("AR", "Alarm Activated");
        if (alarmMgr == null) {
            alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        }

        Calendar alarm = Calendar.getInstance();
        int chour = alarm.get(Calendar.HOUR_OF_DAY);
        //be careful here
        int cmin = alarm.get(Calendar.MINUTE);
        //be careful here

        chour += reminder.getHourOfDay();
        cmin += reminder.getMinute();
        if( cmin >= 60 ) {
            cmin -= 60;
            chour ++;
        }
        if( chour >= 24 )
            chour -= 24;

        alarm.setTimeInMillis(System.currentTimeMillis());
        alarm.set(Calendar.HOUR_OF_DAY, chour);
        alarm.set(Calendar.MINUTE, cmin);

        //checkAlarm(alarm);

        Intent intent = new Intent(this, ActivityAlarmReceiver.class);
        intent.putExtra(VIBRATE_ONLY, reminder.isVibrateOnly());
        intent.putExtra(BUNDLE_ITEM, reminder);
        PendingIntent alarmIntent = PendingIntent.getActivity(this, reminder.getCreationDate(), intent, PendingIntent.FLAG_UPDATE_CURRENT);
        if (reminder.isRenewAutomatically()){
            alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, alarm.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY, alarmIntent);
        } else {
            alarmMgr.set(AlarmManager.RTC_WAKEUP, alarm.getTimeInMillis(),
                    alarmIntent);
        }
        */
    }

    public void cancelAlarm(Reminder reminder) {
        Log.d("AR", "Alarm Cancelled");
        Intent intent = new Intent(this, ActivityAlarmReceiver.class);
        PendingIntent alarmIntent = PendingIntent.getActivity(this, reminder.getCreationDate(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (alarmMgr != null) {
            alarmMgr.cancel(alarmIntent);

        } else {
            alarmMgr = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
            alarmMgr.cancel(alarmIntent);
        }
    }

    //TODO handle situations where we don't want to restart entire FragRemList needlessly
    private void writeReminderToDatabase(Reminder item) {
        WriteToDatabase writer = new WriteToDatabase();
        writer.setData(item);

        writer.setWriteCompleteListener(new WriteToDatabase.OnWriteComplete() {
            @Override
            public void setWriteComplete(long result) {
                if (result != -1) {
                    makeSomeToast(getString(R.string.manage_alarm_write_successful));
                } else {
                    makeSomeToast(getString(R.string.manage_alarm_write_unsuccessful));
                }
                loadCurrentAlarms();
            }
        });
        writer.execute(AlarmDatabase.getInstance(getApplicationContext()));

    }

    /**
     * Props to Chris Noldus on stackoverflow for this solution
     * Checks whether the Calendar.getTimeInMillis() for the current alarm, is less than the
     * the current system time. If so, we need to add a day (86400000 millis) to the current alarm
     * so that it will fire at the appropriate time tomorrow. Otherwise, it would fire as soon as
     * it is set, which would be pretty stupid.
     * @param alarm Current alarm
     */
    private void checkAlarm(Calendar alarm){
        Calendar now = Calendar.getInstance();
        if (alarm.before(now)){
            long alarmForFollowingDay = alarm.getTimeInMillis() + 86400000L;
            alarm.setTimeInMillis(alarmForFollowingDay);
        }
    }

    private void makeSomeToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_activity_reminders, menu);
        return true;
    }

    @Override
    public void onBackPressed()
    {
        Fragment fragment = manager.findFragmentByTag(FRAG_MANAGE_REMINDER);
        if (fragment instanceof FragmentManageReminder){
            loadCurrentAlarms();
        } else {
            super.onBackPressed();
        }
    }

    /*------------------------Fragment Callbacks--------------------------------*/

    /**
     * Callback for FragmentManageReminder. Fires when user has finished updating/creating a
     * reminder object
     * @param reminder reminder to be written to the database
     */
    @Override
    public void onProceedPressed(Reminder reminder) {
        //TODO Find some way to check whether the reminder's name already exists. If so, prompt the user to pick another name
        writeReminderToDatabase(reminder);
    }

    /**
     * Callback for FragmentReminderList. Fires when user wishes to edit an existing Reminder
     * reminder object
     * @param position position in RecyclerView of clicked Reminder
     */
    @Override
    public void onReminderClicked(int position) {
        Fragment listFrag = FragmentManageReminder.newInstance(reminders.get(position));
        FragmentTransaction transaction = manager.beginTransaction();
        toolbar.startAnimation(scaleDown);
        toolbar.setVisibility(View.GONE);
        transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);


        transaction.replace(R.id.cont_reminder_fragments, listFrag, FRAG_MANAGE_REMINDER);
        transaction.commit();

        getSupportActionBar().setTitle(R.string.reminder_manage_title);
    }

    /**
     * Callback for FragmentReminderList. Fires when user wishes to toggle a Reminder object
     * @param position position in RecyclerView of clicked Reminder
     */
    @Override
    public void onReminderToggled(int position) {
        Reminder reminder = reminders.get(position);
        if (reminder.isActive()){
            cancelAlarm(reminder);
            reminder.setActive(false);
        }else {
            // checkForActiveAlarms(reminder);
            reminder.setActive(true);
            setAlarm(reminder);
        }

        ToggleReminderAlarmState toggle = new ToggleReminderAlarmState(reminder);
        toggle.execute(AlarmDatabase.getInstance(getApplicationContext()));
    }

    /**
     * Callback for FragmentReminderList. Fires when user wishes to create a new Reminder Object
     */
    //TODO enforce 5 reminder limit
    @Override
    public void onFabAddReminderClicked() {


        if (reminders.size() < 6){
            Fragment listFrag = FragmentManageReminder.newInstance();
            FragmentTransaction transaction = manager.beginTransaction();
            toolbar.startAnimation(scaleDown);
            toolbar.setVisibility(View.GONE);

            transaction.setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_right);

            transaction.replace(R.id.cont_reminder_fragments, listFrag, FRAG_MANAGE_REMINDER);
            transaction.commit();

            getSupportActionBar().setTitle(R.string.reminder_manage_title);
        } else {
            Toast.makeText(this, "Five Reminder Maximum", Toast.LENGTH_SHORT).show();
        }


    }

    /**
     * Callback for FragmentReminderList. Fires when user swipes out a Reminder object, therefore
     * wishing to delete said object.
     */
    //TODO figure out a way to confirm this action
    @Override
    public void onReminderSwiped(int position) {
        tempDeletedItem = reminders.get(position);
        reminders.remove(position);
        DeleteFromDatabase delete = new DeleteFromDatabase(tempDeletedItem);
        delete.setDeleteCompleteListener(new DeleteFromDatabase.OnDeleteComplete() {
            @Override
            public void setDeleteComplete() {
                showUndoSnackbar();
                cancelAlarm(tempDeletedItem);
            }
        });
        delete.execute(AlarmDatabase.getInstance(getApplicationContext()));
    }

    //TODO check if this is actually working
    private void showUndoSnackbar() {
        View v = (View)findViewById(R.id.cont_alarm_content);
        Snackbar.make(v, "Are you sure?", Snackbar.LENGTH_SHORT)
                .setAction("UNDO", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        writeReminderToDatabase(tempDeletedItem);
                    }
                });
    }

    /**
     * Pretty much only used when App is first run. Requires user to accept a disclaimer.
     */

    @Override
    public void onDisclaimerAccepted() {
        SharedPreferences sPrefs = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());

        SharedPreferences.Editor ed = sPrefs.edit();
        ed.putBoolean(USER_ACCEPTED_DISCLAIMER, true).apply();

        setUpNavigationDrawer();
        loadCurrentAlarms();
    }

}