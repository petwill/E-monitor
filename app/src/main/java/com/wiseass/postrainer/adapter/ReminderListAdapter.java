package com.wiseass.postrainer.adapter;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.StringTokenizer;
import android.content.Context;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.CountDownTimer;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import android.support.v7.widget.SwitchCompat;
import android.widget.TextView;

import com.wiseass.postrainer.R;
import com.wiseass.postrainer.model.objects.Reminder;

import com.wiseass.postrainer.ui.activity.ActivityReminders;
import com.wiseass.postrainer.util.AppStatus;
import com.wiseass.postrainer.util.HttpUrlConnection;
import com.wiseass.postrainer.util.TimeConverter;

import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import android.os.Handler;
//import static java.security.AccessController.getContext;

/**
 * Adapter class for Globally used Navigation Drawer.
 * Created by Ryan on 18/09/2015.
 */
public class ReminderListAdapter extends RecyclerView.Adapter<ReminderListAdapter.ViewHolder> {
    private LayoutInflater inflater;
    private List<Reminder> data = Collections.emptyList();
    private OnItemClickListener itemClickListener;
    private Context mContext;

    private boolean toggle;
    /**
     * @param context - Context of Activity which contains contains the fragment which manages
     *                this class
     * @param data    - List of NavListItems which contain a title and icon resource id, to be
     *                passed to bound to ViewHolder objects appropriately.
     */
    public ReminderListAdapter(Context context, List<Reminder> data) {
        mContext = context;
        inflater = LayoutInflater.from(context);
        this.data = data;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.item_alarm_widget, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ReminderListAdapter.ViewHolder holder, final int position) {
        Reminder item = data.get(position);
        holder.reminder = item;
        String titleArray[] =  (item.getAlarmTitle()).split("/");

        String title = titleArray[3];
        holder.channel = titleArray[2];
        holder.deviceID = titleArray[0];
        holder.deviceKey = titleArray[1];
        //holder.alarmTitle.setText(item.getAlarmTitle());
        holder.alarmTitle.setText(title);
        holder.defaultHour = item.getHourOfDay();
        holder.defaultMin =  item.getMinute();
        holder.defaultSec = 0;

        holder.hour = 0;
        holder.min = 0;
        holder.sec = 0;

         holder.alarmIcon.setImageResource(R.mipmap.ic_state_black);

        holder.state = -1;
        holder.alarmTime.setText(

                "Undetermined"
                //String.format("%02d : %02d : %02d",holder.hour, holder.min, holder.sec)
                //TimeConverter.convertTime(item.getHourOfDay(),item.getMinute())
        );

        if (item.isActive()){
            holder.alarmStateLabel.setText(R.string.on);
        } else {
            holder.alarmStateLabel.setText(R.string.off);
        }
        holder.alarmStateSwitch.setChecked(item.isActive());


    }


    @Override
    public int getItemCount() {
        return data.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView alarmTitle;
        TextView alarmTime;
        TextView alarmStateLabel;
        ImageView alarmIcon;
         SwitchCompat alarmStateSwitch;
        View alarmActiveContainer;
        Reminder reminder;

        int defaultHour, defaultMin, defaultSec;
        int hour, min, sec;
        int state;
        private double highBound = 0.6;
        private double lowBound = 0.5;
        String deviceID, deviceKey, channel;


        private Handler handler, countdownHandler;
        private boolean toggle=true;
        public ViewHolder(View itemView) {
            super(itemView);

            alarmTitle = (TextView) itemView.findViewById(R.id.lbl_alarm_title);
            alarmTime = (TextView) itemView.findViewById(R.id.lbl_alarm_time);


            alarmStateLabel = (TextView) itemView.findViewById(R.id.lbl_alarm_activation);
            alarmStateSwitch = (SwitchCompat) itemView.findViewById(R.id.swi_alarm_activation);
            alarmIcon = (ImageView) itemView.findViewById(R.id.im_clock);
            //alarmIcon.setImageResource(R.mipmap.ic_state_white);
            alarmIcon.setImageResource(R.mipmap.ic_state_green);
            state = -1;

            alarmIcon.setOnClickListener(this);
            alarmActiveContainer = (View) itemView.findViewById(R.id.cont_alarm_activation);
            alarmStateSwitch.setOnClickListener(this);

            handler = new Handler();
            handler.postDelayed(statusChecker, 1000);
            countdownHandler = new Handler();
            countdownHandler.postDelayed(startCountdown, 1000);

        }

        Runnable statusChecker = new Runnable() {
            @Override
            public void run() {

                if (AppStatus.getInstance(mContext).isOnline()) {

                    //Toast t = Toast.makeText(mContext, "You are online!!!!", 8000);
                    //t.show();

                } else {

                    alarmTime.setText("You are not online!");
                    alarmIcon.setImageResource(R.mipmap.ic_state_black);
                    handler.postDelayed(statusChecker, 5000);
                    return;
                }

                String ret = new HttpUrlConnection(deviceID, deviceKey, channel, mContext).getData();
                double res = Double.parseDouble(ret);
                //alarmTime.setText(ret);'
                if( state == -1 ) {
                    if (res > lowBound) {
                        alarmIcon.setImageResource(R.mipmap.ic_state_red);
                        alarmTime.setText("Occupied");
                        state = 1;
                    } else {
                        if (!(hour == 0 && min == 0 && sec == 0)) {
                            alarmIcon.setImageResource(R.mipmap.ic_state_orange);
                            alarmTime.setText("Abnormal!!!");
                            state = 2;
                        } else {
                            if (state != 3 && reminder.isActive())
                                ((ActivityReminders) mContext).triggerAlarm(reminder);
                            alarmIcon.setImageResource(R.mipmap.ic_state_green);
                            alarmTime.setText("Available");
                            state = 3;
                        }
                    }
                }
                else if( state == 3 ) {
                    if (res > highBound) {
                        alarmIcon.setImageResource(R.mipmap.ic_state_red);
                        hour = defaultHour;
                        min = defaultMin;
                        sec = defaultSec;
                        alarmTime.setText("Occupied");
                        state = 1;
                    }
                }else {
                    if (res > lowBound) {
                        alarmIcon.setImageResource(R.mipmap.ic_state_red);
                        alarmTime.setText("Occupied");
                        state = 1;
                    } else {
                        if (!(hour == 0 && min == 0 && sec == 0)) {
                            alarmIcon.setImageResource(R.mipmap.ic_state_orange);
                            alarmTime.setText("Abnormal!!!");
                            state = 2;
                        } else {
                            if (state != 3 &&  reminder.isActive())
                                ((ActivityReminders) mContext).triggerAlarm(reminder);
                            alarmIcon.setImageResource(R.mipmap.ic_state_green);
                            alarmTime.setText("Available");
                            state = 3;
                        }
                    }
                }
                handler.postDelayed(statusChecker, 5000);
            };
        };
        Runnable startCountdown = new Runnable() {
            @Override
            public void run() {
                if( state == 2 ) {

                }
                else if( hour == 0 && min == 0 && sec == 0 ) {
                    //alarmTime.setText("Time's Up!!!");
                    return;
                }
                else if( min == 0 && sec == 0) {
                    hour--;
                    min += 59;
                }
                else if( sec == 0 ) {
                    min--;
                    sec+=59;
                }
                else
                    sec--;

                //alarmTitle.setText(String.format("%02d : %02d : %02d", hour, min, sec));
                countdownHandler.postDelayed(startCountdown, 1000);
            }
        };



        @Override
        public void onClick(View v) {
            int id = v.getId();

            if (id == R.id.swi_alarm_activation){
                itemClickListener.onAlarmSwitchClick(getAdapterPosition());
                if (alarmStateSwitch.isChecked()){
                    alarmStateLabel.setText(R.string.on);
                } else {
                    alarmStateLabel.setText(R.string.off);
                }
            } else if (id == R.id.im_clock){
                itemClickListener.onAlarmIconClick(getAdapterPosition());
            }

        }
    }


    public interface OnItemClickListener {
        void onAlarmIconClick(int position);
        void onAlarmSwitchClick(int position);
    }

    public void setOnClickListener(final OnItemClickListener itemClickListener) {
        this.itemClickListener = itemClickListener;
    }
}
