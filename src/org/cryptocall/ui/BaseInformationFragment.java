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

import org.cryptocall.util.PreferencesHelper;
import org.sufficientlysecure.keychain.integration.KeychainContentProviderHelper;
import org.sufficientlysecure.keychain.integration.KeychainIntentHelper;
import org.sufficientlysecure.keychain.integration.KeychainUtil;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class BaseInformationFragment extends Fragment {
    private BaseActivity mBaseActivity;

    private TextView mTelTextView;
    private TextView mNicknameTextView;
    private Button mEditKeyringButton;
    private Button mSelectKeyringButton;

    private KeychainIntentHelper mKeychainIntentHelper;

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.base_information_fragment, container, false);

        mKeychainIntentHelper = new KeychainIntentHelper(getActivity());

        // get views
        mEditKeyringButton = (Button) view
                .findViewById(R.id.base_information_fragment_edit_keyring_button);
        mSelectKeyringButton = (Button) view
                .findViewById(R.id.base_information_fragment_select_keyring_button);
        mTelTextView = (TextView) view.findViewById(R.id.base_information_fragment_tel);
        mNicknameTextView = (TextView) view.findViewById(R.id.base_information_fragment_nickname);

        mEditKeyringButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                long masterKeyId = PreferencesHelper.getPgpMasterKeyId(mBaseActivity);
                mKeychainIntentHelper.editKey(masterKeyId);
            }
        });

        mSelectKeyringButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                startActivity(new Intent(getActivity(), WizardActivity.class));
            }
        });

        return view;
    }

    /**
     * onResume set informations and QrCode
     */
    @Override
    public void onResume() {
        super.onResume();

        // set textview from preferenes
        mTelTextView.setText(PreferencesHelper.getTelephoneNumber(mBaseActivity));

        // set nickname from keyring
        String userId = (new KeychainContentProviderHelper(mBaseActivity))
                .getUserId(PreferencesHelper.getPgpMasterKeyId(mBaseActivity), true);
        String[] splitUserId = KeychainUtil.splitUserId(userId);
        mNicknameTextView.setText(splitUserId[0]);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBaseActivity = (BaseActivity) getActivity();
    }
}
