package to.mephis.apiscrapercontrol;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.app.LoaderManager.LoaderCallbacks;

import android.content.CursorLoader;
import android.content.Loader;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.graphics.Color.GRAY;
import static android.graphics.Color.GREEN;


/**
 * Mainscreen for setting apiScraper Properties
 */
public class ScraperActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {


    /**
     * Shared pref store..
     */

    private static boolean polling_enabled = false;

    public static final String pref_btMac = "btMac";
    public static final String pref_apiUrl = "apiUrl";
    public static final String pref_apiKey = "apiKey";
    public static final String pref_Polling = "polling";

    public static String apiUrl = "";
    public static String apiKey = "";
    public static boolean disableScraping = false;

    // UI references.
    private EditText mBtMac;
    private EditText mApiUrl;
    private EditText mApiSecret;
    private TextView mDebugBox;
    private Button mBtnScraperState;
    private View mProgressView;
    private View mLoginFormView;
    private Switch mEnablePolling;

    // We do this to publish it to our other Classes
    private static ScraperActivity instance;
    public static ScraperActivity getInstance() {
        return instance;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;
        setContentView(R.layout.activity_login);

        mBtMac = (EditText) findViewById(R.id.btMac);
        mApiUrl = (EditText) findViewById(R.id.apiurl);
        mApiSecret = (EditText) findViewById(R.id.apikey);
        mBtnScraperState = (Button) findViewById(R.id.btn_scraperStatus);
        mDebugBox = (TextView) findViewById(R.id.debugbox);
        mEnablePolling = (Switch) findViewById(R.id.swEnablePolling);



//        Button m_btn_scraperStatus = (Button) findViewById(R.id.btn_scraperStatus);

        mBtnScraperState.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                switchScraper();
            }
        });

        mEnablePolling.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // do something, the isChecked will be
                // true if the switch is in the On position
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


        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);

        mBtMac.setText(getBtMac().toString());
        mApiUrl.setText(getapiUrl().toString());
        mApiSecret.setText(getapiKey().toString());

        mEnablePolling.setChecked(getPolling());

        doPoll();
    }

    // Setup a recurring alarm every half hour
    public void scheduleAlarm() {
        // Construct an intent that will execute the AlarmReceiver
        Intent intent = new Intent(getApplicationContext(), alarmReceiver.class);
        // Create a PendingIntent to be triggered when the alarm goes off
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, alarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        // First parameter is the type: ELAPSED_REALTIME, ELAPSED_REALTIME_WAKEUP, RTC_WAKEUP
        // Interval can be INTERVAL_FIFTEEN_MINUTES, INTERVAL_HALF_HOUR, INTERVAL_HOUR, INTERVAL_DAY
//        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
//                (1*1000), pIntent);

        alarm.setRepeating(AlarmManager.RTC_WAKEUP,firstMillis,(15*1000),pIntent);
    }

    public void cancelAlarm() {
        Intent intent = new Intent(getApplicationContext(), alarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, alarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
    }

    public void setScrapeState(boolean state)
    {
        if (!state) {
            mBtnScraperState.setText("Scraper is running");
            mBtnScraperState.setBackgroundColor(GREEN);

        } else {
            mBtnScraperState.setText("Scraper is throttling");
            mBtnScraperState.setBackgroundColor(GRAY);
        }
        disableScraping = state;
    }

    public String getBtMac()
    {
        SharedPreferences sp = getSharedPreferences(pref_btMac,0);
        String str = sp.getString("myStore","00:00:00:00:00:00");
        return str;
    }
    public void writeBtMac(String pref)
    {
        SharedPreferences.Editor editor = getSharedPreferences(pref_btMac,0).edit();
        editor.putString("myStore", pref);
        editor.commit();
    }

    public String getapiUrl()
    {
        SharedPreferences sp = getSharedPreferences(pref_apiUrl,0);
        String str = sp.getString("myStore","00:00:00:00:00:00");
        apiUrl = str;
        return str;
    }
    public void writeApiUrl(String pref)
    {
        SharedPreferences.Editor editor = getSharedPreferences(pref_apiUrl,0).edit();
        editor.putString("myStore", pref);
        editor.commit();
    }

    public String getapiKey()
    {
        SharedPreferences sp = getSharedPreferences(pref_apiKey,0);
        String str = sp.getString("myStore","00:00:00:00:00:00");
        apiKey = str;
        return str;
    }
    public void writeApiKey(String pref)
    {
        SharedPreferences.Editor editor = getSharedPreferences(pref_apiKey,0).edit();
        editor.putString("myStore", pref);
        editor.commit();
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

    public static void doPoll() {
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
                            ScraperActivity.getInstance().setScrapeState(disableScraping);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("volley", "error" + error.toString());
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
    }

    public static void setScraper(boolean doScrape) {

        //Perform http post
        RequestQueue requestQueue;
        requestQueue = Volley.newRequestQueue(getInstance());
        JSONObject json = new JSONObject();
        try {
            json.put("command","switch");
            json.put("value",!disableScraping);
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
                //serverResp.setText("Error getting response");
                Log.e("POST Error", "Post error");
            }
        });
        //jsonObjectRequest.setTag(REQ_TAG);
        requestQueue.add(jsonObjectRequest);


    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void switchScraper() {

        // Reset errors.
        mBtMac.setError(null);
        mApiUrl.setError(null);
        mApiSecret.setError(null);

        // Store values at the time of the login attempt.
        String btmac = mBtMac.getText().toString();
        String apiurl = mApiUrl.getText().toString();
        String apisecret = mApiSecret.getText().toString();

        //Save Settings
        writeBtMac(btmac);
        writeApiUrl(apiurl);
        writeApiKey(apisecret);

        boolean cancel = false;
        View focusView = null;


        setScraper(disableScraping);


/**        //Perform http post
        RequestQueue requestQueue;
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        JSONObject json = new JSONObject();
        try {
            json.put("command","switch");
            json.put("value",!disableScraping);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, apiurl + "switch", json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Log.i("POST Response", response.toString());

                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                //serverResp.setText("Error getting response");
                Log.e("POST Error", "Post error");
            }
        });
        //jsonObjectRequest.setTag(REQ_TAG);
        requestQueue.add(jsonObjectRequest); */
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        //addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    /*
    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(ScraperActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }*/


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }


            // TODO: register the new account here.
            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            showProgress(false);

            /*if (success) {
                finish();
            } else {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            }*/
        }

        @Override
        protected void onCancelled() {
            showProgress(false);
        }
    }
}

