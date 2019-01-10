package com.garrettgray.android.ggphoto;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class PollService extends IntentService {
    //private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    private static final long POLL_INTERVAL_MS = TimeUnit.MINUTES.toMillis(1);
    public static final String ACTION_SHOW_NOTIF = "ACTION_SHOW_NOTIF";

    public static final String TAG = "PollService";

    public static Intent newIntent(Context context){
       return new Intent(context, PollService.class);
    }

    public PollService(){
        super(TAG);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (!isNetworkAvailableAndConnected()){
            return;
        } else {
            String query = QueryPreferences.getStoredQuery(this);
            String lastResultId = QueryPreferences.getLastResultId(this);

            List<GalleryItem> items;

            if (query == null){
                items = new FlickrFetcher().fetchRecentPhotos();
            } else {
                items = new FlickrFetcher().searchPhotos(query);
            }

            if (items.size() == 0){
                return;
            }

            String resultId = items.get(0).getId();

            if (resultId.equals(lastResultId)){
                Log.i(TAG, "---GOT AN OLD RESULT!" + lastResultId);
            } else {
                Log.i(TAG, "---GOT A NEW RESULT!" + lastResultId);

                Resources resources = getResources();
                Intent i = PhotoGalleryActivity.newIntent(this);
                PendingIntent pi = PendingIntent.getService(this,0,i,0);
                Notification notification =
                        new NotificationCompat.Builder(this,"OPEN_PHOTOGAL_ACTIVITY")
                        .setTicker(resources.getString(R.string.new_pictures))
                                .setSmallIcon(android.R.drawable.ic_menu_report_image)
                                .setContentTitle(resources.getString(R.string.new_pictures))
                                .setContentText(getResources().getString(R.string.new_text))
                                .setContentIntent(pi)
                                .setAutoCancel(true)
                                .build();

                NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
                notificationManager.notify(0, notification);

                sendBroadcast(new Intent(ACTION_SHOW_NOTIF));
            }

                QueryPreferences.setLastResultId(this,lastResultId);
           }

        Log.i(TAG,"Received an intent: " + intent);
    }

    private boolean isNetworkAvailableAndConnected(){
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        boolean isNetworkAvailable = cm.getActiveNetworkInfo() != null;
        boolean isNetworkConnected = isNetworkAvailable && cm.getActiveNetworkInfo().isConnected();

        return isNetworkConnected;
    }

    public static void setServiceAlarm(Context context, boolean isOn){
        Intent i = PollService.newIntent(context);
        //Call send() on pending intents to resend them the exact same way at a later time
            PendingIntent pi = PendingIntent.getService(context,0,i,0);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        //FOR FF USE alarmManager.setWindow() or alarmManager.setExact()
        //These allow you to set exact alarms that occur only once!
            if (isOn) {
                //USE ELAPSED REAL TIME WAKE-Up WHICH will wake up a sleeping device for the alarm.
                alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME,
                        SystemClock.elapsedRealtime(),POLL_INTERVAL_MS,pi);
            } else {
                alarmManager.cancel(pi);
                pi.cancel();
            }

        QueryPreferences.setAlarmOn(context,isOn);

    }

    public static boolean isServiceAlarmOn(Context context){
        Intent i = PollService.newIntent(context);
        PendingIntent pi = PendingIntent.getService(context, 0, i,PendingIntent.FLAG_NO_CREATE);

        return pi != null;
    }
}
