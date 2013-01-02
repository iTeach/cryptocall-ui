/*
 * Copyright (C) 2011 Sergej Dechand <cryptocall@serj.de>
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

package org.cryptocall.util;

import org.cryptocall.util.Constants;

import android.content.Context;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;

public class CryptoCallSession implements Parcelable {

    private String mEmail;

    private String mName;
    private String mTelephoneNumber;

    private String mServerIp;
    private int mServerPort;
    private String mPublicKey;
    private String mX509Cert;

    public String getEmail() {
        return mEmail;
    }

    public String getName() {
        return mName;
    }

    public String getServerIp() {
        return mServerIp;
    }

    public String getTelephoneNumber() {
        return mTelephoneNumber;
    }

    public int getServerPort() {
        return mServerPort;
    }

    public String getServerExtra() {
        return serverExtra;
    }

    private String serverExtra;

    // Constructor
    public CryptoCallSession(String email) {
        this.mEmail = email;
    }

    /**
     * Retrieves name and corresponding telephone number from address book based on email address
     * 
     * @param context
     */
    public void getNameAndTelephoneNumber(Context context) {
        byte[] pgpEmailSalt = null;
        try {
            pgpEmailSalt = ProtectedEmailUtils.getSaltFromProtectedEmail(mEmail);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception", e);
        }

        // build cursor with new CursorLoader to get all contacts with specific email
        Cursor cursor = context.getContentResolver()
                .query(Data.CONTENT_URI,
                        new String[] { Contacts._ID, Contacts.DISPLAY_NAME, Email.DATA,
                                Data.RAW_CONTACT_ID },
                        Data.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "' AND " + Email.DATA
                                + "=?", new String[] { mEmail }, null);

        // get the contact for this email
        if (cursor.moveToFirst()) {
            mName = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
            long rawContactId = cursor.getLong(cursor.getColumnIndex(Data.RAW_CONTACT_ID));

            Cursor phonesCursor = context.getContentResolver().query(
                    Data.CONTENT_URI,
                    null,
                    Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "' AND "
                            + Email.RAW_CONTACT_ID + "=?",
                    new String[] { String.valueOf(rawContactId) }, null);

            // go through all phone numbers of this contact and find matching one
            while (phonesCursor != null && phonesCursor.moveToNext()) {
                String telephoneNumber = phonesCursor.getString(phonesCursor
                        .getColumnIndex(Phone.NUMBER));

                Log.d(Constants.TAG, "telephoneNumber: " + telephoneNumber);

                String generatedEmail = ProtectedEmailUtils.generateProtectedEmail(telephoneNumber,
                        pgpEmailSalt);

                if (generatedEmail != null && generatedEmail.equals(mEmail)) {
                    Log.d(Constants.TAG, "Found telephoneNumber! email: " + mEmail
                            + " for telephoneNumber " + telephoneNumber);

                    mTelephoneNumber = telephoneNumber;
                }

            }
            phonesCursor.close();

        }
        cursor.close();
    }

    // private String parseEmailToTelephoneNumber(String email) {
    // // Parse email to telephoneNumber
    // String telephoneNumber = null;
    // Matcher cryptoCallMatcher = Constants.cryptoCallPattern2.matcher(email);
    // while (cryptoCallMatcher.find()) {
    // telephoneNumber = cryptoCallMatcher.group(1);
    // }
    //
    // return telephoneNumber;
    // }

    // Parcelling part
    public CryptoCallSession(Parcel in) {
        String[] data = new String[6];

        in.readStringArray(data);

        this.mEmail = data[0];
        this.mName = data[1];
        this.mTelephoneNumber = data[2];
        this.mServerIp = data[3];
        this.mServerPort = Integer.parseInt(data[4]);
        this.serverExtra = data[5];
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringArray(new String[] { this.mEmail, this.mName, this.mTelephoneNumber,
                this.mServerIp, String.valueOf(this.mServerPort), this.serverExtra });
    }

    public String getPublicKey() {
        return mPublicKey;
    }

    public void setPublicKey(String publicKey) {
        this.mPublicKey = publicKey;
    }

    public String getX509Cert() {
        return mX509Cert;
    }

    public void setX509Cert(String x509Cert) {
        mX509Cert = x509Cert;
    }

    @SuppressWarnings("rawtypes")
    public static final Parcelable.Creator CREATOR = new Parcelable.Creator() {
        public CryptoCallSession createFromParcel(Parcel in) {
            return new CryptoCallSession(in);
        }

        public CryptoCallSession[] newArray(int size) {
            return new CryptoCallSession[size];
        }
    };

}