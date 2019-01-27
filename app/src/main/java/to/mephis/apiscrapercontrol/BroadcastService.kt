package to.mephis.apiscrapercontrol

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.CountDownTimer
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.util.Log

class BroadcastService : Service() {
    internal var bi = Intent(COUNTDOWN_BR)
    internal var cdt: CountDownTimer? = null

    var max: Int? = null
    lateinit var n_title: String
    lateinit var n_text: String
    lateinit var n_summary: String

    override fun onCreate() {
        super.onCreate()
        //startForeground();
        Log.i(TAG, "timer service onCreate")

    }

    override fun onDestroy() {
        cdt!!.cancel()
        Log.i(TAG, "Timer cancelled")
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {

        max = intent.extras!!.getInt("max")
        n_title = intent.extras!!.get("n_title") as String
        n_text = intent.extras!!.get("n_text") as String
        n_summary = intent.extras!!.get("n_summary") as String

        val mContext = this

        val mBuilder = NotificationCompat.Builder(this.applicationContext, "notify_bttimeout")

        val ii = Intent(mContext.applicationContext, ScraperActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(mContext, 0, ii, 0)

        val bigText = NotificationCompat.BigTextStyle()
        bigText.bigText(n_text)
        bigText.setBigContentTitle(n_title)
        bigText.setSummaryText(n_summary)

        mBuilder.setContentIntent(pendingIntent)
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round)
        mBuilder.setContentTitle(n_text)
        mBuilder.setContentText(n_title)
        //        mBuilder.setProgress(max*1000,0,false);
        mBuilder.priority = Notification.PRIORITY_MAX
        mBuilder.setTimeoutAfter((max!! * 1000).toLong())
        mBuilder.setStyle(bigText)

        val mNotificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "smartscrape"
            val channel = NotificationChannel(channelId,
                    "Smartscrape Remote Control",
                    NotificationManager.IMPORTANCE_DEFAULT)
            mNotificationManager.createNotificationChannel(channel)
            mBuilder.setChannelId(channelId)
        }

        startForeground(32, mBuilder.notification)

        Log.i(TAG, "timer service onStartCommand: " + max!!)

        cdt = object : CountDownTimer((max!! * 1000).toLong(), 1000) {
            override fun onTick(millisUntilFinished: Long) {
                Log.i(TAG, "Countdown Seconds remaining: " + millisUntilFinished / 1000)
                bi.putExtra("countdown", millisUntilFinished)
                bi.putExtra("finnished", false)
                sendBroadcast(bi)
            }

            override fun onFinish() {
                Log.i(TAG, "Countdown finnished")
                bi.putExtra("finnished", true)
                ScraperActivity.setScraper(true)
                sendBroadcast(bi)
            }
        }
        cdt!!.start()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(arg0: Intent): IBinder? {
        return null
    }

    companion object {
        private val TAG = "BroadcastService"
        val COUNTDOWN_BR = "to.mephis.apiscrapercontrol.countdown_br"
    }

}
