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
import org.cryptocall.util.Log;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

public class SmsReceivedActivity extends SherlockActivity {
    public static final String EXTRA_CRYPTOCALL_EMAIL = "email";
    public static final String EXTRA_SERVER_IP = "serverIp";
    public static final String EXTRA_SERVER_PORT = "serverPort";

    Activity mActivity;
    ProgressBar mProgress;
    static TextView mStatus;
    Button mAccept;
    Button mDecline;

    String mEmail;
    String mServerIp;
    int mServerPort;

    private static Handler mHandler = new Handler() {

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

        setContentView(R.layout.sms_received_activity);

        mActivity = this;
        mStatus = (TextView) findViewById(R.id.sms_received_status);
        mAccept = (Button) findViewById(R.id.sms_received_accept_button);
        mDecline = (Button) findViewById(R.id.sms_received_decline_button);

        Bundle extras = getIntent().getExtras();
        if (extras != null && extras.containsKey(EXTRA_CRYPTOCALL_EMAIL)) {

            mEmail = extras.getString(EXTRA_CRYPTOCALL_EMAIL);
            mServerIp = extras.getString(EXTRA_SERVER_IP);
            mServerPort = extras.getInt(EXTRA_SERVER_PORT);

            mStatus.setText("Do you want to connect to " + mServerIp + ":" + mServerPort + "?");

        } else {
            Log.e(Constants.TAG, "Missing email in intent!");
        }

        mAccept.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                Intent serviceIntent = new Intent(mActivity, CryptoCallIntentService.class);
                serviceIntent.putExtra(CryptoCallIntentService.EXTRA_ACTION,
                        CryptoCallIntentService.ACTION_START_RECEIVED);
                serviceIntent.putExtra(CryptoCallIntentService.EXTRA_MESSENGER, new Messenger(
                        mHandler));

                Bundle data = new Bundle();
                data.putString(CryptoCallIntentService.DATA_PEER_CRYPTOCALL_EMAIL, mEmail);

                data.putString(CryptoCallIntentService.DATA_SERVER_IP, mServerIp);
                data.putInt(CryptoCallIntentService.DATA_SERVER_PORT, mServerPort);

                serviceIntent.putExtra(CryptoCallIntentService.EXTRA_DATA, data);

                startService(serviceIntent);

            }
        });

        mDecline.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                finish();
            }
        });
    }
}
