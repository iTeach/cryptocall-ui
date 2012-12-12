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

package org.cryptocall.ui;

import org.cryptocall.R;

import org.cryptocall.util.QrCodeUtils;

import android.app.Activity;

import android.content.Context;
import android.graphics.Bitmap;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import android.telephony.TelephonyManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

public class BaseInformationFragment extends Fragment {
    private Activity mActivity;
    private TextView mKeyTextView;
    private TextView mTelTextView;
    private ImageView mQrCodeImageView;

    private Bitmap mQrCodeBitmap;

    // Toggle Button to enable CryptoCall
    ToggleButton mEnableCryptoCallToggleButton;

    /**
     * Inflate the layout for this fragment
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.base_information_fragment, container, false);

        // get views
        mKeyTextView = (TextView) view.findViewById(R.id.base_information_fragment_key);
        mTelTextView = (TextView) view.findViewById(R.id.base_information_fragment_tel);
        mQrCodeImageView = (ImageView) view.findViewById(R.id.base_information_fragment_qr);

        mQrCodeImageView.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                QrCodeUtils.showQrCode(mActivity, mQrCodeBitmap);
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

        updateView();
    }

    public void updateView() {
        mActivity = getActivity();

        TelephonyManager tMgr = (TelephonyManager) mActivity
                .getSystemService(Context.TELEPHONY_SERVICE);
        mTelTextView.setText(tMgr.getLine1Number());

        // populate qrcode representation
        mQrCodeBitmap = QrCodeUtils.getQRCodeBitmap("asfdsqodwqodwqdoiuwqoiduwqodiwudqoiqwudoiduw",
                256);
        mQrCodeImageView.setImageBitmap(mQrCodeBitmap);
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mActivity = getActivity();

    }
}
