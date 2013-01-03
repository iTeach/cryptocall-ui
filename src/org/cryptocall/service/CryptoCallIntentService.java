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

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;

import org.cryptocall.CryptoCallApplication;
import org.cryptocall.api.CryptoCallSession;
import org.cryptocall.util.Constants;
import org.cryptocall.util.CryptoCallSessionFactory;
import org.cryptocall.util.Log;
import org.cryptocall.util.NetworkUtils;
import org.cryptocall.util.PgpHelper;
import org.cryptocall.util.PgpToX509;
import org.cryptocall.util.PreferencesHelper;
import org.cryptocall.util.SmsHelper;
import org.spongycastle.bcpg.BCPGKey;
import org.spongycastle.bcpg.DSAPublicBCPGKey;
import org.spongycastle.bcpg.RSAPublicBCPGKey;
import org.spongycastle.openpgp.PGPPublicKeyRing;
import org.spongycastle.openpgp.PGPSecretKeyRing;
import org.thialfihar.android.apg.integration.ApgContentProviderHelper;
import org.thialfihar.android.apg.service.IApgKeyService;
import org.thialfihar.android.apg.service.handler.IApgGetKeyringsHandler;

import com.csipsimple.api.SipManager;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class CryptoCallIntentService extends IntentService {
    private CryptoCallApplication mApplication;
    private IApgKeyService mIApgKeyService;

    public static final int HANDLER_MSG_UPDATE_UI = 20001;
    public static final String HANDLER_DATA_MESSAGE = "message";

    /* extras that can be given by intent */
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_DATA = "data";

    /* possible EXTRA_ACTIONs */
    public static final int ACTION_START = 10;

    /* values for data bundle */
    public static final String DATA_SEND_SMS = "sendSms";
    public static final String DATA_CRYPTOCALL_EMAIL = "session";

    /* Return values */
    public static final int HANDLER_MSG_OKAY = 10001;

    // public static final String RESULT_X509_CERT = "X509cert";
    // public static final String RESULT_PUB_KEY_CONTENT = "pubKeyContent";
    // public static final String RESULT_PUB_KEY_TYPE = "pubKeyType";

    public static final int KEY_TYPE_RSA = 1;
    public static final int KEY_TYPE_DSA = 2;

    /* internal variables */
    private PGPPublicKeyRing mReceiverPublicKeyring;
    private PGPSecretKeyRing mMySecretKeyring;

    Messenger mMessenger;

    SmsHelper mSmsHelper;

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

    @Override
    public void onDestroy() {
        super.onDestroy();

        // mSmsHelper.unregisterReceivers(this);
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

        mSmsHelper = new SmsHelper(this);

        int action = extras.getInt(EXTRA_ACTION);

        // execute action from extra bundle
        switch (action) {
        case ACTION_START:
            Log.d(Constants.TAG, "ACTION_PUB_KEY_AND_CERT");

            try {
                /* Input */
                String email = data.getString(DATA_CRYPTOCALL_EMAIL);
                boolean sendSms = data.getBoolean(DATA_SEND_SMS);

                /* 0. Get corresponding telephoneNumber, name of receiver */
                CryptoCallSession session = CryptoCallSessionFactory
                        .generateSessionWithNameAndTelephoneNumber(this, email);

                String myIp = NetworkUtils.getLocalIpAddress(this);

                // TODO: choose from random?
                session.serverPort = 6666;

                // if we have a local ip address
                if (myIp != null) {

                    ApgContentProviderHelper apgContentProviderHelper = new ApgContentProviderHelper(
                            this);
                    long[] receiverKeyringIds = apgContentProviderHelper
                            .getPublicKeyringIdsByEmail(session.email);

                    // Get actual key!
                    if (receiverKeyringIds != null) {
                        long receiverKeringId = receiverKeyringIds[0];
                        long myKeyringId = PreferencesHelper.getPgpMasterKeyId(this);

                        if (mIApgKeyService != null) {

                            /* 1. get X509 certificate and pub key of receiver */

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
                                mIApgKeyService.getSecretKeyRings(new long[] { myKeyringId },
                                        false, getSecretKeyringHandler);
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
                            BCPGKey key = mReceiverPublicKeyring.getPublicKey()
                                    .getPublicKeyPacket().getKey();

                            BigInteger pubKeyContentBI = null;
                            if (key instanceof RSAPublicBCPGKey) {
                                pubKeyContentBI = ((RSAPublicBCPGKey) key).getModulus();
                                session.publicKeyType = KEY_TYPE_RSA;
                            } else if (key instanceof DSAPublicBCPGKey) {
                                pubKeyContentBI = ((DSAPublicBCPGKey) key).getY();
                                session.publicKeyType = KEY_TYPE_DSA;
                            }

                            session.publicKeyHex = pubKeyContentBI.toString(16);

                            Log.d(Constants.TAG, "pubKeyType: " + session.publicKeyType
                                    + "\npubKeyContent:\n" + session.publicKeyHex);

                            // TODO: for pgp keyrings with password
                            CallbackHandler pgpPwdCallbackHandler = new PgpToX509.PredefinedPasswordCallbackHandler(
                                    "");

                            PgpToX509.createFiles(this, mMySecretKeyring.getSecretKey(), "",
                                    pgpPwdCallbackHandler);

                            session.X509CertFile = (new File(getFilesDir(), PgpToX509.CERT_FILENAME))
                                    .getAbsolutePath();
                            Log.d(Constants.TAG, "session.X509CertFile: " + session.X509CertFile);

                            session.X509PrivKeyFile = (new File(getFilesDir(),
                                    PgpToX509.PRIV_KEY_FILENAME)).getAbsolutePath();
                            Log.d(Constants.TAG, "session.X509PrivKeyFile: "
                                    + session.X509PrivKeyFile);

                            /* 1. Open CSipSimple port with X509 certificate and pub key of receiver */
                            // start sip service to open port etc.
                            Intent it = new Intent(SipManager.INTENT_SIP_SERVICE);
                            it.putExtra(SipManager.EXTRA_CRYPTOCALL_SESSION, session);
                            startService(it);

                            if (sendSms) {
                                /* 2. Send SMS with my ip, port. TODO: sign? */
                                mSmsHelper.sendCryptoCallSms(this, session);
                            }

                        }

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

    public void sendUpdateUiToHandler(String message) {
        Message msg = Message.obtain();
        msg.what = HANDLER_MSG_UPDATE_UI;

        Bundle data = new Bundle();
        data.putString(HANDLER_DATA_MESSAGE, message);
        msg.setData(data);

        try {
            mMessenger.send(msg);
        } catch (RemoteException e) {
            Log.w(Constants.TAG, "Exception sending message, Is handler present?", e);
        } catch (NullPointerException e) {
            Log.w(Constants.TAG, "Messenger is null!", e);
        }
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
