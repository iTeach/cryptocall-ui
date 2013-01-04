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

package org.cryptocall.util;

import org.cryptocall.CryptoCallSession;
import org.cryptocall.service.CryptoCallIntentService;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import android.telephony.PhoneNumberUtils;
import android.telephony.SmsManager;

import android.util.Log;

public class SmsHelper {

    CryptoCallIntentService mCryptoCallService;

    public SmsHelper(CryptoCallIntentService cryptoCallService) {
        this.mCryptoCallService = cryptoCallService;
    }

    /**
     * Send CryptoCall SMS
     * 
     * @param context
     * @param phoneNumber
     * @param ip
     * @param port
     * @param extra
     */
    public void sendCryptoCallSms(Context context, CryptoCallSession session) {
        // build message
        String message = Constants.SMS_PREFIX + session.serverIp + Constants.SMS_SEPERATOR
                + session.serverPort + Constants.SMS_SEPERATOR + "-"; // reserved for future

        sendSms(context, session.peerTelephoneNumber, message);
    }

    public BroadcastReceiver SmsSendReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            switch (getResultCode()) {
            case Activity.RESULT_OK:
                mCryptoCallService.sendUpdateUiToHandler("SMS was send!");
                break;
            case SmsManager.RESULT_ERROR_GENERIC_FAILURE:
                mCryptoCallService.sendUpdateUiToHandler("Generic failure");
                break;
            case SmsManager.RESULT_ERROR_NO_SERVICE:
                mCryptoCallService.sendUpdateUiToHandler("No service");
                break;
            case SmsManager.RESULT_ERROR_NULL_PDU:
                mCryptoCallService.sendUpdateUiToHandler("Null PDU");
                break;
            case SmsManager.RESULT_ERROR_RADIO_OFF:
                mCryptoCallService.sendUpdateUiToHandler("Radio off");
                break;
            }
        }
    };

    public BroadcastReceiver SmsDeliveredReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context arg0, Intent arg1) {
            switch (getResultCode()) {
            case Activity.RESULT_OK:
                mCryptoCallService.sendUpdateUiToHandler("SMS delivered");
                break;
            case Activity.RESULT_CANCELED:
                mCryptoCallService.sendUpdateUiToHandler("SMS not delivered");
                break;
            }
        }
    };

    /**
     * Send generic SMS
     * 
     * @param phoneNumber
     * @param message
     */
    public void sendSms(Context context, String phoneNumber, String message) {
        mCryptoCallService.sendUpdateUiToHandler("Sending CryptoCall SMS...");

        if (PhoneNumberUtils.isWellFormedSmsAddress(phoneNumber)) {
            String SENT = "SMS_SENT";
            String DELIVERED = "SMS_DELIVERED";

            PendingIntent sentPI = PendingIntent.getBroadcast(context, 0, new Intent(SENT), 0);

            PendingIntent deliveredPI = PendingIntent.getBroadcast(context, 0,
                    new Intent(DELIVERED), 0);

            /* when the SMS has been sent */
            context.registerReceiver(SmsSendReceiver, new IntentFilter(SENT));

            /* when the SMS has been delivered */
            context.registerReceiver(SmsDeliveredReceiver, new IntentFilter(DELIVERED));

            Log.d(Constants.TAG, "Sending SMS to " + phoneNumber + " with message: " + message);

            SmsManager sms = SmsManager.getDefault();
            sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);

        } else {
            mCryptoCallService.sendUpdateUiToHandler("SMS destination invalid - try again");
        }
    }

    public void unregisterReceivers(Context context) {
        context.unregisterReceiver(SmsSendReceiver);
        context.unregisterReceiver(SmsDeliveredReceiver);
    }

}
