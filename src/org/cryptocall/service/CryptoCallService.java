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

import java.util.ArrayList;
import java.util.List;

import org.cryptocall.service.handler.ICryptoCallGetPublicKeyHandler;
import org.cryptocall.util.Constants;
import org.cryptocall.util.Log;
import org.cryptocall.util.PGPHelper;
import org.cryptocall.util.ProtectedEmailUtils;
import org.spongycastle.bcpg.BCPGKey;
import org.spongycastle.bcpg.PublicKeyPacket;
import org.spongycastle.openpgp.PGPPublicKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.util.encoders.Base64;
import org.spongycastle.util.encoders.Hex;
import org.thialfihar.android.apg.integration.ApgContentProviderHelper;
import org.thialfihar.android.apg.integration.ApgUtil;
import org.thialfihar.android.apg.service.IApgKeyService;
import org.thialfihar.android.apg.service.handler.IApgGetKeyringsHandler;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.os.RemoteException;

public class CryptoCallService extends Service {
    private Service mService;

    private IApgKeyService mApgKeyService;
    private boolean mApgKeyServiceBound;

    /**
     * Connection to APGs KeyService to get real OpenPGP keys
     */
    private ServiceConnection mApgKeyConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
            mApgKeyService = IApgKeyService.Stub.asInterface(service);
            Log.d(Constants.TAG, "CryptoCallService: connected to ApgKeyService");
            mApgKeyServiceBound = true;
        }

        public void onServiceDisconnected(ComponentName name) {
            mApgKeyService = null;
            Log.d(Constants.TAG, "CryptoCallService: disconnected from ApgKeyService");
            mApgKeyServiceBound = false;
        }
    };

    /**
     * Bind this service to APGKeyService
     * 
     * @return
     */
    private boolean bindToApgKeyService() {
        if (mApgKeyService == null && !mApgKeyServiceBound) { // if not already connected
            try {
                Log.d(Constants.TAG, "CryptoCallService: bindToIApgKeyService: not bound yet");
                mService.bindService(new Intent(IApgKeyService.class.getName()), mApgKeyConnection,
                        Context.BIND_AUTO_CREATE);

                return true;
            } catch (Exception e) {
                Log.d(Constants.TAG, "CryptoCallService: bindToIApgKeyService Exception", e);
                return false;
            }
        } else { // already connected
            Log.d(Constants.TAG, "CryptoCallService: bindToApgService: already bound... ");
            return true;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mService = this;

        Log.d(Constants.TAG, "CryptoCallService: onCreate()");

        bindToApgKeyService();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(Constants.TAG, "CryptoCallService: onDestroy()");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public void getPublicKeyImplementation(String telephoneNumber,
            ICryptoCallGetPublicKeyHandler handler) {
        Long masterKeyId = null;

        // go through all APG emails with @cryptocall in it and find the one for this
        // telephoneNumber
        try {
            Uri contentUri = Uri.withAppendedPath(
                    ApgContentProviderHelper.CONTENT_URI_PUBLIC_KEY_RING_BY_LIKE_EMAIL,
                    Constants.CRYPTOCALL_DOMAIN);
            Cursor c = mService.getContentResolver().query(contentUri,
                    new String[] { "master_key_id", "user_id" }, null, null, null);

            while (c != null && c.moveToNext()) {
                String cUserId = c.getString(1);
                String[] cSplit = ApgUtil.splitUserId(cUserId);
                String cEmail = cSplit[1];
                byte[] cEmailSalt;
                try {
                    cEmailSalt = ProtectedEmailUtils.getSaltFromProtectedEmail(cEmail);

                    String generatedEmail = ProtectedEmailUtils.generateProtectedEmail(
                            telephoneNumber, cEmailSalt);

                    if (generatedEmail.equals(cEmail)) {
                        Log.d(Constants.TAG, "Found email! cEmail: " + cEmail + " for telNumber "
                                + telephoneNumber);
                        masterKeyId = c.getLong(0);
                        break;
                    }
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Exception", e);
                }

            }

            if (c != null) {
                c.close();
            }
        } catch (SecurityException e) {
            Log.e(Constants.TAG, "insufficient permissions to use apg service!");
        }

        // if masterKeyId was found, get actual key!
        if (masterKeyId != null) {
            try {
                // The return values are handled in MyGetKeyringsHandler, which also gets
                // ICryptoCallGetTrustedPublicKeysHandler
                mApgKeyService.getPublicKeyRings(new long[] { masterKeyId }, false,
                        new MyGetKeyringsHandler(handler));
            } catch (RemoteException e) {
                Log.e(Constants.TAG, "RemoteException", e);
            }
        } else {
            Log.e(Constants.TAG, "cryptocall email to this telephoneNumber was not found!");
        }

    }

    class MyGetKeyringsHandler extends IApgGetKeyringsHandler.Stub {
        ICryptoCallGetPublicKeyHandler getTrustedPublicKeysHandler;

        public MyGetKeyringsHandler(ICryptoCallGetPublicKeyHandler getTrustedPublicKeysHandler) {
            super();
            this.getTrustedPublicKeysHandler = getTrustedPublicKeysHandler;
        }

        @Override
        public void onException(final int exceptionId, final String message) throws RemoteException {
            // give exception to next handler
            getTrustedPublicKeysHandler.onException(exceptionId, message);
        }

        @Override
        public void onSuccess(final byte[] outputBytes, final List<String> publicKeys)
                throws RemoteException {
            Log.d(Constants.TAG, "Got keyRing bytes. Trying to get hex encoded pub key...");

            PGPPublicKeyRing keyRing = (PGPPublicKeyRing) PGPHelper.BytesToPGPKeyRing(outputBytes);
            PGPPublicKey key = keyRing.getPublicKey();

            PublicKeyPacket packet = key.getPublicKeyPacket();
            BCPGKey actKey = packet.getKey();

            String keyHex = new String(Hex.encode(actKey.getEncoded()));

            Log.d(Constants.TAG, "keyHex: " + keyHex);

            getTrustedPublicKeysHandler.onSuccess(keyHex);
        }
    }

    /**
     * This is the implementation of the interface ICryptoCallService. All methods are oneway,
     * meaning asynchronous and return to the client using handlers.
     */
    private final ICryptoCallService.Stub mBinder = new ICryptoCallService.Stub() {

        @Override
        public void getPublicKey(String telephoneNumber, ICryptoCallGetPublicKeyHandler handler)
                throws RemoteException {
            getPublicKeyImplementation(telephoneNumber, handler);
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
