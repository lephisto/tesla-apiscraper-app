package to.mephis.apiscrapercontrol

import android.app.IntentService
import android.content.Intent
import android.content.Context
import android.util.Log


/**
 * An [IntentService] subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 *
 *
 * TODO: Customize class - update intent actions, extra parameters and static
 * helper methods.
 */
class apiPoller : IntentService("apiPoller") {

    override fun onHandleIntent(intent: Intent?) {
        //Toast.makeText(this, "onhandleintent", Toast.LENGTH_SHORT).show();
        Log.i("apiPoller", "Service running")
        if (intent != null) {
            val action = intent.action
            Log.i("Intent", "intent")
            handleApiRequest()
        }
    }


    /**
     * Handle action Foo in the provided background thread with the provided
     * parameters.
     */
    private fun handleApiRequest() {
        ScraperActivity.doPoll()
    }

    companion object {

        private val state_scraping = false

        private val GET_STATE = "to.mephis.apiscrapercontrol.action.FOO"

        private val EXTRA_PARAM1 = "to.mephis.apiscrapercontrol.extra.PARAM1"

        /**
         * Starts this service to perform action Foo with the given parameters. If
         * the service is already performing a task this action will be queued.
         *
         * @see IntentService
         */
        fun startApiCall(context: Context, param1: String, param2: String) {
            val intent = Intent(context, apiPoller::class.java)
            intent.action = GET_STATE
            intent.putExtra(EXTRA_PARAM1, param1)
            context.startService(intent)
        }
    }
}
