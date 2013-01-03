package org.cryptocall.util;

import org.cryptocall.api.CryptoCallSession;

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
    public static CryptoCallSession generateSessionWithNameAndTelephoneNumber(Context context, String email) {
        CryptoCallSession session = new CryptoCallSession();
        session.email = email;

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
            session.name = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
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

                if (generatedEmail != null && generatedEmail.equals(session.email)) {
                    Log.d(Constants.TAG, "Found telephoneNumber! email: " + session.email
                            + " for telephoneNumber " + telephoneNumber);

                    session.telephoneNumber = telephoneNumber;
                }

            }
            phonesCursor.close();

        }
        cursor.close();
        
        return session;
    }
}
