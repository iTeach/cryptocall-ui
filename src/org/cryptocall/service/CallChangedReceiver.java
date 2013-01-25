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

import org.cryptocall.util.Constants;
import org.cryptocall.util.Log;

import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * This receiver is called if a current SIP call is started, stopped, resumed, etc.
 */
public class CallChangedReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // get call session
        SipCallSession sipCallSession = intent.getExtras()
                .getParcelable(SipManager.EXTRA_CALL_INFO);

        Log.d(Constants.TAG, "Call changed to state " + sipCallSession.getCallState());

        // delegate action to intent service
        Intent serviceIntent = new Intent(context, CryptoCallIntentService.class);
        serviceIntent.putExtra(CryptoCallIntentService.EXTRA_ACTION,
                CryptoCallIntentService.ACTION_CALL_STATE_CHANGED);
        Bundle data = new Bundle();
        data.putParcelable(CryptoCallIntentService.DATA_SIP_CALL_SESSION, sipCallSession);
        serviceIntent.putExtra(CryptoCallIntentService.EXTRA_DATA, data);
        context.startService(serviceIntent);
    }
}
