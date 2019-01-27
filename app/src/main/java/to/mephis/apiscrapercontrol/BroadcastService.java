package to.mephis.apiscrapercontrol;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class BroadcastService extends Service {
    private final static String TAG = "BroadcastService";
    public static final String COUNTDOWN_BR = "to.mephis.apiscrapercontrol.countdown_br";
    Intent bi = new Intent(COUNTDOWN_BR);
    CountDownTimer cdt = null;

    public Integer max;
    public String n_title;
    public String n_text;
    public String n_summary;

    @Override
    public void onCreate(){
        super.onCreate();
        //startForeground();
        Log.i(TAG,"timer service onCreate");

    }

    @Override
    public void onDestroy() {
        cdt.cancel();
        Log.i(TAG,"Timer cancelled");
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        max=(Integer) intent.getExtras().getInt("max");
        n_title=(String) intent.getExtras().get("n_title");
        n_text=(String) intent.getExtras().get("n_text");
        n_summary=(String) intent.getExtras().get("n_summary");

        Context mContext = this;

        final NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this.getApplicationContext(), "notify_bttimeout");

        Intent ii = new Intent(mContext.getApplicationContext(), ScraperActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, ii, 0);

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText(n_text);
        bigText.setBigContentTitle(n_title);
        bigText.setSummaryText(n_summary);

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle(n_text);
        mBuilder.setContentText(n_title);
//        mBuilder.setProgress(max*1000,0,false);
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mBuilder.setTimeoutAfter(max * 1000);
        mBuilder.setStyle(bigText);

        NotificationManager mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "smartscrape";
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Smartscrape Remote Control",
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }

        startForeground(32, mBuilder.getNotification());

        Log.i(TAG,"timer service onStartCommand: " + max);

        cdt = new CountDownTimer(max*1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                Log.i(TAG,"Countdown Seconds remaining: " + millisUntilFinished/1000);
                bi.putExtra("countdown",millisUntilFinished);
                bi.putExtra("finnished",false);
                sendBroadcast(bi);
            }

            @Override
            public void onFinish() {
                Log.i(TAG,"Countdown finnished");
                bi.putExtra("finnished",true);
                ScraperActivity.setScraper(true);
                sendBroadcast(bi);
            }
        };
        cdt.start();
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

}
