package to.mephis.apiscrapercontrol;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
public class apiPoller extends IntentService {

    private static boolean state_scraping = false;

    private static final String GET_STATE = "to.mephis.apiscrapercontrol.action.FOO";

    private static final String EXTRA_PARAM1 = "to.mephis.apiscrapercontrol.extra.PARAM1";


    public apiPoller() {
        super("apiPoller");
    }

    /**
     * Starts this service to perform action Foo with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    public static void startApiCall(Context context, String param1, String param2) {
        Intent intent = new Intent(context, apiPoller.class);
        intent.setAction(GET_STATE);
        intent.putExtra(EXTRA_PARAM1, param1);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        //Toast.makeText(this, "onhandleintent", Toast.LENGTH_SHORT).show();
        Log.i("apiPoller", "Service running");
        if (intent != null) {
            final String action = intent.getAction();
            Log.i("Intent", "intent");
            handleApiRequest();
        }
    }


    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private void handleApiRequest() {
        RequestQueue requestQueue;
        //init rest client
        requestQueue = Volley.newRequestQueue(getApplicationContext());
        // Request a string response from the provided URL.
        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(ScraperActivity.apiUrl + "/state",
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            JSONObject state = response.getJSONObject(0);
                            state_scraping = state.getBoolean("scraping");
                            ScraperActivity.getInstance().setScrapeState(state_scraping);
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
                });
        requestQueue.add(jsonArrayRequest);
    }
}
