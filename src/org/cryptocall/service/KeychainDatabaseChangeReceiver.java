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

package org.cryptocall.service;

import org.cryptocall.syncadapter.AccountHelper;
import org.cryptocall.util.Constants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

public class KeychainDatabaseChangeReceiver extends BroadcastReceiver {

    private static final String EXTRA_BROADCAST_KEY_TYPE = "keyType";
    private static final int KEY_TYPE_PUBLIC = 0;

    private static final String EXTRA_BROADCAST_CONTENT_ITEM_TYPE = "contentItemType";
    private static final String ITEM_TYPE_KEYRING = "vnd.android.cursor.item/vnd.thialfihar.apg.key_ring";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(Constants.TAG, "Keychain database changed!");

        Bundle extras = intent.getExtras();
        if (extras != null) {
            int keyType = extras.getInt(EXTRA_BROADCAST_KEY_TYPE);
            String contentItemType = extras.getString(EXTRA_BROADCAST_CONTENT_ITEM_TYPE);

            if (KEY_TYPE_PUBLIC == keyType && ITEM_TYPE_KEYRING.equals(contentItemType)) {
                Log.d(Constants.TAG, "Public keyring changed!");
                (new AccountHelper(context)).manualSyncNonblocking();
            }
        }
    }
}