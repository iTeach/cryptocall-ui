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

import java.text.ParseException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cryptocall.CryptoCallSession;
import org.sufficientlysecure.keychain.integration.KeychainContentProviderHelper;

import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

public class CryptoCallSessionUtils {

    public static class EmailNotFoundException extends Exception {
        private static final long serialVersionUID = -3346819833021232175L;

        public EmailNotFoundException() {

        }

        public EmailNotFoundException(String s) {
            super(s);
        }
    }

    public static class SmsParsingFailedException extends Exception {
        private static final long serialVersionUID = 196162475783075979L;

        public SmsParsingFailedException() {

        }

        public SmsParsingFailedException(String s) {
            super(s);
        }
    }

    /* Static pattern and matcher */
    private static final String PRE_MESSAGE_MATCHER = "^" + Constants.SMS_PREFIX;
    private static Pattern mPreMessagePattern;
    private static Matcher mPreMessageMatcher;

    static {
        mPreMessagePattern = Pattern.compile(PRE_MESSAGE_MATCHER);
    }

    /**
     * Retrieves name and corresponding telephone number from address book based on email address
     * and build CryptoCallSession out of it!
     * 
     * @param context
     */
    public static CryptoCallSession getNameAndTelephoneNumberFromEmail(Context context,
            CryptoCallSession session) {
        byte[] pgpEmailSalt = null;
        try {
            pgpEmailSalt = ProtectedEmailUtils.getSaltFromProtectedEmail(session.peerEmail);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception", e);
        }

        // build cursor with new CursorLoader to get all contacts with specific email
        Cursor cursor = context.getContentResolver()
                .query(Data.CONTENT_URI,
                        new String[] { Contacts._ID, Contacts.DISPLAY_NAME, Email.DATA,
                                Data.RAW_CONTACT_ID },
                        Data.MIMETYPE + "='" + Email.CONTENT_ITEM_TYPE + "' AND " + Email.DATA
                                + "=?", new String[] { session.peerEmail }, null);

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

    /**
     * Checks if sms if cryptocall sms
     * 
     * @param body
     * @return true if is cryptocall sms
     */
    public static boolean isCryptoCallSms(String body) {
        mPreMessageMatcher = mPreMessagePattern.matcher(body);

        // is CryptoCall SMS?
        if (mPreMessageMatcher.find()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Parse sms message
     * 
     * @return returns session if it was successfully parsed as a cryptocall sms
     */
    public static CryptoCallSession getIpPortAndTelephoneNumberFromSms(CryptoCallSession session,
            String body, String from) throws SmsParsingFailedException {
        Log.d(Constants.TAG, "getIpPortAndTelephoneNumberFromSms:\nbody: " + body + "\nfrom: "
                + from);

        // normalize and save telephone number
        session.peerTelephoneNumber = ProtectedEmailUtils.normalizeTelephoneNumber(from);

        mPreMessageMatcher = mPreMessagePattern.matcher(body);

        // is CryptoCall SMS?
        if (mPreMessageMatcher.find()) {
            // get content
            String content = mPreMessageMatcher.replaceFirst("");

            mPreMessageMatcher = mPreMessagePattern.matcher(content);
            int indexSeperator = -1;

            indexSeperator = content.indexOf(Constants.SMS_SEPERATOR);
            // if seperator found go on
            if (indexSeperator != -1) {
                // save ip and strip ip with seperator from message
                session.serverIp = content.substring(0, indexSeperator);
                content = content.substring(indexSeperator + 1);

                indexSeperator = content.indexOf(Constants.SMS_SEPERATOR);
                // if seperator found go on
                if (indexSeperator != -1) {
                    // save port and strip port with seperator from message
                    session.serverPort = Integer.parseInt(content.substring(0, indexSeperator));
                    content = content.substring(indexSeperator + 1);

                    if (!content.equals("")) {
                        // extra is remaining part of message
                        String extra = content; // TODO: not used

                        // everything okay, return
                        return session;
                    } else {
                        throw new SmsParsingFailedException(
                                "Could not parse CryptoCall SMS! content != ''");
                    }
                } else {
                    throw new SmsParsingFailedException(
                            "Could not parse CryptoCall SMS! Did not found 2. seperator");
                }
            } else {
                throw new SmsParsingFailedException(
                        "Could not parse CryptoCall SMS! Did not found 1. seperator");
            }
        } else {
            throw new SmsParsingFailedException("Not a CryptoCall SMS!");
        }
    }

    /**
     * Searches for telephone number in androids address book. And checks Email against emails in
     * the found contact. When email address is found it, session.peerEmail is set accordingly and
     * function returns session, else it returns null
     * 
     * @param context
     * @param session
     */
    public static CryptoCallSession getEmailFromTelephoneNumber(Context context,
            CryptoCallSession session) throws EmailNotFoundException {
        Cursor phonesCursor = context.getContentResolver().query(
                Data.CONTENT_URI,
                null,
                Phone.NORMALIZED_NUMBER + "=?" + " AND " + Data.MIMETYPE + "='"
                        + Phone.CONTENT_ITEM_TYPE + "'",
                new String[] { String.valueOf(session.peerTelephoneNumber) }, null);

        // go through all phone numbers
        // TODO: many contacts with same number???
        while (phonesCursor != null && phonesCursor.moveToNext()) {
            long rawContactId = phonesCursor.getLong(phonesCursor
                    .getColumnIndex(Data.RAW_CONTACT_ID));

            String currentTelephoneNumber = phonesCursor.getString(phonesCursor
                    .getColumnIndex(Phone.NORMALIZED_NUMBER));

            Log.d(Constants.TAG, "currentTelephoneNumber: " + currentTelephoneNumber);

            Cursor emailsCursor = context.getContentResolver().query(
                    Data.CONTENT_URI,
                    null,
                    Email.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                            + Email.CONTENT_ITEM_TYPE + "'",
                    new String[] { String.valueOf(rawContactId) }, null);

            // go through all email addresses of this user
            // boolean existing = false;
            while (emailsCursor != null && emailsCursor.moveToNext()) {
                // This would allow you get several email addresses
                String currentEmail = emailsCursor.getString(emailsCursor
                        .getColumnIndex(Email.DATA));

                Log.d(Constants.TAG, "currentEmail: " + currentEmail);

                try {
                    String generatedEmail = ProtectedEmailUtils.generateProtectedEmail(
                            session.peerTelephoneNumber,
                            ProtectedEmailUtils.getSaltFromProtectedEmail(currentEmail));

                    if (currentEmail.equals(generatedEmail)) {
                        Log.d(Constants.TAG, "Found email: " + currentEmail);

                        // TODO: check if valid and can encrypt etc
                        // check if existing in Keychain!
                        long[] keyringIds = (new KeychainContentProviderHelper(context))
                                .getPublicKeyringIdsByEmail(currentEmail);
                        if (keyringIds != null && keyringIds.length > 0) {
                            Log.d(Constants.TAG, "found keyring for " + currentEmail);

                            session.peerEmail = currentEmail;
                            return session;
                        } else {
                            Log.d(Constants.TAG, "Did not found keyring for " + currentEmail);
                        }
                    }
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Exception", e);
                }
            }
            emailsCursor.close();
        }
        phonesCursor.close();

        throw new EmailNotFoundException("No email found!");
    }

}
