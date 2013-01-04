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

import org.cryptocall.CryptoCallSession;
import org.cryptocall.R;
import org.cryptocall.service.CryptoCallIntentService;
import org.cryptocall.util.Constants;
import org.cryptocall.util.Log;

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
    public static final String EXTRA_SEND_SMS = "sendSms";

    Activity mActivity;
    ProgressBar mProgress;
    TextView mStatus;

    CryptoCallSession mSession;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case CryptoCallIntentService.HANDLER_MSG_UPDATE_UI:
                mStatus.setText(msg.getData().getString(
                        CryptoCallIntentService.HANDLER_DATA_MESSAGE));
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

            // Send a sms with ip and port, when done manual connection this should not be done
            boolean sendSms = true;
            if (extras.containsKey(EXTRA_SEND_SMS)) {
                sendSms = extras.getBoolean(EXTRA_SEND_SMS);
            }

            mSession = new CryptoCallSession();
            mSession.peerEmail = email;

            Intent serviceIntent = new Intent(mActivity, CryptoCallIntentService.class);
            serviceIntent.putExtra(CryptoCallIntentService.EXTRA_ACTION,
                    CryptoCallIntentService.ACTION_START_SENDING);
            serviceIntent
                    .putExtra(CryptoCallIntentService.EXTRA_MESSENGER, new Messenger(mHandler));

            Bundle data = new Bundle();
            data.putParcelable(CryptoCallIntentService.DATA_CRYPTOCALL_SESSION, mSession);
            data.putBoolean(CryptoCallIntentService.DATA_SEND_SMS, sendSms);
            serviceIntent.putExtra(CryptoCallIntentService.EXTRA_DATA, data);

            startService(serviceIntent);
        } else {
            Log.e(Constants.TAG, "Missing email in intent!");
        }
    }

}
