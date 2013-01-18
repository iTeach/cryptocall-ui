/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de
 * Copyright (C) 2010 Sam Steele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cryptocall.syncadapter;

import org.cryptocall.util.Constants;
import org.cryptocall.util.Log;

import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.os.Bundle;

public class CreateAccountActivity extends AccountAuthenticatorActivity {

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        AccountHelper accHelper = new AccountHelper(this);
        Bundle result = accHelper.addAccount();

        if (result != null) {
            if (result.containsKey(AccountManager.KEY_ACCOUNT_NAME)) {
                // Force a sync! Even when background sync is disabled, this will force one sync!
                accHelper.manualSyncNonblocking();

                setAccountAuthenticatorResult(result);
            } else {
                Log.e(Constants.TAG,
                        "Account was not added! result did not contain KEY_ACCOUNT_NAME!");
            }
        } else {
            Log.e(Constants.TAG, "Account was not added! result was null!");
        }

        finish();
    }
}
