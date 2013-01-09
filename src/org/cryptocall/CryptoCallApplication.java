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

package org.cryptocall;

import java.security.Security;

import org.cryptocall.service.ApgKeyServiceConnection;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.thialfihar.android.apg.service.IApgKeyService;

import com.csipsimple.api.SipConfigManager;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class CryptoCallApplication extends Application {
    public final ApgKeyServiceConnection mApgKeyServiceConnection = new ApgKeyServiceConnection(
            this);

    static {
        // Define Java Security Provider to be Bouncy Castle
        Security.addProvider(new BouncyCastleProvider());
    }

    public IApgKeyService getApgKeyService() {
        return mApgKeyServiceConnection.getService();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Apg service
        mApgKeyServiceConnection.bindToApgKeyService();

        // Retrieve private preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        // boolean alreadySetup = prefs.getBoolean(SAMPLE_ALREADY_SETUP, false);
        // if(!alreadySetup) {
        // Activate debugging .. here can come various other options
        // One can also decide to reuse csipsimple activities to setup config
        SipConfigManager.setPreferenceStringValue(this, SipConfigManager.LOG_LEVEL, "5");

        // Enable usage for in and outgoing calls for following networks
        // TODO: Is anyways enough???
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_ANYWAY_IN, true);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_ANYWAY_OUT, true);

        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_3G_IN, true);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_3G_OUT, true);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_EDGE_IN, true);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_EDGE_OUT, true);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_GPRS_IN, true);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_GPRS_OUT, true);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_OTHER_IN, true);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_OTHER_OUT, true);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_WIFI_IN, true);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_WIFI_OUT, true);

        // deactivate icon in status on registration of sip account
        // SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.ICON_IN_STATUS_BAR_NBR,
        // false);
        // SipConfigManager
        // .setPreferenceBooleanValue(this, SipConfigManager.ICON_IN_STATUS_BAR, false);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.ICON_IN_STATUS_BAR_NBR,
                true);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.ICON_IN_STATUS_BAR, true);

        // ssl etc.
        SipConfigManager
                .setPreferenceStringValue(this, SipConfigManager.UDP_TRANSPORT_PORT, "5060");

        // other settings
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.ENABLE_DNS_SRV, false);

        // }

    }

    @Override
    public void onTerminate() {
        mApgKeyServiceConnection.unbindFromApgKeyService();
    }

}
