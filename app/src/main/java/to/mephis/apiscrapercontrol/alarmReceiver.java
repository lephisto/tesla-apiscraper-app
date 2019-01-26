package to.mephis.apiscrapercontrol;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class alarmReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, apiPoller.class);
        context.startService(i);
    }
}
