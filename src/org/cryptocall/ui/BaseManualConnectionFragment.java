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
import org.cryptocall.util.Constants;
import org.cryptocall.util.Log;
import org.cryptocall.util.PreferencesHelper;
import org.sufficientlysecure.keychain.integration.KeychainData;
import org.sufficientlysecure.keychain.integration.KeychainIntentHelperSupportV4;
import org.sufficientlysecure.keychain.integration.KeychainUtil;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class BaseManualConnectionFragment extends Fragment {
    private BaseActivity mBaseActivity;

    private EditText mSendEmail;
    private Button mSendEmailButton;

    private EditText mReceiveEmail;
    private Button mReceiveEmailButton;

    private boolean mOnResultIsSendEmail;

    private EditText mIp;
    private EditText mPort;
    private Button mSending;
    private Button mReceived;

    private TextView mKeyTextView;
    private TextView mPgpMailTextView;

    private KeychainIntentHelperSupportV4 mKeychainIntentHelper;
    private KeychainData mKeychainData;

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.base_manual_connection_fragment, container,
                false);

        // get views
        mSendEmail = (EditText) view.findViewById(R.id.base_manual_connection_send_email);
        mSendEmailButton = (Button) view
                .findViewById(R.id.base_manual_connection_send_email_button);

        mReceiveEmail = (EditText) view.findViewById(R.id.base_manual_connection_receive_email);
        mReceiveEmailButton = (Button) view
                .findViewById(R.id.base_manual_connection_receive_email_button);

        mIp = (EditText) view.findViewById(R.id.base_manual_connection_ip);
        mPort = (EditText) view.findViewById(R.id.base_manual_connection_port);
        mSending = (Button) view.findViewById(R.id.base_manual_connection_send_button);
        mReceived = (Button) view.findViewById(R.id.base_manual_connection_received_button);

        mPgpMailTextView = (TextView) view.findViewById(R.id.base_manual_connection_pgp_mail);
        mKeyTextView = (TextView) view.findViewById(R.id.base_manual_connection_key);

        mPgpMailTextView.setText(PreferencesHelper.getPgpEmail(mBaseActivity));
        mKeyTextView.setText(String.valueOf(PreferencesHelper.getPgpMasterKeyId(mBaseActivity)));

        mSending.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    // start sending sms activity
                    Intent activityIntent = new Intent();
                    activityIntent.setClass(mBaseActivity, SmsSendingActivity.class);
                    activityIntent.putExtra(SmsSendingActivity.EXTRA_CRYPTOCALL_EMAIL, mSendEmail
                            .getText().toString());
                    activityIntent.putExtra(SmsSendingActivity.EXTRA_SEND_SMS, false);
                    mBaseActivity.startActivity(activityIntent);
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Invalid input?", e);
                }
            }
        });

        mReceived.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    // start received sms activity
                    Intent activityIntent = new Intent();
                    activityIntent.setClass(mBaseActivity, SmsReceivedActivity.class);
                    activityIntent.putExtra(SmsReceivedActivity.EXTRA_CRYPTOCALL_EMAIL,
                            mReceiveEmail.getText().toString());
                    activityIntent.putExtra(SmsReceivedActivity.EXTRA_SERVER_IP, mIp.getText()
                            .toString());
                    activityIntent.putExtra(SmsReceivedActivity.EXTRA_SERVER_PORT,
                            Integer.valueOf(mPort.getText().toString()));
                    mBaseActivity.startActivity(activityIntent);
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Invalid input?", e);
                }

            }
        });

        mSendEmailButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mOnResultIsSendEmail = true;
                mKeychainIntentHelper.selectPublicKeys(null);
            }
        });

        mReceiveEmailButton.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                mOnResultIsSendEmail = false;
                mKeychainIntentHelper.selectPublicKeys(null);
            }
        });

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBaseActivity = (BaseActivity) getActivity();

        mKeychainIntentHelper = new KeychainIntentHelperSupportV4(this);
        mKeychainData = new KeychainData();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this updates the mKeychainData object to the result of the methods
        boolean result = mKeychainIntentHelper.onActivityResult(requestCode, resultCode, data,
                mKeychainData);
        if (result && resultCode == Activity.RESULT_OK) {
            Log.d(Constants.TAG, mKeychainData.toString());
            String[] selectedUserIds = mKeychainData.getPublicUserIds();

            String[] split = KeychainUtil.splitUserId(selectedUserIds[0]);
            String selectedEmail = split[1];
            Log.d(Constants.TAG, "selectedEmail: " + selectedEmail);

            if (mOnResultIsSendEmail) {
                mSendEmail.setText(selectedEmail);
            } else {
                mReceiveEmail.setText(selectedEmail);
            }
        }

        // continue with other activity results
        super.onActivityResult(requestCode, resultCode, data);
    }
}
