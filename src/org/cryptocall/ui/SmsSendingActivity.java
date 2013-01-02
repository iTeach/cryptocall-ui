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

package org.cryptocall.ui;

import org.cryptocall.R;
import org.cryptocall.service.CryptoCallIntentService;
import org.cryptocall.util.Constants;
import org.cryptocall.util.CryptoCallSession;
import org.cryptocall.util.Log;
import org.cryptocall.util.SmsHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class SmsSendingActivity extends SherlockActivity {
    public static final String EXTRA_CRYPTOCALL_EMAIL = "email";

    // if given considered as manual without sending of sms:
    public static final String EXTRA_MANUAL_IP = "ip";
    public static final String EXTRA_MANUAL_PORT = "port";

    Activity mActivity;
    ProgressBar mProgress;
    static TextView mStatus;

    CryptoCallSession mCryptoCallSession;

    private static Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case SmsHelper.HANDLER_MSG_UPDATE_UI:
                mStatus.setText(msg.getData().getString(SmsHelper.HANDLER_DATA_MESSAGE));
                break;

            case CryptoCallIntentService.HANDLER_MSG_OKAY:

                break;

            default:
                break;
            }

        }

    };

    /**
     * Executed onCreate of Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sms_sending_activity);

        mActivity = this;
        mProgress = (ProgressBar) findViewById(R.id.sms_sending_progress);
        mStatus = (TextView) findViewById(R.id.sms_sending_status);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(EXTRA_CRYPTOCALL_EMAIL)) {

            String email = extras.getString(EXTRA_CRYPTOCALL_EMAIL);

            mCryptoCallSession = new CryptoCallSession(email);

            /* 0. Get corresponding telephoneNumber, name of receiver */
            mCryptoCallSession.getNameAndTelephoneNumber(mActivity);

            if (extras.containsKey(EXTRA_MANUAL_IP)) {
                String ip = extras.getString(EXTRA_MANUAL_IP);
                int port = extras.getInt(EXTRA_MANUAL_PORT);

                /* 1. get X509 certificate and pub key of receiver */
                Intent serviceIntent = new Intent(mActivity, CryptoCallIntentService.class);
                serviceIntent.putExtra(CryptoCallIntentService.EXTRA_ACTION,
                        CryptoCallIntentService.ACTION_PUB_KEY_AND_CERT);
                serviceIntent.putExtra(CryptoCallIntentService.EXTRA_MESSENGER, new Messenger(
                        mHandler));

                Bundle data = new Bundle();
                data.putString(CryptoCallIntentService.DATA_CRYPTOCALL_RECEIVER_EMAIL,
                        mCryptoCallSession.getEmail());
                serviceIntent.putExtra(CryptoCallIntentService.EXTRA_DATA, data);

                startService(serviceIntent);

                /* 2. Send SMS with my ip, port. TODO: sign? */
                // No sms in manual mode!
            } else {
                String ip = "192.168.1.1";
                int port = 666;

                /* 1. Open CSipSimple port with X509 certificate and pub key of receiver */

                /* 2. Send SMS with my ip, port. TODO: sign? */
                SmsHelper smsHelper = new SmsHelper(new Messenger(mHandler));
                smsHelper.sendCryptoCallSms(mActivity, mCryptoCallSession.getTelephoneNumber(), ip,
                        port, "");
            }

        } else {
            Log.e(Constants.TAG, "Missing email in intent!");
        }
    }
}
