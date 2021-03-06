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

package org.cryptocall.ui;

import java.util.List;

import org.cryptocall.CryptoCallApplication;
import org.cryptocall.R;

import org.cryptocall.util.Constants;
import org.cryptocall.util.Log;
import org.cryptocall.util.PreferencesHelper;
import org.cryptocall.util.QrCodeUtils;
import org.sufficientlysecure.keychain.integration.KeychainIntentHelper;
import org.sufficientlysecure.keychain.service.IKeychainKeyService;
import org.sufficientlysecure.keychain.service.handler.IKeychainGetKeyringsHandler;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

public class BaseExchangeFragment extends Fragment {
    private BaseActivity mBaseActivity;
    private CryptoCallApplication mApplication;
    private IKeychainKeyService mIKeychainKeyService;

    private ImageView mQrCodeImageView;
    private Button mShareNfcButton;
    private Button mShareButton;

    private Button mImportFromQrCode;

    private Bitmap mQrCodeBitmap;

    private KeychainIntentHelper mKeychainIntentHelper;

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.base_exchange_fragment, container, false);

        mKeychainIntentHelper = new KeychainIntentHelper(getActivity());

        // get views
        mQrCodeImageView = (ImageView) view.findViewById(R.id.base_information_fragment_qr);
        mShareNfcButton = (Button) view
                .findViewById(R.id.base_information_fragment_share_nfc_button);
        mShareButton = (Button) view.findViewById(R.id.base_information_fragment_share_button);
        mImportFromQrCode = (Button) view
                .findViewById(R.id.base_information_fragment_import_from_qr_code_button);

        mQrCodeImageView.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                if (mQrCodeBitmap != null) {
                    QrCodeUtils.showQrCode(mBaseActivity, mQrCodeBitmap);
                }
            }
        });

        mShareNfcButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                long masterKeyId = PreferencesHelper.getPgpMasterKeyId(mBaseActivity);
                mKeychainIntentHelper.shareWithNfc(masterKeyId);
            }
        });

        mShareButton.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                long masterKeyId = PreferencesHelper.getPgpMasterKeyId(mBaseActivity);
                mKeychainIntentHelper.share(masterKeyId);
            }
        });

        mImportFromQrCode.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                mKeychainIntentHelper.importFromQrCode();
            }
        });

        return view;
    }

    /**
     * onDestroy of view, recycle QRCodeBitmap
     */
    @Override
    public void onDestroyView() {
        if (mQrCodeBitmap != null) {
            mQrCodeBitmap.recycle();
            mQrCodeBitmap = null;
        }

        super.onDestroyView();
    }

    /**
     * onResume set informations and QrCode
     */
    @Override
    public void onResume() {
        super.onResume();

        // get public keyring for qr code
        if (mIKeychainKeyService != null) {
            try {
                mIKeychainKeyService.getPublicKeyRings(
                        new long[] { PreferencesHelper.getPgpMasterKeyId(mBaseActivity) }, true,
                        getPublicKeyringHandler);
            } catch (RemoteException e) {
                Log.e(Constants.TAG, "RemoteException", e);
            }
        }
    }

    private final IKeychainGetKeyringsHandler.Stub getPublicKeyringHandler = new IKeychainGetKeyringsHandler.Stub() {

        @Override
        public void onException(final int exceptionId, final String message) throws RemoteException {
            mBaseActivity.runOnUiThread(new Runnable() {
                public void run() {
                    Log.e(Constants.TAG, "Exception in KeychainKeyService: " + message);
                }
            });
        }

        @Override
        public void onSuccess(final byte[] outputBytes, final List<String> outputStrings)
                throws RemoteException {
            String keyring = outputStrings.get(0);

            // populate qrcode representation
            mQrCodeBitmap = QrCodeUtils.getQRCodeBitmap(keyring, 1000);

            mBaseActivity.runOnUiThread(new Runnable() {
                public void run() {
                    mQrCodeImageView.setImageBitmap(mQrCodeBitmap);
                }
            });

        }

    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBaseActivity = (BaseActivity) getActivity();
        mApplication = (CryptoCallApplication) getActivity().getApplication();
        mIKeychainKeyService = mApplication.getKeychainKeyService();
    }
}
