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

import java.math.BigInteger;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;

import org.cryptocall.CryptoCallApplication;
import org.cryptocall.util.Constants;
import org.cryptocall.util.Log;
import org.cryptocall.util.PgpHelper;
import org.cryptocall.util.PgpX509Bridge;
import org.cryptocall.util.PreferencesHelper;
import org.spongycastle.bcpg.BCPGKey;
import org.spongycastle.bcpg.DSAPublicBCPGKey;
import org.spongycastle.bcpg.RSAPublicBCPGKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.integration.ApgContentProviderHelper;
import org.thialfihar.android.apg.service.IApgKeyService;
import org.thialfihar.android.apg.service.handler.IApgGetKeyringsHandler;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class CryptoCallIntentService extends IntentService {
    private CryptoCallApplication mApplication;
    private IApgKeyService mIApgKeyService;

    /* extras that can be given by intent */
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_DATA = "data";

    /* possible EXTRA_ACTIONs */
    public static final int ACTION_PUB_KEY_AND_CERT = 10;

    /* values for data bunle */
    public static final String DATA_CRYPTOCALL_RECEIVER_EMAIL = "email";

    /* Return values */
    public static final int HANDLER_MSG_OKAY = 10001;

    public static final String RESULT_X509_CERT = "X509cert";
    public static final String RESULT_PUB_KEY_CONTENT = "pubKeyContent";
    public static final String RESULT_PUB_KEY_TYPE = "pubKeyType";

    public static final int RESULT_KEY_TYPE_RSA = 1;
    public static final int RESULT_KEY_TYPE_DSA = 2;

    /* internal variables */
    private PGPPublicKeyRing mReceiverPublicKeyring;
    private PGPSecretKeyRing mMySecretKeyring;

    Messenger mMessenger;

    public CryptoCallIntentService() {
        super("CryptoCallIntentService");
    }

    Object syncToken = new Object();;

    @Override
    public void onCreate() {
        super.onCreate();

        mApplication = (CryptoCallApplication) getApplication();
        mIApgKeyService = mApplication.getApgKeyService();
    }

    private final IApgGetKeyringsHandler.Stub getPublicKeyringHandler = new IApgGetKeyringsHandler.Stub() {

        @Override
        public void onException(final int exceptionId, final String message) throws RemoteException {
            Log.e(Constants.TAG, "Exception in ApgKeyService: " + message);
        }

        @Override
        public void onSuccess(final byte[] outputBytes, final List<String> outputStrings)
                throws RemoteException {
            Log.d(Constants.TAG, "getPublicKeyringHandler on success");

            mReceiverPublicKeyring = (PGPPublicKeyRing) PgpHelper.BytesToPGPKeyRing(outputBytes);

            // notify main thread that keyring was successfully retrieved
            synchronized (syncToken) {
                syncToken.notify();
            }
        }

    };

    private final IApgGetKeyringsHandler.Stub getSecretKeyringHandler = new IApgGetKeyringsHandler.Stub() {

        @Override
        public void onException(final int exceptionId, final String message) throws RemoteException {
            Log.e(Constants.TAG, "Exception in ApgKeyService: " + message);
        }

        @Override
        public void onSuccess(final byte[] outputBytes, final List<String> outputStrings)
                throws RemoteException {
            Log.d(Constants.TAG, "getPublicKeyringHandler on success");

            mMySecretKeyring = (PGPSecretKeyRing) PgpHelper.BytesToPGPKeyRing(outputBytes);

            // notify main thread that keyring was successfully retrieved
            synchronized (syncToken) {
                syncToken.notify();
            }
        }

    };

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

        if (!(extras.containsKey(EXTRA_MESSENGER) || extras.containsKey(EXTRA_DATA) || extras
                .containsKey(EXTRA_ACTION))) {
            Log.e(Constants.TAG,
                    "Extra bundle must contain a messenger, a data bundle, and an action!");
            return;
        }

        mMessenger = (Messenger) extras.get(EXTRA_MESSENGER);
        Bundle data = extras.getBundle(EXTRA_DATA);

        int action = extras.getInt(EXTRA_ACTION);

        // execute action from extra bundle
        switch (action) {
        case ACTION_PUB_KEY_AND_CERT:
            Log.d(Constants.TAG, "ACTION_PUB_KEY_AND_CERT");
            try {
                /* Input */
                String email = data.getString(DATA_CRYPTOCALL_RECEIVER_EMAIL);

                ApgContentProviderHelper apgContentProviderHelper = new ApgContentProviderHelper(
                        this);
                long[] receiverKeyringIds = apgContentProviderHelper
                        .getPublicKeyringIdsByEmail(email);

                // Get actual key!
                if (receiverKeyringIds != null) {
                    long receiverKeringId = receiverKeyringIds[0];
                    long myKeyringId = PreferencesHelper.getPgpMasterKeyId(this);

                    if (mIApgKeyService != null) {

                        /* Get public key of receiver */
                        try {
                            mIApgKeyService.getPublicKeyRings(new long[] { receiverKeringId },
                                    false, getPublicKeyringHandler);
                        } catch (RemoteException e) {
                            Log.e(Constants.TAG, "RemoteException", e);
                        }

                        // wait for asynchronous getPublicKeyringHandler
                        synchronized (syncToken) {
                            try {
                                syncToken.wait();
                            } catch (InterruptedException e) {
                                Log.e(Constants.TAG, "InterruptedException", e);
                            }
                        }
                        Log.d(Constants.TAG, "After sync token");

                        /* get my secret keyring */
                        try {
                            mIApgKeyService.getSecretKeyRings(new long[] { myKeyringId }, false,
                                    getSecretKeyringHandler);
                        } catch (RemoteException e) {
                            Log.e(Constants.TAG, "RemoteException", e);
                        }

                        // wait for asynchronous getSecretKeyringHandler
                        synchronized (syncToken) {
                            try {
                                syncToken.wait();
                            } catch (InterruptedException e) {
                                Log.e(Constants.TAG, "InterruptedException", e);
                            }
                        }
                        Log.d(Constants.TAG, "After sync token 2");

                        /* Get actual pub key values */
                        BCPGKey key = mReceiverPublicKeyring.getPublicKey().getPublicKeyPacket()
                                .getKey();

                        BigInteger pubKeyContentBI = null;
                        int pubKeyType = -1;
                        if (key instanceof RSAPublicBCPGKey) {
                            pubKeyContentBI = ((RSAPublicBCPGKey) key).getModulus();
                            pubKeyType = RESULT_KEY_TYPE_RSA;
                        } else if (key instanceof DSAPublicBCPGKey) {
                            pubKeyContentBI = ((DSAPublicBCPGKey) key).getY();
                            pubKeyType = RESULT_KEY_TYPE_DSA;
                        }

                        String pubKeyContent = pubKeyContentBI.toString(16);

                        Log.d(Constants.TAG, "pubKeyType: " + pubKeyType + "\npubKeyContent:\n"
                                + pubKeyContent);

                        // TODO: for pgp keyrings with password
                        CallbackHandler pgpPwdCallbackHandler = new PgpX509Bridge.PredefinedPasswordCallbackHandler(
                                "");

                        byte[] x509cert = PgpX509Bridge
                                .createAndSaveSelfSignedCertInPKCS12AsByteArray(
                                        mMySecretKeyring.getSecretKey(), "", pgpPwdCallbackHandler);

                        Bundle resultData = new Bundle();
                        resultData.putString(RESULT_PUB_KEY_CONTENT, pubKeyContent);
                        resultData.putInt(RESULT_PUB_KEY_TYPE, pubKeyType);
                        resultData.putByteArray(RESULT_X509_CERT, x509cert);

                        // send results back to activity
                        sendMessageToHandler(HANDLER_MSG_OKAY, resultData);
                    } else {
                        Log.e(Constants.TAG, "mIApgKeyService is null!");
                    }
                } else {
                    Log.e(Constants.TAG, "public keyring not found for this email!");
                }

            } catch (Exception e) {
                sendErrorToHandler(e);
            }

            break;

        default:
            break;
        }

    }

    private void sendErrorToHandler(Exception e) {
        Log.e(Constants.TAG, "ApgService Exception: ", e);
        e.printStackTrace();

        // Bundle data = new Bundle();
        // data.putString(ApgIntentServiceHandler.DATA_ERROR, e.getMessage());
        // sendMessageToHandler(ApgIntentServiceHandler.MESSAGE_EXCEPTION, null, data);
    }

    private void sendMessageToHandler(Integer arg1, Integer arg2, Bundle data) {
        Message msg = Message.obtain();
        msg.arg1 = arg1;
        if (arg2 != null) {
            msg.arg2 = arg2;
        }
        if (data != null) {
            msg.setData(data);
        }

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
    }

    private void sendMessageToHandler(Integer arg1, Bundle data) {
        sendMessageToHandler(arg1, null, data);
    }

    private void sendMessageToHandler(Integer arg1) {
        sendMessageToHandler(arg1, null, null);
    }
}
