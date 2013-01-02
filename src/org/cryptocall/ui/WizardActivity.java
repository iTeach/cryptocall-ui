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
import org.cryptocall.util.ProtectedEmailUtils;
import org.thialfihar.android.apg.integration.ApgData;
import org.thialfihar.android.apg.integration.ApgIntentHelper;
import org.thialfihar.android.apg.integration.ApgUtil;

import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

public class WizardActivity extends SherlockFragmentActivity {
    private ApgIntentHelper mApgIntentHelper;

    private int mCurrentScreen;

    // values for mCurrentScreen
    private static final int SCREEN_MAIN = 0;
    private static final int SCREEN_GENERATE_KEYRING_INPUT = 1;
    private static final int SCREEN_SELECT_KEYRING = 2;
    private static final int SCREEN_SUCCESS = 3;

    Button mBackButton;
    Button mNextButton;
    MainFragment mMainFragment;
    GenerateKeyringFragment mGenerateKeyRingFragment;
    SelectKeyringFragment mSelectKeyringFragment;
    SuccessFragment mSuccessFragment;

    ApgData mApgData;

    private static long mMasterKeyId;
    private static String mProtectedEmail;
    private static String mTelephoneNumber;

    public static void prefillTelephoneNumber(Context context, EditText editText) {
        // prefill telephone number if available
        TelephonyManager tMgr = (TelephonyManager) context
                .getSystemService(Context.TELEPHONY_SERVICE);
        String telephoneNumber = tMgr.getLine1Number();
        if (telephoneNumber != null) {
            Log.d(Constants.TAG, "TelephoneNumber: " + telephoneNumber);
            editText.setText(telephoneNumber);
        } else {
            Log.d(Constants.TAG, "TelephoneNumber could not be retrived with TelephonyManager!");
        }
    }

    private static boolean isEditTextNotEmpty(Context context, EditText editText) {
        boolean output = true;
        if (editText.getText().toString().length() == 0) {
            editText.setError(context.getString(R.string.wizard_error_blank));
            output = false;
        } else {
            editText.setError(null);
        }

        return output;
    }

    private static boolean isEditTextValidTelephoneNumber(Context context, EditText editText) {
        boolean output = true;

        if (!PhoneNumberUtils.isWellFormedSmsAddress(editText.getText().toString())) {
            editText.setError(context.getString(R.string.wizard_error_blank));
            output = false;
        } else {
            if (!editText.getText().toString().startsWith("+")
                    || !PhoneNumberUtils.isGlobalPhoneNumber(editText.getText().toString())) {
                editText.setError(context.getString(R.string.wizard_error_not_international));
                output = false;
            } else {
                editText.setError(null);
            }
        }

        return output;
    }

    public static class MainFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            return inflater.inflate(R.layout.wizard_main_fragment, container, false);
        }
    }

    public static class GenerateKeyringFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.wizard_generate_keyring_fragment, container,
                    false);

            // prefill telephone number if available
            EditText telephoneNumberEdit = (EditText) view
                    .findViewById(R.id.wizard_generate_keyring_telephone_number);
            WizardActivity.prefillTelephoneNumber(getActivity(), telephoneNumberEdit);
            return view;
        }
    }

    public static class SelectKeyringFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.wizard_select_keyring_fragment, container, false);

            // prefill telephone number if available
            EditText telephoneNumberEdit = (EditText) view
                    .findViewById(R.id.wizard_select_keyring_telephone_number);
            WizardActivity.prefillTelephoneNumber(getActivity(), telephoneNumberEdit);
            return view;
        }
    }

    public static class SuccessFragment extends Fragment {
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View view = inflater.inflate(R.layout.wizard_success_fragment, container, false);

            TextView telephoneNumberTextView = (TextView) view
                    .findViewById(R.id.wizard_success_telephone_number);
            TextView protectedEmailTextView = (TextView) view
                    .findViewById(R.id.wizard_success_protected_email);

            telephoneNumberTextView.setText(mTelephoneNumber);
            protectedEmailTextView.setText(mProtectedEmail);

            return view;
        }
    }

    /**
     * Loads new fragment
     * 
     * @param fragment
     */
    private void loadFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.wizard_fragment_container, fragment);
        fragmentTransaction.commit();
    }

    /**
     * Instantiate View and initialize fragments for this Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mApgData = new ApgData();

        setContentView(R.layout.wizard_activity);
        mBackButton = (Button) findViewById(R.id.wizard_activity_back);
        mNextButton = (Button) findViewById(R.id.wizard_activity_next);

        mMainFragment = new MainFragment();
        loadFragment(mMainFragment);
        mCurrentScreen = SCREEN_MAIN;

        mApgIntentHelper = new ApgIntentHelper(this);
    }

    public void nextOnClick(View view) {
        switch (mCurrentScreen) {
        case SCREEN_MAIN:
            RadioButton generateKeyringRadio = (RadioButton) findViewById(R.id.wizard_activity_generate_keyring_radio);
            RadioButton selectKeyringRadio = (RadioButton) findViewById(R.id.wizard_activity_select_keyring_radio);

            if (generateKeyringRadio.isChecked()) {
                mGenerateKeyRingFragment = new GenerateKeyringFragment();
                loadFragment(mGenerateKeyRingFragment);
                mCurrentScreen = SCREEN_GENERATE_KEYRING_INPUT;
            } else if (selectKeyringRadio.isChecked()) {
                mSelectKeyringFragment = new SelectKeyringFragment();
                loadFragment(mSelectKeyringFragment);
                mCurrentScreen = SCREEN_SELECT_KEYRING;
            }

            break;

        case SCREEN_GENERATE_KEYRING_INPUT:
            EditText generateNicknameEdit = (EditText) findViewById(R.id.wizard_generate_keyring_nickname);
            EditText generateTelephoneNumberEdit = (EditText) findViewById(R.id.wizard_generate_keyring_telephone_number);

            if (isEditTextNotEmpty(this, generateNicknameEdit)
                    && isEditTextValidTelephoneNumber(this, generateTelephoneNumberEdit)) {
                String nickname = generateNicknameEdit.getText().toString();
                String number = generateTelephoneNumberEdit.getText().toString();
                Log.d(Constants.TAG, "nickname: " + nickname);
                Log.d(Constants.TAG, "number: " + number);

                String protectedEmail = ProtectedEmailUtils.generateProtectedEmail(number);
                Log.d(Constants.TAG, "protectedEmail: " + protectedEmail);

                String id = nickname + " (CryptoCall) <" + protectedEmail + ">";

                Log.d(Constants.TAG, "id: " + id);

                mApgIntentHelper.createNewKey(id, true, true);
            }
            break;

        case SCREEN_SELECT_KEYRING:
            EditText selectTelephoneNumberEdit = (EditText) findViewById(R.id.wizard_select_keyring_telephone_number);

            if (isEditTextValidTelephoneNumber(this, selectTelephoneNumberEdit)) {
                mApgIntentHelper.selectSecretKey();
            }
            break;

        case SCREEN_SUCCESS:
            // save into prefs
            PreferencesHelper.setPgpEmail(this, mProtectedEmail);
            PreferencesHelper.setPgpMasterKeyId(this, mMasterKeyId);
            PreferencesHelper.setTelephoneNumber(this, mTelephoneNumber);

            PreferencesHelper.setFirstStart(this, false);

            // go to base activity
            Intent baseIntent = new Intent(this, BaseActivity.class);
            startActivity(baseIntent);
            finish();

            break;

        default:
            break;
        }

        // set button to "back"
        mBackButton.setText(R.string.button_back);
    }

    public void backOnClick(View view) {
        if (mCurrentScreen == SCREEN_MAIN) {
            finish();
        } else {
            loadFragment(mMainFragment);
            mCurrentScreen = SCREEN_MAIN;
            mBackButton.setText(R.string.button_cancel);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // this updates the mApgData object to the result of the methods
        boolean result = mApgIntentHelper.onActivityResult(requestCode, resultCode, data, mApgData);
        if (result) {
            switch (mCurrentScreen) {
            case SCREEN_GENERATE_KEYRING_INPUT:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(Constants.TAG, mApgData.toString());
                    long generatedKeyId = mApgData.getSecretKeyId();
                    String generatedUserId = mApgData.getSecretKeyUserId();

                    // TODO: handle keys with many user ids

                    String[] split = ApgUtil.splitUserId(generatedUserId);
                    String generatedEmail = split[1];
                    Log.d(Constants.TAG, "generatedEmail: " + generatedEmail);
                    byte[] generatedSalt;
                    try {
                        generatedSalt = ProtectedEmailUtils
                                .getSaltFromProtectedEmail(generatedEmail);

                        EditText inputNumberEdit = (EditText) findViewById(R.id.wizard_generate_keyring_telephone_number);

                        String inputNumber = inputNumberEdit.getText().toString();
                        Log.d(Constants.TAG, "inputNumber: " + inputNumber);
                        String protectedInputEmail = ProtectedEmailUtils.generateProtectedEmail(
                                inputNumber, generatedSalt);
                        Log.d(Constants.TAG, "protectedInputEmail: " + protectedInputEmail);

                        if (generatedEmail.equals(protectedInputEmail)) {
                            mMasterKeyId = generatedKeyId;
                            mProtectedEmail = generatedEmail;
                            mTelephoneNumber = inputNumber;

                            mCurrentScreen = SCREEN_SUCCESS;
                            // success fragment is handled in onResume, due to android bug
                            // http://stackoverflow.com/questions/10114324/show-dialogfragment-from-onactivityresult
                        } else {
                            Toast.makeText(this, R.string.wizard_error_not_matching,
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(Constants.TAG, "Parsing problem!", e);
                    }

                }
                break;
            case SCREEN_SELECT_KEYRING:
                if (resultCode == Activity.RESULT_OK) {
                    Log.d(Constants.TAG, mApgData.toString());
                    long selectedKeyId = mApgData.getSecretKeyId();
                    String selectedUserId = mApgData.getSecretKeyUserId();

                    // TODO: handle keys with many user ids

                    String[] split = ApgUtil.splitUserId(selectedUserId);
                    String selectedEmail = split[1];
                    Log.d(Constants.TAG, "selectedEmail: " + selectedEmail);
                    byte[] selectedSalt;
                    try {
                        selectedSalt = ProtectedEmailUtils.getSaltFromProtectedEmail(selectedEmail);

                        EditText inputNumberEdit = (EditText) findViewById(R.id.wizard_select_keyring_telephone_number);

                        String inputNumber = inputNumberEdit.getText().toString();
                        Log.d(Constants.TAG, "inputNumber: " + inputNumber);
                        String protectedInputEmail = ProtectedEmailUtils.generateProtectedEmail(
                                inputNumber, selectedSalt);
                        Log.d(Constants.TAG, "protectedInputEmail: " + protectedInputEmail);

                        if (selectedEmail.equals(protectedInputEmail)) {
                            mMasterKeyId = selectedKeyId;
                            mProtectedEmail = selectedEmail;
                            mTelephoneNumber = inputNumber;

                            mCurrentScreen = SCREEN_SUCCESS;
                            // success fragment is handled in onResume, due to android bug
                            // http://stackoverflow.com/questions/10114324/show-dialogfragment-from-onactivityresult
                        } else {
                            Toast.makeText(this, R.string.wizard_error_not_matching,
                                    Toast.LENGTH_LONG).show();
                        }
                    } catch (Exception e) {
                        Log.e(Constants.TAG, "Parsing problem!", e);
                    }

                }
                break;

            default:
                break;
            }
        }

        // continue with other activity results
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (mCurrentScreen == SCREEN_SUCCESS) {
            mSuccessFragment = new SuccessFragment();
            loadFragment(mSuccessFragment);
            mBackButton.setEnabled(false);
            // set button to "finish"
            mNextButton.setText(R.string.button_finish);
        }
    }

}
