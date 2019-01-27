package to.mephis.apiscrapercontrol

import android.app.*
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.support.v4.app.NotificationCompat
import android.support.v7.app.AppCompatActivity


import android.os.Bundle
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.WindowManager
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ProgressBar
import android.widget.Switch

import android.content.SharedPreferences
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast

import com.android.volley.AuthFailureError
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

import java.text.SimpleDateFormat
import java.util.Arrays
import java.util.Date
import java.util.HashMap

import android.graphics.Color.GRAY
import android.graphics.Color.GREEN
import android.support.v7.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM

/**
 * Mainscreen for setting apiScraper Properties
 */
class ScraperActivity : AppCompatActivity() {

    // UI references.
    private var mBtnScraperState: Button? = null
    private var mLoginFormView: View? = null
    private var mEnablePolling: Switch? = null
    private var mEnableBTProxmity: Switch? = null
    private var mDisableBTProxmity: Switch? = null
    private var mpbBtTimeout: ProgressBar? = null
    private var mNotificationManager: NotificationManager? = null
    private val toolbar: Toolbar? = null

    internal var interval = 1000 // 1 second

    private val br = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            updateGUI(intent)
        }
    }

    val btName: String?
        get() {
            val sp = getSharedPreferences(pref_btMac, 0)
            return sp.getString("myStore", "00:00:00:00:00:00")
        }

    val polling: Boolean
        get() {
            val sp = getSharedPreferences(pref_Polling, 0)
            return sp.getBoolean("myStore", false)
        }

    val startOnProximity: Boolean
        get() {
            val sp = getSharedPreferences(pref_StartOnProximity, 0)
            return sp.getBoolean("myStore", false)
        }

    val stopOnProximityLost: Boolean
        get() {
            val sp = getSharedPreferences(pref_StopOnProximityLost, 0)
            return sp.getBoolean("myStore", false)
        }

    private val mAclConnectReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)

            if (BluetoothDevice.ACTION_ACL_CONNECTED == intent.action) {
                Log.i("btDevice", "ACL Connect Device: " + device.name + " " + device.address)
                //todo: proximity get trigger mandatory if disable checked?
                if (mDisableBTProxmity!!.isChecked) {
                    stopBtTimeout()
                }
                if ((device.name == btName) and mEnableBTProxmity!!.isChecked) {
                    val toast = Toast.makeText(applicationContext,
                            "Proximity Detected...",
                            Toast.LENGTH_SHORT)
                    toast.show()
                    notifySmartscrape("Proximity detected", "Starting Scrape.", "Bluetooth Proximity to your car ist detected. You configured to turn on scraping when this happens", 10)
                    setScraper(false)
                }
            }
            if (BluetoothDevice.ACTION_ACL_DISCONNECTED == intent.action || BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED == intent.action) {
                Log.i("btDevice", "ACL Disconnect Device: " + device.name + " " + device.address)
                if ((device.name == btName) and mDisableBTProxmity!!.isChecked) {
                    val toast = Toast.makeText(applicationContext,
                            "Proximity lost detected...",
                            Toast.LENGTH_SHORT)
                    toast.show()
                    startBtTimeout()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM)

        mResources = resources

        super.onCreate(savedInstanceState)
        instance = this
        setContentView(R.layout.activity_main)

        //Look at BT
        val filter = IntentFilter()
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED)
        registerReceiver(mAclConnectReceiver, filter)

        mBtnScraperState = findViewById<View>(R.id.btn_scraperStatus) as Button
        mEnablePolling = findViewById<View>(R.id.swEnablePolling) as Switch
        mEnableBTProxmity = findViewById<View>(R.id.swEnableBTProximity) as Switch
        mDisableBTProxmity = findViewById<View>(R.id.swEnableBTProximityLost) as Switch
        mpbBtTimeout = findViewById<View>(R.id.prgProximityTimeout) as ProgressBar
        mTblData = findViewById<View>(R.id.tblData) as TableLayout

        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN)

        mBtnScraperState!!.setOnClickListener { switchScraper() }
        mpbBtTimeout!!.setOnLongClickListener { view ->
            if ((view as ProgressBar).progress != 0) {
                ScraperActivity.instance!!.stopBtTimeout()
                setScraper(false)
            }
            true
        }

        mEnablePolling!!.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                val toast = Toast.makeText(applicationContext,
                        "Polling ScraperApiController enabled",
                        Toast.LENGTH_SHORT)
                toast.show()
                scheduleAlarm()
            } else {
                val toast = Toast.makeText(applicationContext,
                        "Polling ScraperApiController disabled",
                        Toast.LENGTH_SHORT)
                toast.show()
                cancelAlarm()
            }
            writePolling(isChecked)
        }

        mEnableBTProxmity!!.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                val toast = Toast.makeText(applicationContext,
                        "Activate on Proximity enabled",
                        Toast.LENGTH_SHORT)
                toast.show()
            } else {
                val toast = Toast.makeText(applicationContext,
                        "Activate on Proximity disabled",
                        Toast.LENGTH_SHORT)
                toast.show()
            }
            writeStartOnPromity(isChecked)
        }

        mDisableBTProxmity!!.setOnCheckedChangeListener { buttonView, isChecked ->
            if (isChecked) {
                val toast = Toast.makeText(applicationContext,
                        "Deactivate on Proximity lost enabled",
                        Toast.LENGTH_SHORT)
                toast.show()
            } else {
                val toast = Toast.makeText(applicationContext,
                        "Deactivate on Proximity lost disabled",
                        Toast.LENGTH_SHORT)
                toast.show()
                //stopBtTimeout();
                mpbBtTimeout!!.progress = 0
            }
            writeStopOnProximityLost(isChecked)
        }


        mLoginFormView = findViewById(R.id.main_scrollview)

        //Load perfs
        loadSettings()

        mEnablePolling!!.isChecked = polling
        if (mEnablePolling!!.isChecked) {
            doPoll()
        } else {
            mBtnScraperState!!.text = "Polling disabled.."
        }
        mEnableBTProxmity!!.isChecked = startOnProximity
        mDisableBTProxmity!!.isChecked = stopOnProximityLost
    }

    override fun onDestroy() {
        super.onDestroy()
        cancelAlarm()
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(br)
        Log.i(TAG, "Unregistered broacast receiver")
        cancelAlarm()
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(br, IntentFilter(BroadcastService.COUNTDOWN_BR))
        Log.i(TAG, "Registered broacast receiver")
        if (mpbBtTimeout!!.progress > 0) {
            doPoll()
        }
        scheduleAlarm()
    }

    private fun notifySmartscrape(title: String, Text: String, Summary: String, timeoutSecs: Int?) {

        val mContext = this

        val mBuilder = NotificationCompat.Builder(this.applicationContext, "notify_001")

        val ii = Intent(mContext.applicationContext, ScraperActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(mContext, 0, ii, 0)

        val bigText = NotificationCompat.BigTextStyle()
        bigText.bigText(Text)
        bigText.setBigContentTitle(title)
        bigText.setSummaryText(Summary)

        mBuilder.setContentIntent(pendingIntent)
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round)
        mBuilder.setContentTitle(title)
        mBuilder.setContentText(Text)
        mBuilder.priority = Notification.PRIORITY_MAX
        mBuilder.setTimeoutAfter((timeoutSecs!! * 1000).toLong())
        mBuilder.setStyle(bigText)

        mNotificationManager = mContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "smartscrape"
            val channel = NotificationChannel(channelId,
                    "Smartscrape Remote Control",
                    NotificationManager.IMPORTANCE_DEFAULT)
            mNotificationManager!!.createNotificationChannel(channel)
            mBuilder.setChannelId(channelId)
        }

        mNotificationManager!!.notify(0, mBuilder.build())
    }

    private fun setProgressBarValues() {
        mpbBtTimeout!!.max = btTimeout
        mpbBtTimeout!!.progress = btTimeout
    }

    fun startBtTimeout() {
        setProgressBarValues()
        val countDownService = Intent(this, BroadcastService::class.java)
        countDownService.putExtra("max", btTimeout)
        countDownService.putExtra("n_title", "Proximity lost")
        countDownService.putExtra("n_text", "Stopping scrape in ")
        countDownService.putExtra("n_summary", "Bluetooth Proximit to your car was lost. You configured to turn off scraping when this happens")
        startForegroundService(countDownService)
    }

    fun stopBtTimeout() {
        val countDownService = Intent(this, BroadcastService::class.java)
        stopService(countDownService)
        mpbBtTimeout!!.progress = 0
    }

    private fun updateGUI(intent: Intent) {
        if (intent.extras != null) {
            if (intent.extras!!.getBoolean("finnished")) {
                //setScraper(true);
                mpbBtTimeout!!.progress = 0
            } else {
                val millisUntilFinished = intent.getLongExtra("countdown", 0)
                Log.i(TAG, "Countdown seconds remaining: " + millisUntilFinished / 1000)
                val progress = millisUntilFinished.toInt() / interval
                mpbBtTimeout!!.progress = mpbBtTimeout!!.max - progress
            }
        }
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    fun loadSettings() {
        apiUrl = getapiUrl()
        apiKey = getapiKey()
        getBtTimeout()
    }

    fun launchSettings() {
        val intent = Intent(this, Settings::class.java)
        intent.putExtra("pref_btMac", pref_btMac)
        intent.putExtra("pref_btTimeout", pref_btTimeout)
        intent.putExtra("pref_apiUrl", pref_apiUrl)
        intent.putExtra("pref_apiKey", pref_apiKey)
        intent.putExtra("btname", btName)
        intent.putExtra("bttimeout", getBtTimeout())
        intent.putExtra("apiurl", getapiUrl())
        intent.putExtra("apikey", getapiKey())
        startActivityForResult(intent, 1)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1) {
            //if (resultCode == Activity.RESULT_OK) {
              if (resultCode == Activity.RESULT_OK) {
                val result = data!!.getStringExtra("result")
                Toast.makeText(applicationContext, result, Toast.LENGTH_SHORT).show()
                loadSettings()
            }
            /**if (resultCode == RESULT_CANCELED) {
             * String result=data.getStringExtra("result");
             * Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
             * } */
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_refresh -> {
                Log.i("Toolbar", "refresh")
                doPoll()
                return true
            }
            R.id.settings -> {
                Log.i("Toolbar", "settings")
                launchSettings()
                return true
            }
            R.id.about -> {
                Log.i("Toolbar", "about")

                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    fun scheduleAlarm() {
        Log.i("Alarm", "schedule")
        val intent = Intent(applicationContext, alarmReceiver::class.java)
        val pIntent = PendingIntent.getBroadcast(this, alarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val firstMillis = System.currentTimeMillis() // alarm is set right away
        val alarm = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.setRepeating(AlarmManager.RTC_WAKEUP, firstMillis, (15 * 1000).toLong(), pIntent)
        //        getInstance().startService(pIntent);
    }

    fun cancelAlarm() {
        Log.i("Alarm", "cancel")
        val intent = Intent(applicationContext, alarmReceiver::class.java)
        val pIntent = PendingIntent.getBroadcast(this, alarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT)
        val alarm = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(pIntent)
    }

    fun setScrapeState(state: Boolean) {
        if (!state) {
            mBtnScraperState!!.text = "Scraper is running, car $carAsleep"
            mBtnScraperState!!.setBackgroundColor(GREEN)

        } else {
            mBtnScraperState!!.text = "Scraper is inactive, press to activate.."
            mBtnScraperState!!.setBackgroundColor(GRAY)
        }
        disableScraping = state
    }

    fun getBtTimeout(): String {
        val sp = getSharedPreferences(pref_btTimeout, 0)
        val str = sp.getString("myStore", "240") // default 4 minutes
        btTimeout = Integer.parseInt(str!!)
        return str
    }

    fun getapiUrl(): String? {
        val sp = getSharedPreferences(pref_apiUrl, 0)
        val str = sp.getString("myStore", "https://yourapiurl/")
        apiUrl = str
        return str
    }

    fun getapiKey(): String? {
        val sp = getSharedPreferences(pref_apiKey, 0)
        val str = sp.getString("myStore", "YourApiKey")
        apiKey = str
        return str
    }

    fun writePolling(pref: Boolean) {
        val editor = getSharedPreferences(pref_Polling, 0).edit()
        editor.putBoolean("myStore", pref)
        editor.commit()
    }

    fun writeStartOnPromity(pref: Boolean) {
        val editor = getSharedPreferences(pref_StartOnProximity, 0).edit()
        editor.putBoolean("myStore", pref)
        editor.commit()
    }

    fun writeStopOnProximityLost(pref: Boolean) {
        val editor = getSharedPreferences(pref_StopOnProximityLost, 0).edit()
        editor.putBoolean("myStore", pref)
        editor.commit()
    }

    private fun switchScraper() {
        setScraper(!disableScraping)
    }

    companion object {

        /**
         * Shared pref store..
         */

        private val polling_enabled = false


        // Set Keys for Prefstore
        val pref_btMac = "btMac"
        val pref_btTimeout = "btTimeout"
        val pref_apiUrl = "apiUrl"
        val pref_apiKey = "apiKey"
        val pref_Polling = "polling"
        val pref_StartOnProximity = "startOnProximity"
        val pref_StopOnProximityLost = "stopOnProximityLost"

        //Globals
        var apiUrl: String? = ""
        var apiKey: String? = ""
        var disableScraping = false
        var btTimeout: Int = 0
        var carAsleep = "unknown"
        var lastPoll: Long? = null
        lateinit var mResources: Resources
        private var mTblData: TableLayout? = null
        private val TAG = "ScraperActivity"


        // We do this to publish it to our other Classes
        var instance: ScraperActivity? = null
            private set

        private fun populateDataTable(jsonData: JSONObject) {
            mTblData!!.removeAllViews()
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z")
            val hideItems = Arrays.asList("apikey")
            val asDate = Arrays.asList("disabledsince")
            val iterator = jsonData.keys()
            while (iterator.hasNext()) {
                val row = TableRow(instance)
                val key = iterator.next()
                try {
                    if (!hideItems.contains(key)) {
                        val value = jsonData.get(key.toString())
                        val col1 = TextView(instance)
                        val col2 = TextView(instance)
                        col1.text = key.toString()
                        if (asDate.contains(key)) {
                            val unixSeconds = java.lang.Long.parseLong(value.toString())
                            val date = Date(unixSeconds * 1000L)
                            val formattedDate = sdf.format(date)
                            col2.text = formattedDate
                        } else {
                            col2.text = value.toString()
                        }
                        col2.gravity = Gravity.RIGHT
                        row.addView(col1)
                        row.addView(col2)
                        mTblData!!.addView(row)
                    }
                } catch (ignored: JSONException) {
                    ignored.printStackTrace()
                }

            }
            val row1 = TableRow(instance)
            val col1 = TextView(instance)
            val col2 = TextView(instance)
            col1.text = "apiurl"
            col2.text = apiUrl
            col2.gravity = Gravity.RIGHT
            row1.addView(col1)
            row1.addView(col2)

            row1.setBackgroundColor(mResources.getColor(R.color.colorBackground))
            mTblData!!.addView(row1)
            val row2 = TableRow(instance)
            val ts1 = TextView(instance)
            val ts2 = TextView(instance)
            ts1.text = "lastpoll"
            val date = Date(lastPoll!!)
            val formattedDate = sdf.format(date)
            ts2.text = formattedDate
            ts2.gravity = Gravity.RIGHT
            row2.addView(ts1)
            row2.addView(ts2)
            row2.setBackgroundColor(mResources.getColor(R.color.colorBackground))
            mTblData!!.addView(row2)

        }

        fun doPoll() {
            try {
                val requestQueue: RequestQueue
                //init rest client
                requestQueue = Volley.newRequestQueue(instance!!)
                // Request a string response from the provided URL.
                val jsonArrayRequest = object : JsonArrayRequest(ScraperActivity.apiUrl!! + "state",
                        Response.Listener { response ->
                            try {
                                val state = response.getJSONObject(0)
                                disableScraping = state.getBoolean("disablescraping")
                                carAsleep = state.getString("state")
                                ScraperActivity.instance!!.setScrapeState(disableScraping)
                                populateDataTable(state)
                            } catch (e: JSONException) {
                                e.printStackTrace()
                            }
                        },
                        Response.ErrorListener { error ->
                            if (error.networkResponse != null) {
                                if (error.networkResponse.statusCode == 400) {
                                    val toast = Toast.makeText(instance,
                                            "Wrong API Key: Code 400: $error",
                                            Toast.LENGTH_SHORT)
                                    toast.show()
                                } else {
                                    Log.e("volley", "error$error")
                                    val toast = Toast.makeText(instance,
                                            "Connection Problem: $error",
                                            Toast.LENGTH_SHORT)
                                    toast.show()
                                }
                            } else {
                                Log.e("volley", "error$error")
                                val toast = Toast.makeText(instance,
                                        "Connection Problem: $error",
                                        Toast.LENGTH_SHORT)
                                toast.show()
                            }
                        }) {
                    @Throws(AuthFailureError::class)
                    override fun getHeaders(): Map<String, String> {
                        val headers = HashMap<String, String>()
                        //headers.put("Content-Type", "application/json");
                        //headers["apikey"] = ScraperActivity.apiKey
                        headers.put("test",ScraperActivity.apiKey.toString())
                        return headers
                    }
                }
                requestQueue.add(jsonArrayRequest)
                lastPoll = System.currentTimeMillis()
            } finally {
                //
            }
        }

        fun setScraper(doScrape: Boolean) {
            //Perform http post
            val requestQueue: RequestQueue
            requestQueue = Volley.newRequestQueue(instance!!)
            val json = JSONObject()
            try {
                json.put("command", "scrape")
                json.put("value", doScrape)
            } catch (e: JSONException) {
                e.printStackTrace()
            }

            val jsonObjectRequest = object : JsonObjectRequest(Request.Method.POST, apiUrl!! + "switch", json,
                    Response.Listener { response -> Log.i("POST Response", response.toString()) }, Response.ErrorListener { error -> Log.e("POST Error", "Post error: " + error.message) }) {
                @Throws(AuthFailureError::class)
                override fun getHeaders(): Map<String, String> {
                    val headers = HashMap<String, String>()
                    //headers.put("Content-Type", "application/json");
                    //headers["apikey"] = ScraperActivity.apiKey
                    headers.put("apikey",ScraperActivity.apiKey.toString())
                    return headers
                }
            }
            requestQueue.add(jsonObjectRequest)

            // Since the user initiated an action, we need to cancel all delayed actions as invalid
            ScraperActivity.instance!!.stopBtTimeout()

            //Give API 100ms to accomodate..
            Handler().postDelayed({ doPoll() }, 100)
            //        doPoll();
        }
    }
}

