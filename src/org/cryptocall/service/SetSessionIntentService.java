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

import org.cryptocall.CryptoCallSession;
import org.cryptocall.CurrentSessionSingelton;
import org.cryptocall.util.Constants;
import org.cryptocall.util.Log;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

/**
 * This does only one thing: Set CryptoCallSession in singelton CurrentSessionSingelton. It is
 * needed to set the singelton in the process :sipStack!
 * 
 */
public class SetSessionIntentService extends IntentService {
    /* extras that can be given by intent */
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_DATA = "data";

    /* possible EXTRA_ACTIONs */
    public static final int ACTION_SET_SESSION = 10;

    /* values for data bundle */
    public static final String DATA_CRYPTOCALL_SESSION = "session";

    public SetSessionIntentService() {
        super("SetSessionIntentService");
    }

    /**
     * The IntentService calls this method from the default worker thread with the intent that
     * started the service. When this method returns, IntentService stops the service, as
     * appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.e(Constants.TAG, "Extras bundle is null!");
            return;
        }

        if (!(extras.containsKey(EXTRA_DATA) || extras.containsKey(EXTRA_ACTION))) {
            Log.e(Constants.TAG, "Extra bundle must contain a data bundle and an action!");
            return;
        }

        Bundle data = extras.getBundle(EXTRA_DATA);

        int action = extras.getInt(EXTRA_ACTION);

        try {

            // execute action from extra bundle
            switch (action) {
            case ACTION_SET_SESSION:
                Log.d(Constants.TAG, "ACTION_SET_SESSION");

                /* Input */
                CryptoCallSession session = data.getParcelable(DATA_CRYPTOCALL_SESSION);

                CurrentSessionSingelton.getInstance().setCryptoCallSession(session);

                break;

            default:
                break;
            }

        } catch (Exception e) {
            Log.e(Constants.TAG, "Error in SetSessionIntentService", e);
        }

    }

}
