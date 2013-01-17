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
import org.sufficientlysecure.keychain.service.IKeychainKeyService;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.util.Log;

public class KeychainKeyServiceConnection {
    private Application mApplication;

    private IKeychainKeyService mService;
    private boolean bound;

    public KeychainKeyServiceConnection(Application application) {
        mApplication = application;
    }

    public IKeychainKeyService getService() {
        return mService;
    }

    private ServiceConnection mKeychainKeyConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mService = IKeychainKeyService.Stub.asInterface(service);
            Log.d(Constants.TAG, "KeychainKeyServiceConnection: connected to service");
            bound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            Log.d(Constants.TAG, "KeychainKeyServiceConnection: disconnected from service");
            bound = false;
        }
    };

    /**
     * If not already bound, bind!
     * 
     * @return
     */
    public boolean bindToKeychainKeyService() {
        if (mService == null && !bound) { // if not already connected
            try {
                Log.d(Constants.TAG, "KeychainKeyServiceConnection: bindToIKeychainKeyService: not bound yet");
                mApplication.bindService(new Intent(IKeychainKeyService.class.getName()),
                        mKeychainKeyConnection, Context.BIND_AUTO_CREATE);

                return true;
            } catch (Exception e) {
                Log.d(Constants.TAG, "KeychainKeyServiceConnection Exception", e);
                return false;
            }
        } else { // already connected
            Log.d(Constants.TAG, "KeychainKeyServiceConnection: bindToKeychainService: already bound... ");
            return true;
        }
    }

    public void unbindFromKeychainKeyService() {
        mApplication.unbindService(mKeychainKeyConnection);
    }

}
