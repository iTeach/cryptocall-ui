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

import org.cryptocall.service.KeychainKeyServiceConnection;
import org.cryptocall.util.Constants;
import org.spongycastle.jce.provider.BouncyCastleProvider;
import org.sufficientlysecure.keychain.service.IKeychainKeyService;

import com.csipsimple.api.SipConfigManager;

import android.app.Application;

public class CryptoCallApplication extends Application {
    public final KeychainKeyServiceConnection mKeychainKeyServiceConnection = new KeychainKeyServiceConnection(
            this);

    static {
        // Define Java Security Provider to be Bouncy Castle
        Security.addProvider(new BouncyCastleProvider());
    }

    public IKeychainKeyService getKeychainKeyService() {
        return mKeychainKeyServiceConnection.getService();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        // Bind to Keychain service
        mKeychainKeyServiceConnection.bindToKeychainKeyService();

        /* CSipSimple preferences */

        // Activate debugging in CSipSimple if enabled in CryptoCall
        if (Constants.DEBUG) {
            SipConfigManager.setPreferenceStringValue(this, SipConfigManager.LOG_LEVEL, "5");
        }

        // set up codecs
        SipConfigManager.setPreferenceStringValue(this,
                SipConfigManager.getCodecKey("opus/48000/1", SipConfigManager.CODEC_NB), "249");
        SipConfigManager.setPreferenceStringValue(this,
                SipConfigManager.getCodecKey("PCMU/8000/1", SipConfigManager.CODEC_NB), "248");
        SipConfigManager.setPreferenceStringValue(this,
                SipConfigManager.getCodecKey("PCMA/8000/1", SipConfigManager.CODEC_NB), "247");
        SipConfigManager.setPreferenceStringValue(this,
                SipConfigManager.getCodecKey("SILK/24000/1", SipConfigManager.CODEC_NB), "246");
        SipConfigManager.setPreferenceStringValue(this,
                SipConfigManager.getCodecKey("opus/48000/1", SipConfigManager.CODEC_WB), "249");
        SipConfigManager.setPreferenceStringValue(this,
                SipConfigManager.getCodecKey("PCMU/8000/1", SipConfigManager.CODEC_WB), "248");
        SipConfigManager.setPreferenceStringValue(this,
                SipConfigManager.getCodecKey("PCMA/8000/1", SipConfigManager.CODEC_WB), "247");
        SipConfigManager.setPreferenceStringValue(this,
                SipConfigManager.getCodecKey("SILK/24000/1", SipConfigManager.CODEC_WB), "246");

        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.USE_VIDEO, true);

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
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.ICON_IN_STATUS_BAR_NBR,
                false);
        SipConfigManager
                .setPreferenceBooleanValue(this, SipConfigManager.ICON_IN_STATUS_BAR, false);

        // other settings
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.ENABLE_DNS_SRV, false);

        // disable integrations
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.INTEGRATE_TEL_PRIVILEGED,
                false);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.INTEGRATE_WITH_CALLLOGS,
                false);
        SipConfigManager.setPreferenceBooleanValue(this, SipConfigManager.INTEGRATE_WITH_DIALER,
                false);

        // only enable pause/resume of music
        SipConfigManager.setPreferenceBooleanValue(this,
                SipConfigManager.INTEGRATE_WITH_NATIVE_MUSIC, true);
    }

    @Override
    public void onTerminate() {
        mKeychainKeyServiceConnection.unbindFromKeychainKeyService();
    }

}
