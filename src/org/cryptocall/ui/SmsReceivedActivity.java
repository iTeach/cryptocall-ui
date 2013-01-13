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
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.view.View;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;
import com.csipsimple.utils.Ringer;

public class SmsReceivedActivity extends SherlockActivity {
    public static final String EXTRA_CRYPTOCALL_EMAIL = "email";
    public static final String EXTRA_SERVER_IP = "serverIp";
    public static final String EXTRA_SERVER_PORT = "serverPort";
    // or
    public static final String EXTRA_SMS_BODY = "smsMessage";
    public static final String EXTRA_SMS_FROM = "smsFrom";

    Activity mActivity;
    ProgressBar mProgress;
    TextView mStatus;
    Button mAccept;
    Button mDecline;

    CryptoCallSession mSession;

    Ringer mRinger;

    private Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
            case CryptoCallIntentService.HANDLER_MSG_UPDATE_UI:
                Bundle dataUi = msg.getData();
                mStatus.setText(dataUi.getString(CryptoCallIntentService.HANDLER_DATA_MESSAGE));
                if (dataUi.containsKey(CryptoCallIntentService.HANDLER_DATA_PROGRESS)) {
                    mProgress.setProgress(dataUi
                            .getInt(CryptoCallIntentService.HANDLER_DATA_PROGRESS));
                }
                break;

            case CryptoCallIntentService.HANDLER_MSG_RETURN_SESSION:
                Log.d(Constants.TAG, "HANDLER_MSG_RETURN_SESSION");
                // get returned session
                Bundle data = msg.getData();
                mSession = data.getParcelable(CryptoCallIntentService.DATA_CRYPTOCALL_SESSION);

                showButtons();
                break;

            default:
                break;
            }

        }

    };

    private static final int COUNTDOWN_TIMEOUT = 60 * 1000;

    private CountDownTimer mCountDownTimer = new CountDownTimer(COUNTDOWN_TIMEOUT, 1000) {

        @Override
        public void onFinish() {
            closeActivityAndStopSip();
        }

        @Override
        public void onTick(long millisUntilFinished) {
            mProgress.setProgress((int) millisUntilFinished);
        }
    };

    private void showButtons() {
        mAccept.setVisibility(View.VISIBLE);
        mDecline.setVisibility(View.VISIBLE);
    }

    /**
     * Executed onCreate of Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // show on lock screen and turn screen on while showing activity
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.sms_received_activity);

        // Internal CSipSimple API!
        mRinger = new Ringer(this);
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (ringtoneUri != null) {
            mRinger.ring(null, ringtoneUri.toString());
        }

        mActivity = this;
        mStatus = (TextView) findViewById(R.id.sms_received_status);
        mProgress = (ProgressBar) findViewById(R.id.sms_received_progress);
        mAccept = (Button) findViewById(R.id.sms_received_accept_button);
        mDecline = (Button) findViewById(R.id.sms_received_decline_button);

        // init progress bar
        mProgress.setMax(COUNTDOWN_TIMEOUT);
        mProgress.setProgress(COUNTDOWN_TIMEOUT);
        mCountDownTimer.start();

        handleActions(getIntent());

        mAccept.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mCountDownTimer.cancel();
                // set progress bar to max 100% and current 0% to be used by service
                mProgress.setMax(100);
                mProgress.setProgress(0);

                if (mRinger != null) {
                    mRinger.stopRing();
                }

                Intent serviceIntent = new Intent(mActivity, CryptoCallIntentService.class);
                serviceIntent.putExtra(CryptoCallIntentService.EXTRA_ACTION,
                        CryptoCallIntentService.ACTION_START_RECEIVED);
                serviceIntent.putExtra(CryptoCallIntentService.EXTRA_MESSENGER, new Messenger(
                        mHandler));

                Bundle data = new Bundle();
                data.putParcelable(CryptoCallIntentService.DATA_CRYPTOCALL_SESSION, mSession);
                serviceIntent.putExtra(CryptoCallIntentService.EXTRA_DATA, data);

                startService(serviceIntent);
            }
        });

        mDecline.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                closeActivityAndStopSip();
            }
        });
    }

    private void closeActivityAndStopSip() {
        finish();

        // stop sip stack on decline
        Intent serviceIntent = new Intent(mActivity, CryptoCallIntentService.class);
        serviceIntent.putExtra(CryptoCallIntentService.EXTRA_ACTION,
                CryptoCallIntentService.ACTION_STOP_SIP_STACK);
        serviceIntent.putExtra(CryptoCallIntentService.EXTRA_MESSENGER, new Messenger(mHandler));
        startService(serviceIntent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mRinger != null) {
            mRinger.stopRing();
        }
    }

    private void handleActions(Intent intent) {
        Bundle extras = intent.getExtras();

        if (extras != null && extras.containsKey(EXTRA_CRYPTOCALL_EMAIL)
                && extras.containsKey(EXTRA_SERVER_IP) && extras.containsKey(EXTRA_SERVER_PORT)) {

            mSession = new CryptoCallSession();
            mSession.peerEmail = extras.getString(EXTRA_CRYPTOCALL_EMAIL);
            mSession.serverIp = extras.getString(EXTRA_SERVER_IP);
            mSession.serverPort = extras.getInt(EXTRA_SERVER_PORT);

            mStatus.setText(getString(R.string.status_connection, mSession.serverIp,
                    mSession.serverPort, mSession.peerEmail));

            showButtons();
        } else if (extras != null && extras.containsKey(EXTRA_SMS_BODY)
                && extras.containsKey(EXTRA_SMS_FROM)) {

            // get session from sms asynchronous in service, result is handled in mHandler
            Intent serviceIntent = new Intent(mActivity, CryptoCallIntentService.class);
            serviceIntent.putExtra(CryptoCallIntentService.EXTRA_ACTION,
                    CryptoCallIntentService.ACTION_START_PARSE_SMS);
            serviceIntent
                    .putExtra(CryptoCallIntentService.EXTRA_MESSENGER, new Messenger(mHandler));

            Bundle data = new Bundle();
            data.putString(CryptoCallIntentService.DATA_SMS_BODY, extras.getString(EXTRA_SMS_BODY));
            data.putString(CryptoCallIntentService.DATA_SMS_FROM, extras.getString(EXTRA_SMS_FROM));
            serviceIntent.putExtra(CryptoCallIntentService.EXTRA_DATA, data);

            startService(serviceIntent);
        } else {
            Log.e(Constants.TAG, "Missing extras in intent!");
        }
    }
}
