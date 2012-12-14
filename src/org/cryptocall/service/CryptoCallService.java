/*
 * Copyright (C) 2012 Sergej Dechand <cryptocall@serj.de>
 *                    Dominik Schürmann <dominik@dominikschuermann.de>
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

import org.cryptocall.service.handler.ICryptoCallGetTrustedPublicKeysHandler;
import org.cryptocall.util.Constants;
import org.cryptocall.util.Log;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;

/**
 * TODO:
 * 
 * - is this service thread safe? Probably not!
 * 
 */
public class CryptoCallService extends Service {
    Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
        Log.d(Constants.TAG, "ApgKeyService, onCreate()");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "ApgKeyService, onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /**
     * This is the implementation of the interface IApgService. All methods are oneway, meaning
     * asynchronous and return to the client using IApgHandler.
     * 
     * The real PGP code is located in PGPMain.
     */
    private final ICryptoCallService.Stub mBinder = new ICryptoCallService.Stub() {

        @Override
        public void getTrustedPublicKeys(ICryptoCallGetTrustedPublicKeysHandler handler)
                throws RemoteException {
            // TODO Auto-generated method stub

        }

    };

    /**
     * As we can not throw an exception through Android RPC, we assign identifiers to the exception
     * types.
     * 
     * @param e
     * @return
     */
    // private int getExceptionId(Exception e) {
    // if (e instanceof NoSuchProviderException) {
    // return 0;
    // } else if (e instanceof NoSuchAlgorithmException) {
    // return 1;
    // } else if (e instanceof SignatureException) {
    // return 2;
    // } else if (e instanceof IOException) {
    // return 3;
    // } else if (e instanceof ApgGeneralException) {
    // return 4;
    // } else if (e instanceof PGPException) {
    // return 5;
    // } else {
    // return -1;
    // }
    // }

}
