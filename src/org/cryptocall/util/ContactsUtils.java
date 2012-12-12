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

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.support.v4.content.CursorLoader;

public class ContactsUtils {

    static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] { Contacts._ID,
            Contacts.DISPLAY_NAME, Email.DATA, Contacts.LOOKUP_KEY };

    public static String getName(Context context, String email) {
        String name = null;
        
        // build cursor with new CursorLoader to get all contacts with specific email
        String select = Email.DATA + "=?";
        CursorLoader contactCursorLoader = new CursorLoader(context, Email.CONTENT_URI,
                CONTACTS_SUMMARY_PROJECTION, select, new String[] { email }, null);
        Cursor cursor = contactCursorLoader.loadInBackground();

        // go through all contacts
        if (cursor.moveToFirst()) {
            final String currentName = cursor.getString(cursor
                    .getColumnIndex(Contacts.DISPLAY_NAME));
            name = currentName;
        }
        cursor.close();

        return name;
    }

    public static String getPublicKey(String email) {
        // TODO: implement
        return "nope";
    }
}
