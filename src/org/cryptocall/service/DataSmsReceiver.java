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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;

/**
 * Receives Data SMS and proceed handling them
 */
public class DataSmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle bundle = intent.getExtras();

        String recMsgString = "";
        String fromAddress = "";
        SmsMessage recMsg = null;
        byte[] data = null;
        if (bundle != null) {
            // ---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            for (int i = 0; i < pdus.length; i++) {
                recMsg = SmsMessage.createFromPdu((byte[]) pdus[i]);

                try {
                    data = recMsg.getUserData();
                } catch (Exception e) {

                }
                if (data != null) {
                    for (int index = 0; index < data.length; ++index) {
                        recMsgString += Character.toString((char) data[index]);
                    }
                }

                fromAddress = recMsg.getOriginatingAddress();
            }

            // check if its a cryptocall sms
            if (CryptoCallSessionUtils.isCryptoCallSms(recMsgString)) {

                // start received sms activity
                Intent activityIntent = new Intent();
                activityIntent.setClass(context, SmsReceivedActivity.class);
                activityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                activityIntent.putExtra(SmsReceivedActivity.EXTRA_SMS_BODY, recMsgString);
                activityIntent.putExtra(SmsReceivedActivity.EXTRA_SMS_FROM, fromAddress);

                context.startActivity(activityIntent);

                // this is a cryptocall sms, don't let the other applications get the intent!
                abortBroadcast();
            }
        }
    }
}
