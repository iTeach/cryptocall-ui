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

package org.cryptocall.util;

import java.util.ArrayList;

import org.thialfihar.android.apg.integration.ApgContentProviderHelper;
import org.thialfihar.android.apg.integration.ApgUtil;

import android.content.ContentProviderOperation;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Data;

public class SyncContacts {

    public static void syncContacts(Context context) {
        // go through all APG emails with @cryptocall in it and find the one for this
        // telephoneNumber
        try {
            Uri contentUri = Uri.withAppendedPath(
                    ApgContentProviderHelper.CONTENT_URI_PUBLIC_KEY_RING_BY_LIKE_EMAIL,
                    Constants.CRYPTOCALL_DOMAIN);
            Cursor pgpKeyringsCursor = context.getContentResolver().query(contentUri,
                    new String[] { "master_key_id", "user_id" }, null, null, null);

            while (pgpKeyringsCursor != null && pgpKeyringsCursor.moveToNext()) {
                String pgpUserId = pgpKeyringsCursor.getString(1);
                String[] pgpSplit = ApgUtil.splitUserId(pgpUserId);
                String pgpEmail = pgpSplit[1];

                byte[] pgpEmailSalt = null;
                try {
                    pgpEmailSalt = ProtectedEmailUtils.getSaltFromProtectedEmail(pgpEmail);
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Exception", e);
                }

                Cursor phonesCursor = context.getContentResolver().query(Data.CONTENT_URI, null,
                        Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'", null, null);

                // go through all phone numbers
                while (phonesCursor != null && phonesCursor.moveToNext()) {
                    long rawContactId = phonesCursor.getLong(phonesCursor
                            .getColumnIndex(Data.RAW_CONTACT_ID));

                    String telephoneNumber = phonesCursor.getString(phonesCursor
                            .getColumnIndex(Phone.NUMBER));

                    Log.d(Constants.TAG, "telephoneNumber: " + telephoneNumber);

                    String generatedEmail = ProtectedEmailUtils.generateProtectedEmail(
                            telephoneNumber, pgpEmailSalt);

                    if (generatedEmail.equals(pgpEmail)) {
                        Log.d(Constants.TAG, "Found email! pgpEmail: " + pgpEmail
                                + " for telephoneNumber " + telephoneNumber);

                        Cursor emailsCursor = context.getContentResolver().query(
                                Data.CONTENT_URI,
                                null,
                                Email.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                                        + Email.CONTENT_ITEM_TYPE + "'",
                                new String[] { String.valueOf(rawContactId) }, null);

                        // go through all email addresses of this user
                        boolean existing = false;
                        while (emailsCursor != null && emailsCursor.moveToNext()) {
                            // This would allow you get several email addresses
                            String emailAddress = emailsCursor.getString(emailsCursor
                                    .getColumnIndex(Email.DATA));

                            Log.d(Constants.TAG, "emailAddress: " + emailAddress);
                            if (emailAddress.equals(generatedEmail)) {
                                Log.d(Constants.TAG, "existing: true");

                                existing = true;
                            }

                        }
                        emailsCursor.close();

                        if (!existing) {
                            Log.d(Constants.TAG, "Adding new email...");

                            ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

                            ops.add(ContentProviderOperation.newInsert(Data.CONTENT_URI)
                                    .withValue(Data.RAW_CONTACT_ID, rawContactId)
                                    .withValue(Email.DATA, generatedEmail)
                                    .withValue(Email.MIMETYPE, Email.CONTENT_ITEM_TYPE)
                                    .withValue(Email.LABEL, "CryptoCall")
                                    .withValue(Email.TYPE, Email.TYPE_CUSTOM).build());
                            try {
                                context.getContentResolver().applyBatch(ContactsContract.AUTHORITY,
                                        ops);
                            } catch (Exception e) {
                                Log.e(Constants.TAG, "Problem while inserting email address!", e);
                            }
                        }
                    }

                }
                phonesCursor.close();
            }

            if (pgpKeyringsCursor != null) {
                pgpKeyringsCursor.close();
            }
        } catch (SecurityException e) {
            Log.e(Constants.TAG, "insufficient permissions to use apg service!");
        }
    }

}
