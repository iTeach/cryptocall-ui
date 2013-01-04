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

import org.cryptocall.CryptoCallSession;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;

public class CryptoCallSessionFactory {

    /**
     * Retrieves name and corresponding telephone number from address book based on email address
     * and build CryptoCallSession out of it!
     * 
     * @param context
     */
    public static CryptoCallSession generateSessionWithNameAndTelephoneNumber(Context context,
            String email) {
        CryptoCallSession session = new CryptoCallSession();
        session.peerEmail = email;

        byte[] pgpEmailSalt = null;
        try {
            pgpEmailSalt = ProtectedEmailUtils.getSaltFromProtectedEmail(email);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception", e);
        }

        // build cursor with new CursorLoader to get all contacts with specific email
        Cursor cursor = context.getContentResolver()
                .query(Data.CONTENT_URI,
                        new String[] { Contacts._ID, Contacts.DISPLAY_NAME, Email.DATA,
                                Data.RAW_CONTACT_ID },
                        Data.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "' AND " + Email.DATA
                                + "=?", new String[] { email }, null);

        // get the contact for this email
        if (cursor.moveToFirst()) {
            session.peerName = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
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

                if (generatedEmail != null && generatedEmail.equals(session.peerEmail)) {
                    Log.d(Constants.TAG, "Found telephoneNumber! email: " + session.peerEmail
                            + " for telephoneNumber " + telephoneNumber);

                    session.peerTelephoneNumber = telephoneNumber;
                }

            }
            phonesCursor.close();

        }
        cursor.close();

        return session;
    }
}
