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

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

public class AccountHelper {
    Context mContext;
    Handler mBackgroundStatusHandler;

    public AccountHelper(Context context, Handler handler) {
        mContext = context;
        mBackgroundStatusHandler = handler;
    }

    public AccountHelper(Context context) {
        mContext = context;
    }

    /**
     * Add account for Birthday Adapter to Android system
     * 
     * @param context
     * @return
     */
    public Bundle addAccount() {
        Log.d(Constants.TAG, "Adding account...");

        ContentResolver.setSyncAutomatically(Constants.SYNC_ACCOUNT,
                Constants.SYNC_CONTENT_AUTHORITY, true);
        ContentResolver.setIsSyncable(Constants.SYNC_ACCOUNT, Constants.SYNC_ACCOUNT_TYPE, 0);

        /*
         * Add periodic sync interval once per day
         * 
         * Currently disabled as we sync on new keyrings on changes in OpenPGP Keychains database in
         * KeychainDatabaseChangeReceiver
         */
        // long freq = AlarmManager.INTERVAL_DAY;
        // ContentResolver.addPeriodicSync(Constants.ACCOUNT, Constants.ACCOUNT_TYPE, new
        // Bundle(),
        // freq);

        AccountManager am = AccountManager.get(mContext);
        if (am.addAccountExplicitly(Constants.SYNC_ACCOUNT, null, null)) {
            Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ACCOUNT_NAME, Constants.SYNC_ACCOUNT.name);
            result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.SYNC_ACCOUNT.type);
            return result;
        } else {
            return null;
        }
    }

    /**
     * Adds account and forces manual sync afterwards if adding was successful
     */
    public void addAccountAndSyncBlocking() {
        Bundle result = addAccount();

        if (result != null) {
            if (result.containsKey(AccountManager.KEY_ACCOUNT_NAME)) {
                // Force a sync! Even when background sync is disabled, this will force one sync!
                manualSyncBlocking();
            } else {
                Log.e(Constants.TAG,
                        "Account was not added! result did not contain KEY_ACCOUNT_NAME!");
            }
        } else {
            Log.e(Constants.TAG, "Account was not added! result was null!");
        }
    }

    /**
     * Remove account from Android system
     * 
     * @param context
     * @return
     */
    public boolean removeAccount() {
        Log.d(Constants.TAG, "Removing account...");

        AccountManager am = AccountManager.get(mContext);

        // remove account
        AccountManagerFuture<Boolean> future = am.removeAccount(Constants.SYNC_ACCOUNT, null, null);
        if (future.isDone()) {
            try {
                future.getResult();

                return true;
            } catch (Exception e) {
                Log.e(Constants.TAG, "Problem while removing account!", e);
                return false;
            }
        } else {
            return false;
        }
    }

    /**
     * Force a manual sync now!
     */
    public void manualSyncBlocking() {
        Log.d(Constants.TAG, "Force manual sync...");

        ContactsSyncAdapterService.performSync(mContext, Constants.SYNC_ACCOUNT);
    }

    /**
     * Force manual sync in a new thread
     */
    public void manualSyncNonblocking() {
        Log.d(Constants.TAG, "Force manual sync nonblocking...");

        Runnable r = new Runnable() {
            public void run() {
                ContactsSyncAdapterService.performSync(mContext, Constants.SYNC_ACCOUNT);
            }
        };
        Thread thread = new Thread(r);

        thread.start();

        // Disabled: Force resync in Android OS
        // Bundle extras = new Bundle();
        // extras.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        // ContentResolver.requestSync(Constants.SYNC_ACCOUNT, Constants.SYNC_CONTENT_AUTHORITY,
        // extras);
    }

    /**
     * Checks whether the account is enabled or not
     * 
     * @param context
     * @return
     */
    public boolean isAccountActivated() {
        AccountManager am = AccountManager.get(mContext);

        Account[] availableAccounts = am.getAccountsByType(Constants.SYNC_ACCOUNT_TYPE);
        for (Account currentAccount : availableAccounts) {
            if (currentAccount.name.equals(Constants.SYNC_ACCOUNT_NAME)) {
                return true;
            }
        }

        return false;
    }
}
