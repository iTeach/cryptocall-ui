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

package org.cryptocall.util;

import java.util.Hashtable;

import org.cryptocall.R;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.ImageView;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

public class QrCodeUtils {
    public final static QRCodeWriter QR_CODE_WRITER = new QRCodeWriter();

    /**
     * Generate Bitmap with QR Code based on input, taken from Bitcoin-wallet,
     * http://code.google.com/p/bitcoin-wallet/
     * 
     * @author Andreas Schildbach
     * @param input
     * @param size
     * @return QR Code as Bitmap
     */
    public static Bitmap getQRCodeBitmap(final String input, final int size) {
        try {
            final Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);
            final BitMatrix result = QR_CODE_WRITER.encode(input, BarcodeFormat.QR_CODE, size,
                    size, hints);

            final int width = result.getWidth();
            final int height = result.getHeight();
            final int[] pixels = new int[width * height];

            for (int y = 0; y < height; y++) {
                final int offset = y * width;
                for (int x = 0; x < width; x++) {
                    pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.TRANSPARENT;
                }
            }

            final Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
            return bitmap;
        } catch (final WriterException e) {
            Log.e(Constants.TAG, "Exception while generating QR Code!", e);
            return null;
        }
    }

    /**
     * Displays QrCode in Dialog
     */
    public static void showQrCode(Activity activity, Bitmap qrCodeBitmap) {
        final Dialog dialog = new Dialog(activity);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.qr_code_dialog);
        final ImageView imageView = (ImageView) dialog.findViewById(R.id.qr_dialog_view);
        imageView.setImageBitmap(qrCodeBitmap);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        imageView.setOnClickListener(new OnClickListener() {
            public void onClick(final View v) {
                dialog.dismiss();
            }
        });
    }

}
