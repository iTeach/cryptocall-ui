/*
 * Copyright (C) 2012 Dominik Schürmann <dominik@dominikschuermann.de>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cryptocall.api_test;

import org.cryptocall.service.ICryptoCallService;
import org.cryptocall.service.handler.ICryptoCallGetPublicKeyHandler;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class BaseActivity extends Activity {
    Activity mActivity;

    TextView mMessageTextView;
    TextView mCiphertextTextView;
    TextView mDataTextView;

    static final String TAG = "CryptoCall";

    private ICryptoCallService service = null;
    private ServiceConnection svcConn = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder binder) {
            service = ICryptoCallService.Stub.asInterface(binder);
        }

        public void onServiceDisconnected(ComponentName className) {
            service = null;
        }
    };

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.base_activity);

        mActivity = this;

        mMessageTextView = (TextView) findViewById(R.id.aidl_demo_message);

        bindService(new Intent(ICryptoCallService.class.getName()), svcConn,
                Context.BIND_AUTO_CREATE);
    }

    public void getPubKeysOnClick(View view) {

        try {
            service.getPublicKey("+494444", getPubKeyHandler);
        } catch (RemoteException e) {
            exceptionImplementation(-1, e.toString());
        }
    }

    public void getX509OnClick(View view) {
        // byte[] inputBytes = mCiphertextTextView.getText().toString().getBytes();
        //
        // try {
        // service.decryptAndVerifyAsymmetric(inputBytes, null, null, decryptHandler);
        // } catch (RemoteException e) {
        // exceptionImplementation(-1, e.toString());
        // }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        unbindService(svcConn);
    }

    private void exceptionImplementation(int exceptionId, String error) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Exception!").setMessage(error).setPositiveButton("OK", null).show();
    }

    private final ICryptoCallGetPublicKeyHandler.Stub getPubKeyHandler = new ICryptoCallGetPublicKeyHandler.Stub() {

        @Override
        public void onSuccess(String publicKey) throws RemoteException {
            Log.d(TAG, "publicKey: " + publicKey);

        }

        @Override
        public void onException(int exceptionNumber, String message) throws RemoteException {
            // TODO Auto-generated method stub

        }

    };

}
