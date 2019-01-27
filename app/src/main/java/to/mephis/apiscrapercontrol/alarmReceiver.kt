package to.mephis.apiscrapercontrol

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class alarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val i = Intent(context, apiPoller::class.java)
        context.startService(i)
    }

    companion object {
        val REQUEST_CODE = 12345
    }
}
