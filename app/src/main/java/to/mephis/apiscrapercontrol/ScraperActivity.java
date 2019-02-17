package to.mephis.apiscrapercontrol;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.os.Build;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;


import android.os.Bundle;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ProgressBar;
import android.widget.Switch;

import android.content.SharedPreferences;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static android.graphics.Color.GRAY;
import static android.graphics.Color.GREEN;
import static android.support.v7.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;

/**
 * Mainscreen for setting apiScraper Properties
 */
public class ScraperActivity extends AppCompatActivity  {

    /**
     * Shared pref store..
     */

    private static boolean polling_enabled = false;


    // Set Keys for Prefstore
    public static final String pref_btMac = "btMac";
    public static final String pref_btTimeout = "btTimeout";
    public static final String pref_apiUrl = "apiUrl";
    public static final String pref_apiKey = "apiKey";
    public static final String pref_Polling = "polling";
    public static final String pref_StartOnProximity = "startOnProximity";
    public static final String pref_StopOnProximityLost = "stopOnProximityLost";

    //Globals
    public static String apiUrl = "";
    public static String apiKey = "";
    public static boolean disableScraping = false;
    public static int btTimeout;
    public static String carAsleep = "unknown";
    public static Long lastPoll;
    public static Resources mResources;
    private static TableLayout mTblData;
    private static SwipeRefreshLayout swipeLayout;

    // UI references.
    private Button mBtnScraperState;
    private View mLoginFormView;
    private Switch mEnablePolling;
    private Switch mEnableBTProxmity;
    private Switch mDisableBTProxmity;
    private ProgressBar mpbBtTimeout;
    private NotificationManager mNotificationManager;
    private Toolbar toolbar;
    private final static String TAG = "ScraperActivity";

    int interval = 1000; // 1 second


    // We do this to publish it to our other Classes
    private static ScraperActivity instance;
    public static ScraperActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        AppCompatDelegate.setDefaultNightMode(MODE_NIGHT_FOLLOW_SYSTEM);

        mResources = getResources();

        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_main);

        //Look at BT
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED);
        registerReceiver(mAclConnectReceiver, filter);

        mBtnScraperState = (Button) findViewById(R.id.btn_scraperStatus);
        mEnablePolling = (Switch) findViewById(R.id.swEnablePolling);
        mEnableBTProxmity = (Switch) findViewById(R.id.swEnableBTProximity);
        mDisableBTProxmity = (Switch) findViewById(R.id.swEnableBTProximityLost);
        mpbBtTimeout = (ProgressBar) findViewById(R.id.prgProximityTimeout);
        mTblData = (TableLayout) findViewById(R.id.tblData);

        swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                Log.e("debug","refresh");
                doPoll();
            }
        });

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        mBtnScraperState.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                switchScraper();
            }
        });
        mpbBtTimeout.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                if (((ProgressBar)view).getProgress() != 0) {
                    ScraperActivity.getInstance().stopBtTimeout();
                    setScraper(false);
                }
                return true;
            }
        });

        mEnablePolling.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Polling ScraperApiController enabled",
                        Toast.LENGTH_SHORT);
                toast.show();
                scheduleAlarm();
            } else {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Polling ScraperApiController disabled",
                        Toast.LENGTH_SHORT);
                toast.show();
                cancelAlarm();
            }
            writePolling(isChecked);
            }
        });

        mEnableBTProxmity.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if (isChecked) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Activate on Proximity enabled",
                        Toast.LENGTH_SHORT);
                toast.show();
            } else {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Activate on Proximity disabled",
                        Toast.LENGTH_SHORT);
                toast.show();
            }
            writeStartOnPromity(isChecked);
            }
        });

        mDisableBTProxmity.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Deactivate on Proximity lost enabled",
                            Toast.LENGTH_SHORT);
                    toast.show();
                } else {
                    Toast toast = Toast.makeText(getApplicationContext(),
                            "Deactivate on Proximity lost disabled",
                            Toast.LENGTH_SHORT);
                    toast.show();
                    //stopBtTimeout();
                    mpbBtTimeout.setProgress(0);
                }
                writeStopOnProximityLost(isChecked);
            }
        });


        mLoginFormView = findViewById(R.id.main_scrollview);

        //Load perfs
        loadSettings();

        mEnablePolling.setChecked(getPolling());
        if (mEnablePolling.isChecked()) {
            doPoll();
        } else {
            mBtnScraperState.setText("Automated Polling disabled, swipe to refresh");
        }
        mEnableBTProxmity.setChecked(getStartOnProximity());
        mDisableBTProxmity.setChecked(getStopOnProximityLost());
    }

    protected void onDestroy () {
        super.onDestroy();
        cancelAlarm();
    }

    protected void onPause() {
        super.onPause();
        unregisterReceiver(br);
        Log.i(TAG, "Unregistered broacast receiver");
        cancelAlarm();
    }

    protected void onResume() {
        super.onResume();
        registerReceiver(br, new IntentFilter(BroadcastService.COUNTDOWN_BR));
        Log.i(TAG, "Registered broacast receiver");
        if (mpbBtTimeout.getProgress()>0) {
            doPoll();
        }
        if (mEnablePolling.isChecked()) {
            scheduleAlarm();
        }
    }

    private void notifySmartscrape(String title, String Text, String Summary, Integer timeoutSecs) {

        Context mContext = this;

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this.getApplicationContext(), "notify_001");

        Intent ii = new Intent(mContext.getApplicationContext(), ScraperActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, ii, 0);

        NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
        bigText.bigText(Text);
        bigText.setBigContentTitle(title);
        bigText.setSummaryText(Summary);

        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setSmallIcon(R.mipmap.ic_launcher_round);
        mBuilder.setContentTitle(title);
        mBuilder.setContentText(Text);
        mBuilder.setPriority(Notification.PRIORITY_MAX);
        mBuilder.setTimeoutAfter(timeoutSecs * 1000);
        mBuilder.setStyle(bigText);

        mNotificationManager =
                (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "smartscrape";
            NotificationChannel channel = new NotificationChannel(channelId,
                    "Smartscrape Remote Control",
                    NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(channel);
            mBuilder.setChannelId(channelId);
        }

        mNotificationManager.notify(0, mBuilder.build());
    }

    private void setProgressBarValues() {
        mpbBtTimeout.setMax(btTimeout);
        mpbBtTimeout.setProgress(btTimeout);
    }

    public void startBtTimeout() {
        setProgressBarValues();
        Intent countDownService = new Intent(this,BroadcastService.class);
        countDownService.putExtra("max",btTimeout);
        countDownService.putExtra("n_title","Proximity lost");
        countDownService.putExtra("n_text","Stopping scrape in ");
        countDownService.putExtra("n_summary","Bluetooth Proximit to your car was lost. You configured to turn off scraping when this happens");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            startService(countDownService);
        } else {
            startForegroundService(countDownService);
        }
    }

    public void stopBtTimeout() {
        Intent countDownService = new Intent(this,BroadcastService.class);
        stopService(countDownService);
        mpbBtTimeout.setProgress(0);
    }

    private BroadcastReceiver br = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateGUI(intent);
        }
    };

    private void updateGUI(Intent intent) {
        if (intent.getExtras() != null) {
            if (intent.getExtras().getBoolean("finnished")) {
                //setScraper(true);
                mpbBtTimeout.setProgress(0);
            } else {
                long millisUntilFinished = intent.getLongExtra("countdown", 0);
                Log.i(TAG, "Countdown seconds remaining: " +  millisUntilFinished / 1000);
                int progress = (int) millisUntilFinished / interval;
                mpbBtTimeout.setProgress(mpbBtTimeout.getMax() - progress);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    public void loadSettings() {
        apiUrl = getapiUrl();
        apiKey = getapiKey();
        getBtTimeout();
    }

    public void launchSettings(){
        Intent intent = new Intent(this, Settings.class);
        intent.putExtra("pref_btMac",pref_btMac);
        intent.putExtra("pref_btTimeout",pref_btTimeout);
        intent.putExtra("pref_apiUrl",pref_apiUrl);
        intent.putExtra("pref_apiKey",pref_apiKey);
        intent.putExtra("btname",getBtName());
        intent.putExtra("bttimeout",getBtTimeout());
        intent.putExtra("apiurl",getapiUrl());
        intent.putExtra("apikey",getapiKey());
        startActivityForResult(intent,1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1){
            if(resultCode == RESULT_OK){
                String result=data.getStringExtra("result");
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
                loadSettings();
            }
            /**if (resultCode == RESULT_CANCELED) {
                String result=data.getStringExtra("result");
                Toast.makeText(getApplicationContext(), result, Toast.LENGTH_SHORT).show();
            }*/
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                Log.i("Toolbar", "refresh");
                doPoll();
                return true;
            case R.id.settings:
                Log.i("Toolbar","settings");
                launchSettings();
                return true;
            case R.id.about:
                Log.i("Toolbar","about");

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    public void scheduleAlarm() {
        Log.i("Alarm", "schedule");
        Intent intent = new Intent(getApplicationContext(), alarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, alarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.setRepeating(AlarmManager.RTC_WAKEUP,firstMillis,(15*1000),pIntent);
//        getInstance().startService(pIntent);
    }

    public void cancelAlarm() {
        Log.i("Alarm", "cancel");
        Intent intent = new Intent(getApplicationContext(), alarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, alarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
    }

    public void setScrapeState(boolean state)
    {
        if (!state) {
            mBtnScraperState.setText("Scraper is running, car " + carAsleep);
            mBtnScraperState.setBackgroundColor(GREEN);

        } else {
            mBtnScraperState.setText("Scraper is inactive, press to activate..");
            mBtnScraperState.setBackgroundColor(GRAY);
        }
        disableScraping = state;
    }

    public String getBtName()
    {
        SharedPreferences sp = getSharedPreferences(pref_btMac,0);
        String str = sp.getString("myStore","00:00:00:00:00:00");
        return str;
    }

    public String getBtTimeout()
    {
        SharedPreferences sp = getSharedPreferences(pref_btTimeout,0);
        String str = sp.getString("myStore","240"); // default 4 minutes
        btTimeout = Integer.parseInt(str);
        return str;
    }

    public String getapiUrl()
    {
        SharedPreferences sp = getSharedPreferences(pref_apiUrl,0);
        String str = sp.getString("myStore","https://yourapiurl/");
        apiUrl = str;
        return str;
    }

    public String getapiKey()
    {
        SharedPreferences sp = getSharedPreferences(pref_apiKey,0);
        String str = sp.getString("myStore","YourApiKey");
        apiKey = str;
        return str;
    }

    public boolean getPolling()
    {
        SharedPreferences sp = getSharedPreferences(pref_Polling,0);
        boolean polling = sp.getBoolean("myStore",false);
        return polling;
    }
    public void writePolling(boolean pref)
    {
        SharedPreferences.Editor editor = getSharedPreferences(pref_Polling,0).edit();
        editor.putBoolean("myStore", pref);
        editor.commit();
    }

    public boolean getStartOnProximity()
    {
        SharedPreferences sp = getSharedPreferences(pref_StartOnProximity,0);
        boolean startOnProximity = sp.getBoolean("myStore",false);
        return startOnProximity;
    }
    public void writeStartOnPromity(boolean pref)
    {
        SharedPreferences.Editor editor = getSharedPreferences(pref_StartOnProximity,0).edit();
        editor.putBoolean("myStore", pref);
        editor.commit();
    }

    public boolean getStopOnProximityLost()
    {
        SharedPreferences sp = getSharedPreferences(pref_StopOnProximityLost,0);
        boolean stopOnProximityLost = sp.getBoolean("myStore",false);
        return stopOnProximityLost;
    }
    public void writeStopOnProximityLost(boolean pref)
    {
        SharedPreferences.Editor editor = getSharedPreferences(pref_StopOnProximityLost,0).edit();
        editor.putBoolean("myStore", pref);
        editor.commit();
    }

    private static void populateDataTable(JSONObject jsonData) {
        mTblData.removeAllViews();
        SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        List<String> hideItems = Arrays.asList("apikey");
        List<String> asDate = Arrays.asList("disabledsince");
        Iterator<?> iterator = jsonData.keys();
        while (iterator.hasNext()) {
            TableRow row = new TableRow(getInstance());
            Object key = iterator.next();
            try {
                if (!hideItems.contains(key)) {
                    Object value = jsonData.get(key.toString());
                    TextView col1 = new TextView(getInstance());
                    TextView col2 = new TextView(getInstance());
                    col1.setText(key.toString());
                    if (asDate.contains(key)) {
                        long unixSeconds = Long.parseLong(value.toString());
                        Date date = new Date(unixSeconds*1000L);
                        String formattedDate = sdf.format(date);
                        col2.setText(formattedDate);
                    } else {
                        col2.setText(value.toString());
                    }
                    col2.setGravity(Gravity.RIGHT);
                    row.addView(col1);
                    row.addView(col2);
                    mTblData.addView(row);
                }
            } catch (JSONException ignored) {
                ignored.printStackTrace();
            }
        }
        TableRow row1 = new TableRow(getInstance());
        TextView col1 = new TextView(getInstance());
        TextView col2 = new TextView(getInstance());
        col1.setText("apiurl");
        col2.setText(apiUrl);
        col2.setGravity(Gravity.RIGHT);
        row1.addView(col1);
        row1.addView(col2);

        row1.setBackgroundColor(mResources.getColor(R.color.colorBackground));
        mTblData.addView(row1);
        TableRow row2 = new TableRow(getInstance());
        TextView ts1 = new TextView(getInstance());
        TextView ts2 = new TextView(getInstance());
        ts1.setText("lastpoll");
        Date date = new Date(lastPoll);
        String formattedDate = sdf.format(date);
        ts2.setText(formattedDate);
        ts2.setGravity(Gravity.RIGHT);
        row2.addView(ts1);
        row2.addView(ts2);
        row2.setBackgroundColor(mResources.getColor(R.color.colorBackground));
        mTblData.addView(row2);

    }

    public static void doPoll() {
        try {
            RequestQueue requestQueue;
            //init rest client
            requestQueue = Volley.newRequestQueue(getInstance());
            // Request a string response from the provided URL.
            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(ScraperActivity.apiUrl + "state",
                    new Response.Listener<JSONArray>() {
                        @Override
                        public void onResponse(JSONArray response) {
                            try {
                                JSONObject state = response.getJSONObject(0);
                                disableScraping = state.getBoolean("disablescraping");
                                carAsleep = state.getString("state");
                                ScraperActivity.getInstance().setScrapeState(disableScraping);
                                populateDataTable(state);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            if (error.networkResponse != null) {
                                if (error.networkResponse.statusCode == 400) {
                                    Toast toast = Toast.makeText(instance,
                                            "Wrong API Key: Code 400: " + error.toString(),
                                            Toast.LENGTH_SHORT);
                                    toast.show();
                                } else {
                                    Log.e("volley", "error" + error.toString());
                                    Toast toast = Toast.makeText(instance,
                                            "Connection Problem: " + error.toString(),
                                            Toast.LENGTH_SHORT);
                                    toast.show();
                                }
                            } else {
                                Log.e("volley", "error" + error.toString());
                                Toast toast = Toast.makeText(instance,
                                        "Connection Problem: " + error.toString(),
                                        Toast.LENGTH_SHORT);
                                toast.show();
                            }
                        }
                    }) {
                @Override
                public Map<String,String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    //headers.put("Content-Type", "application/json");
                    headers.put("apikey", ScraperActivity.apiKey);
                    return headers;
                }
            };
            requestQueue.add(jsonArrayRequest);
            lastPoll = System.currentTimeMillis();
        } finally {
            //
            swipeLayout.setRefreshing(false);
        }
    }

    public static void setScraper(boolean doScrape) {
        //Perform http post
        RequestQueue requestQueue;
        requestQueue = Volley.newRequestQueue(getInstance());
        JSONObject json = new JSONObject();
        try {
            json.put("command","scrape");
            json.put("value",doScrape);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, apiUrl + "switch", json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("POST Response", response.toString());

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.e("POST Error", "Post error: " + error.getMessage());
            }
        }) {
            @Override
            public Map<String,String> getHeaders() throws AuthFailureError {
                HashMap<String, String> headers = new HashMap<String, String>();
                //headers.put("Content-Type", "application/json");
                headers.put("apikey", ScraperActivity.apiKey);
                return headers;
            }
        };
        requestQueue.add(jsonObjectRequest);

        // Since the user initiated an action, we need to cancel all delayed actions as invalid
        ScraperActivity.getInstance().stopBtTimeout();

        //Give API 100ms to accomodate..
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                doPoll();
            }
        }, 100);
        doPoll();
    }

    private void switchScraper() {
        setScraper(!disableScraping);
    }

    private BroadcastReceiver mAclConnectReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

        if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
            Log.i("btDevice", "ACL Connect Device: "+device.getName() + " " + device.getAddress());
            //todo: proximity get trigger mandatory if disable checked?
            if (mDisableBTProxmity.isChecked()) {
                stopBtTimeout();
            }
            if ((device.getName().equals(getBtName())) & mEnableBTProxmity.isChecked()) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Proximity Detected...",
                        Toast.LENGTH_SHORT);
                toast.show();
                notifySmartscrape("Proximity detected","Starting Scrape."
                        ,"Bluetooth Proximity to your car ist detected. You configured to turn on scraping when this happens"
                        ,10);
                setScraper(false);
            }
        }
        if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())
                || BluetoothDevice.ACTION_ACL_DISCONNECT_REQUESTED.equals(intent.getAction())) {
            Log.i("btDevice", "ACL Disconnect Device: "+device.getName() + " " + device.getAddress());
            if ((device.getName().equals(getBtName())) & mDisableBTProxmity.isChecked()) {
                Toast toast = Toast.makeText(getApplicationContext(),
                        "Proximity lost detected...",
                        Toast.LENGTH_SHORT);
                toast.show();
                startBtTimeout();
            }
        }
        }
    };
}

