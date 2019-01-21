package to.mephis.apiscrapercontrol;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class alarmReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;
    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        //throw new UnsupportedOperationException("Not yet implemented");

        //ComponentName comp = new ComponentName(context.getPackageName(), GCMIntentService.class.getName());
        //context.startForegroundService(context,i);

        Intent i = new Intent(context, apiPoller.class);
        context.startService(i);


    }
}
