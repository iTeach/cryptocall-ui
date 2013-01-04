/*
 * Copyright (C) 2011-2013 Sergej Dechand <cryptocall@serj.de>
 *                         Dominik Schürmann <dominik@dominikschuermann.de>
 * 
 * This file is part of CryptoCall.
 * 
 * CryptoCall is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * CryptoCall is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with CryptoCall.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.cryptocall.service;

import org.cryptocall.ui.SmsReceivedActivity;
import org.cryptocall.util.CryptoCallSessionUtils;
import org.cryptocall.util.Constants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

/**
 * Receives SMS and proceed handling them
 * 
 */
public class SmsReceiver extends BroadcastReceiver {
    static final String ACTION = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        // if intent is SMS_RECEIVED handle here
        if (intent.getAction().equals(SmsReceiver.ACTION)) {
            String from = new String();
            String body = new String();

            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                for (Object pdu : pdus) {
                    SmsMessage messages = SmsMessage.createFromPdu((byte[]) pdu);

                    from = messages.getDisplayOriginatingAddress();
                    body = messages.getDisplayMessageBody();

                    Log.i(Constants.TAG, "[SmsReceiver] onReceiveIntent: " + from + ": " + body);

                    // check if its a cryptocall sms
                    if (CryptoCallSessionUtils.isCryptoCallSms(body)) {

                        // start received sms activity
                        Intent activityIntent = new Intent();
                        activityIntent.setClass(context, SmsReceivedActivity.class);
                        activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        
                        activityIntent.putExtra(SmsReceivedActivity.EXTRA_SMS_BODY, body);
                        activityIntent.putExtra(SmsReceivedActivity.EXTRA_SMS_FROM, from);

                        context.startActivity(activityIntent);

                        // this is a cryptocall sms, don't let the other applications get the
                        // intent!
                        // http://stackoverflow.com/questions/1741628/can-we-delete-an-sms-in-android-before-it-reaches-the-inbox/2566199#2566199
                        abortBroadcast();
                    }
                }
            }

        }
    }
}
