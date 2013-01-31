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
import java.math.BigInteger;
import java.util.List;

import javax.security.auth.callback.CallbackHandler;

import org.cryptocall.CryptoCallApplication;
import org.cryptocall.CryptoCallSession;
import org.cryptocall.R;
import org.cryptocall.util.Constants;
import org.cryptocall.util.CryptoCallSessionUtils;
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
import org.sufficientlysecure.keychain.integration.KeychainContentProviderHelper;
import org.sufficientlysecure.keychain.service.IKeychainKeyService;
import org.sufficientlysecure.keychain.service.handler.IKeychainGetKeyringsHandler;

import com.csipsimple.api.SipCallSession;
import com.csipsimple.api.SipConfigManager;
import com.csipsimple.api.SipManager;
import com.csipsimple.api.SipProfile;
import com.csipsimple.api.SipUri;
import com.csipsimple.utils.PreferencesProviderWrapper;

import android.app.IntentService;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;

public class CryptoCallIntentService extends IntentService {
    private CryptoCallApplication mApplication;
    private IKeychainKeyService mIKeychainKeyService;

    public static final String ACTION_CALL = "org.cryptocall.action.CALL";
    public static final int HANDLER_MSG_UPDATE_UI = 21;
    public static final int HANDLER_MSG_RETURN_SESSION = 22;

    public static final String HANDLER_DATA_MESSAGE = "message";
    public static final String HANDLER_DATA_PROGRESS = "progress";

    /* extras that can be given by intent */
    public static final String EXTRA_MESSENGER = "messenger";
    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_DATA = "data";

    /* possible EXTRA_ACTIONs */
    public static final int ACTION_START_SENDING = 10;
    public static final int ACTION_START_RECEIVED = 20;
    public static final int ACTION_START_PARSE_SMS = 30;
    public static final int ACTION_CALL_STATE_CHANGED = 40;
    public static final int ACTION_STOP_SIP_STACK = 50;

    /* values for data bundle */
    // ACTION_START_SENDING and ACTION_START_RECEIVED
    public static final String DATA_CRYPTOCALL_SESSION = "session";
    // ACTION_START_SENDING
    public static final String DATA_SEND_SMS = "sendSms";
    // ACTION_START_PARSE_SMS
    public static final String DATA_SMS_BODY = "smsBody";
    public static final String DATA_SMS_FROM = "smsFrom";
    // ACTION_STOP_EVERYTHING
    public static final String DATA_SIP_CALL_SESSION = "sipCallSession";

    /* Return values */
    public static final int HANDLER_MSG_OKAY = 10001;

    /* internal variables */
    private PGPPublicKeyRing mPeerPublicKeyring;
    private PGPSecretKeyRing mMySecretKeyring;
    private SipProfile mSipProfile;
    private long mAccId;

    Messenger mMessenger;
    SmsHelper mSmsHelper;

    public CryptoCallIntentService() {
        super("CryptoCallIntentService");
    }

    Object syncToken = new Object();

    @Override
    public void onDestroy() {
        super.onDestroy();

        // mSmsHelper.unregisterReceivers(this);
    }

    private final IKeychainGetKeyringsHandler.Stub getPeerPublicKeyringHandler = new IKeychainGetKeyringsHandler.Stub() {

        @Override
        public void onException(final int exceptionId, final String message) throws RemoteException {
            Log.e(Constants.TAG, "Exception in KeychainKeyService: " + message);
        }

        @Override
        public void onSuccess(final byte[] outputBytes, final List<String> outputStrings)
                throws RemoteException {
            Log.d(Constants.TAG, "getPeerPublicKeyringHandler on success");

            mPeerPublicKeyring = (PGPPublicKeyRing) PgpHelper.BytesToPGPKeyRing(outputBytes);

            // notify main thread that keyring was successfully retrieved
            synchronized (syncToken) {
                syncToken.notify();
            }
        }

    };

    private final IKeychainGetKeyringsHandler.Stub getMySecretKeyringHandler = new IKeychainGetKeyringsHandler.Stub() {

        @Override
        public void onException(final int exceptionId, final String message) throws RemoteException {
            Log.e(Constants.TAG, "Exception in KeychainKeyService: " + message);
        }

        @Override
        public void onSuccess(final byte[] outputBytes, final List<String> outputStrings)
                throws RemoteException {
            Log.d(Constants.TAG, "getMySecretKeyringHandler on success");

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
        mApplication = (CryptoCallApplication) getApplication();
        mIKeychainKeyService = mApplication.getKeychainKeyService();

        mApplication = (CryptoCallApplication) getApplication();
        mIKeychainKeyService = mApplication.getKeychainKeyService();

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

        if (action == ACTION_START_RECEIVED || action == ACTION_START_SENDING) {

            try {
                /* Input */
                CryptoCallSession session = data.getParcelable(DATA_CRYPTOCALL_SESSION);

                /* 0. Get corresponding telephoneNumber, name of receiver */
                session = CryptoCallSessionUtils.getNameAndTelephoneNumberFromEmail(this, session);

                String myIp = NetworkUtils.getLocalIpAddress(this);

                // if we have a local ip address
                if (myIp != null) {
                    KeychainContentProviderHelper KeychainContentProviderHelper = new KeychainContentProviderHelper(
                            this);
                    long[] peerKeyringIds = KeychainContentProviderHelper
                            .getPublicKeyringIdsByEmail(session.peerEmail);

                    // Get actual key!
                    if (peerKeyringIds != null) {
                        long peerKeringId = peerKeyringIds[0];
                        long myKeyringId = PreferencesHelper.getPgpMasterKeyId(this);

                        if (mIKeychainKeyService != null) {

                            /* Get PGP public keyring of peer */
                            sendUpdateUiToHandler(R.string.status_get_peer_pub_keyring, 10);
                            try {
                                mIKeychainKeyService.getPublicKeyRings(new long[] { peerKeringId },
                                        false, getPeerPublicKeyringHandler);
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

                            Log.d(Constants.TAG, "Before Thread.sleep(3000);");
                            Thread.sleep(3000);
                            Log.d(Constants.TAG, "After Thread.sleep(3000);");

                            /* get my PGP secret keyring */
                            sendUpdateUiToHandler(R.string.status_get_my_secret_keyring, 20);
                            try {
                                mIKeychainKeyService.getSecretKeyRings(new long[] { myKeyringId },
                                        false, getMySecretKeyringHandler);
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
                            sendUpdateUiToHandler(R.string.status_create_session, 30);

                            BCPGKey key = mPeerPublicKeyring.getPublicKey().getPublicKeyPacket()
                                    .getKey();

                            BigInteger pubKeyContentBI = null;
                            if (key instanceof RSAPublicBCPGKey) {
                                pubKeyContentBI = ((RSAPublicBCPGKey) key).getModulus();
                                session.peerPublicKeyType = CryptoCallSession.KEY_TYPE_RSA;
                            } else if (key instanceof DSAPublicBCPGKey) {
                                pubKeyContentBI = ((DSAPublicBCPGKey) key).getY();
                                session.peerPublicKeyType = CryptoCallSession.KEY_TYPE_DSA;
                            }

                            session.peerPublicKeyHex = pubKeyContentBI.toString(16);

                            Log.d(Constants.TAG, "pubKeyType: " + session.peerPublicKeyType
                                    + "\npubKeyContent:\n" + session.peerPublicKeyHex);

                            // TODO: for pgp keyrings with password
                            CallbackHandler pgpPwdCallbackHandler = new PgpToX509.PredefinedPasswordCallbackHandler(
                                    "");

                            PgpToX509.createFiles(this, mMySecretKeyring.getSecretKey(), "",
                                    pgpPwdCallbackHandler);

                            session.myX509CertFile = (new File(getFilesDir(),
                                    PgpToX509.CERT_FILENAME)).getAbsolutePath();
                            Log.d(Constants.TAG, "session.myX509CertFile: "
                                    + session.myX509CertFile);

                            session.myX509PrivKeyFile = (new File(getFilesDir(),
                                    PgpToX509.PRIV_KEY_FILENAME)).getAbsolutePath();
                            Log.d(Constants.TAG, "session.myX509PrivKeyFile: "
                                    + session.myX509PrivKeyFile);

                            // execute action from extra bundle
                            switch (action) {
                            case ACTION_START_SENDING:
                                Log.d(Constants.TAG, "ACTION_START_SENDING");
                                boolean sendSms = data.getBoolean(DATA_SEND_SMS);

                                // set ip
                                session.serverIp = myIp;
                                // TODO: choose from random?
                                session.serverPort = 6666;

                                sendUpdateUiToHandler(R.string.status_set_session, 40);
                                setSessionInSingelton(session);

                                Log.d(Constants.TAG, "1Before Thread.sleep(1000);");
                                Thread.sleep(1000);
                                Log.d(Constants.TAG, "1After Thread.sleep(1000);");

                                sendUpdateUiToHandler(R.string.status_start_sip, 60);
                                createAccount();
                                startSipStack();

                                Log.d(Constants.TAG, "2Before Thread.sleep(3000);");
                                Thread.sleep(3000);
                                Log.d(Constants.TAG, "2After Thread.sleep(3000);");

                                if (sendSms) {
                                    sendUpdateUiToHandler(R.string.status_sending, 80);

                                    /* 2. Send SMS with my ip, port. TODO: sign? */
                                    mSmsHelper.sendCryptoCallSms(this, session);
                                }

                                break;

                            case ACTION_START_RECEIVED:
                                Log.d(Constants.TAG, "ACTION_START_RECEIVED");

                                sendUpdateUiToHandler(R.string.status_set_session, 40);
                                setSessionInSingelton(session);

                                Log.d(Constants.TAG, "1Before Thread.sleep(1000);");
                                Thread.sleep(3000);
                                Log.d(Constants.TAG, "1After Thread.sleep(1000);");

                                sendUpdateUiToHandler(R.string.status_start_sip, 70);
                                createAccount();
                                startSipStack();

                                Log.d(Constants.TAG, "2Before Thread.sleep(3000);");
                                Thread.sleep(3000);
                                Log.d(Constants.TAG, "2After Thread.sleep(3000);");

                                sendUpdateUiToHandler(R.string.status_start_call, 100);
                                makeCall(session);

                                break;

                            default:
                                break;
                            }
                        } else {
                            Log.e(Constants.TAG, "mIKeychainKeyService is null!");
                        }
                    } else {
                        Log.e(Constants.TAG,
                                "Public keyring not found in OpenPGP Keychain for this email!");
                    }
                } else {
                    Log.e(Constants.TAG, "No IP!");
                }

            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        } else {
            try {
                // execute action from extra bundle
                switch (action) {
                case ACTION_START_PARSE_SMS:
                    Log.d(Constants.TAG, "ACTION_START_PARSE_SMS");

                    String body = data.getString(DATA_SMS_BODY);
                    String from = data.getString(DATA_SMS_FROM);

                    CryptoCallSession newSession = new CryptoCallSession();
                    try {
                        newSession = CryptoCallSessionUtils.getIpPortAndTelephoneNumberFromSms(
                                newSession, body, from);
                        newSession = CryptoCallSessionUtils.getEmailAndNameFromTelephoneNumber(
                                this, newSession);

                        // return session
                        Bundle resultData = new Bundle();
                        resultData.putParcelable(DATA_CRYPTOCALL_SESSION, newSession);
                        sendMessageToHandler(HANDLER_MSG_RETURN_SESSION, resultData);
                    } catch (CryptoCallSessionUtils.SmsParsingFailedException e) {
                        sendUpdateUiToHandler(R.string.status_problem_parsing_sms);
                    } catch (CryptoCallSessionUtils.EmailNotFoundException e) {
                        sendUpdateUiToHandler(R.string.status_email_not_found);
                    }

                    break;

                case ACTION_CALL_STATE_CHANGED:
                    Log.d(Constants.TAG, "ACTION_CALL_STATE_CHANGED");
                    SipCallSession sipCallSession = data.getParcelable(DATA_SIP_CALL_SESSION);

                    if (sipCallSession.isAfterEnded()) {
                        Log.d(Constants.TAG, "Stop Sip stack!");
                        stopSipStack();
                    }

                    break;

                case ACTION_STOP_SIP_STACK:
                    Log.d(Constants.TAG, "ACTION_STOP_SIP_STACK");

                    Log.d(Constants.TAG, "Stop Sip stack!");
                    stopSipStack();

                    break;

                default:
                    break;
                }
            } catch (Exception e) {
                sendErrorToHandler(e);
            }
        }

    }

    private void setSessionInSingelton(CryptoCallSession session) {
        Intent it = new Intent(this, SetSessionIntentService.class);
        it.putExtra(SetSessionIntentService.EXTRA_ACTION,
                SetSessionIntentService.ACTION_SET_SESSION);
        Bundle data = new Bundle();
        data.putParcelable(SetSessionIntentService.DATA_CRYPTOCALL_SESSION, session);
        it.putExtra(SetSessionIntentService.EXTRA_DATA, data);
        startService(it);
    }

    private void makeCall(CryptoCallSession session) {
        // foo@ is needed, don't know why!
        String sipUri = "CryptoCall@" + session.serverIp + ":" + session.serverPort
                + ";transport=tls";

        Intent itCall = new Intent(ACTION_CALL);
        // sipUri is the sip number or uri you'd like to call (domain added
        // automatically if needed)
        // PROTOCOL_CSIP to force the usage of csipsimple!
        itCall.setData(SipUri.forgeSipUri(SipManager.PROTOCOL_CSIP, sipUri));
        itCall.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        // disable choosing of account:
        itCall.putExtra(SipManager.EXTRA_FALLBACK_BEHAVIOR, SipManager.FALLBACK_AUTO_CALL_OTHER);
        // use this account:
        Log.d(Constants.TAG, "mAccId: " + Long.valueOf(mAccId).toString());
        itCall.putExtra(SipProfile.FIELD_ACC_ID, mAccId);
        // id to use for this call
        startActivity(itCall);
    }

    private void createAccount() {
        /*
         * Workaround To prevent DeviceStateReceiver to restart sip service even when we stopped the
         * service
         * 
         * This is internal API!
         * 
         * TODO: Is it necessary to set it to false here?
         */
        SipConfigManager.setPreferenceBooleanValue(this, PreferencesProviderWrapper.HAS_BEEN_QUIT,
                false);

        // reset current object variables
        mSipProfile = null;
        mAccId = SipProfile.INVALID_ID;

        // Get current account if any
        Cursor c = getContentResolver().query(
                SipProfile.ACCOUNT_URI,
                new String[] { SipProfile.FIELD_ID, SipProfile.FIELD_ACC_ID,
                        SipProfile.FIELD_REG_URI }, null, null, SipProfile.FIELD_PRIORITY + " ASC");
        if (c != null) {
            try {
                // simply go to first found account
                if (c.moveToFirst()) {
                    SipProfile foundProfile = new SipProfile(c);
                    Log.d(Constants.TAG, "Found profile with id " + foundProfile.id + ": "
                            + foundProfile.getSipUserName() + "@" + foundProfile.getSipDomain());

                    // direct return
                    mSipProfile = foundProfile;
                    mAccId = foundProfile.id;
                }
            } catch (Exception e) {
                Log.e(Constants.TAG, "Some problem occured while accessing cursor", e);
            } finally {
                c.close();
            }

        }

        // account settings
        SipProfile builtProfile = new SipProfile();
        builtProfile.display_name = "CryptoCall";
        builtProfile.wizard = "LOCAL";
        builtProfile.reg_uri = "";
        builtProfile.acc_id = "";
        builtProfile.transport = SipProfile.TRANSPORT_TLS;
        builtProfile.use_srtp = 1;
        builtProfile.active = true;
        // builtProfile.sip_stun_use = 1;
        // builtProfile.media_stun_use = 1;
        // builtProfile.ice_cfg_enable = 1;
        // builtProfile.ice_cfg_use = 1;
        // builtProfile.turn_cfg_enable = 1;
        // builtProfile.turn_cfg_use = 1;
        // builtProfile.turn_cfg_password
        // builtProfile.turn_cfg_server
        // builtProfile.turn_cfg_user

        ContentValues builtValues = builtProfile.getDbContentValues();

        // if already existing account was found, update this with the account settings
        if (mAccId != SipProfile.INVALID_ID) {
            getContentResolver().update(
                    ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, mAccId),
                    builtValues, null, null);
            Log.d(Constants.TAG, "Updated local acc with " + mAccId);
        } else {
            // else insert new account!
            Uri savedUri = getContentResolver().insert(SipProfile.ACCOUNT_URI, builtValues);
            if (savedUri != null) {
                mAccId = ContentUris.parseId(savedUri);
            }
            Log.d(Constants.TAG, "Added local acc with " + mAccId);
        }
        mSipProfile = builtProfile;
    }

    private void startSipStack() {
        // start sip service to open port etc.
        Intent it = new Intent(SipManager.INTENT_SIP_SERVICE);
        startService(it);
    }

    private void stopSipStack() {
        /*
         * Workaround To prevent DeviceStateReceiver to restart sip service even when we stopped the
         * service
         * 
         * This is internal API!
         */
        SipConfigManager.setPreferenceBooleanValue(this, PreferencesProviderWrapper.HAS_BEEN_QUIT,
                true);

        Log.d(Constants.TAG, "Stop sip stack!");
        Intent intent = new Intent(SipManager.ACTION_SIP_CAN_BE_STOPPED);
        sendBroadcast(intent);
    }

    private void sendErrorToHandler(Exception e) {
        Log.e(Constants.TAG, "KeychainService Exception: ", e);
        e.printStackTrace();

        // Bundle data = new Bundle();
        // data.putString(KeychainIntentServiceHandler.DATA_ERROR, e.getMessage());
        // sendMessageToHandler(KeychainIntentServiceHandler.MESSAGE_EXCEPTION, null, data);
    }

    public void sendUpdateUiToHandler(String message, int progress) {
        Bundle data = new Bundle();
        data.putString(HANDLER_DATA_MESSAGE, message);
        if (progress != -1) {
            data.putInt(HANDLER_DATA_PROGRESS, progress);
        }

        sendMessageToHandler(HANDLER_MSG_UPDATE_UI, data);
    }

    public void sendUpdateUiToHandler(int messageRes, int progress) {
        sendUpdateUiToHandler(getString(messageRes), progress);
    }

    public void sendUpdateUiToHandler(int messageRes) {
        sendUpdateUiToHandler(getString(messageRes), -1);
    }

    private void sendMessageToHandler(Integer what, Bundle data) {
        Message msg = Message.obtain();
        msg.what = what;
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
}
