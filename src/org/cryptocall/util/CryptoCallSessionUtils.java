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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cryptocall.CryptoCallSession;
import org.cryptocall.syncadapter.ContactsSyncAdapterService.CryptoCallContract;
import org.sufficientlysecure.keychain.integration.KeychainContentProviderHelper;
import org.sufficientlysecure.keychain.integration.KeychainUtil;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
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
        Log.d(Constants.TAG, "getNameAndTelephoneNumberFromEmail email: " + session.peerEmail);
        long masterKeyId = (new KeychainContentProviderHelper(context))
                .getPublicKeyringIdsByEmail(session.peerEmail)[0];

        // build cursor with new CursorLoader to get all contacts with specific email
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI,
                new String[] { Contacts._ID, Contacts.DISPLAY_NAME,
                        CryptoCallContract.DATA1_MASTER_KEY_ID,
                        CryptoCallContract.DATA3_KEYRING_NICKNAME,
                        CryptoCallContract.DATA4_TELEPHONE_NUMBER },
                Data.MIMETYPE + "='" + CryptoCallContract.CONTENT_ITEM_TYPE + "' AND "
                        + CryptoCallContract.DATA1_MASTER_KEY_ID + "=?",
                new String[] { String.valueOf(masterKeyId) }, null);

        Log.d(Constants.TAG,
                "getNameAndTelephoneNumberFromEmail Cursor:\n"
                        + DatabaseUtils.dumpCursorToString(cursor));

        // get the contact for this email
        // TODO: many contacts with same email???
        if (cursor.moveToFirst()) {
            session.peerName = cursor.getString(cursor
                    .getColumnIndex(CryptoCallContract.DATA3_KEYRING_NICKNAME));
            session.peerTelephoneNumber = cursor.getString(cursor
                    .getColumnIndex(CryptoCallContract.DATA4_TELEPHONE_NUMBER));

            Log.d(Constants.TAG, "Found telephoneNumber! email: " + session.peerEmail
                    + " for telephoneNumber " + session.peerTelephoneNumber);

        } else {
            Log.e(Constants.TAG, "name and telephone number not found for email: "
                    + session.peerEmail);
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
    public static CryptoCallSession getEmailAndNameFromTelephoneNumber(Context context,
            CryptoCallSession session) throws EmailNotFoundException {

        // build cursor with new CursorLoader to get all contacts with specific email
        Cursor cursor = context.getContentResolver().query(
                Data.CONTENT_URI,
                new String[] { Contacts._ID, Contacts.DISPLAY_NAME,
                        CryptoCallContract.DATA1_MASTER_KEY_ID,
                        CryptoCallContract.DATA3_KEYRING_NICKNAME,
                        CryptoCallContract.DATA4_TELEPHONE_NUMBER },
                Data.MIMETYPE + "='" + CryptoCallContract.CONTENT_ITEM_TYPE + "' AND "
                        + CryptoCallContract.DATA4_TELEPHONE_NUMBER + "=?",
                new String[] { String.valueOf(session.peerTelephoneNumber) }, null);

        Log.d(Constants.TAG,
                "getNameAndTelephoneNumberFromEmail Cursor:\n"
                        + DatabaseUtils.dumpCursorToString(cursor));

        // get the contact for this email
        // TODO: many contacts with same telephone number???
        if (cursor.moveToFirst()) {
            long masterKeyId = cursor.getLong(cursor
                    .getColumnIndex(CryptoCallContract.DATA1_MASTER_KEY_ID));

            String userId = (new KeychainContentProviderHelper(context)).getUserId(masterKeyId,
                    false);

            session.peerName = KeychainUtil.splitUserId(userId)[0];
            session.peerEmail = KeychainUtil.splitUserId(userId)[1];
            Log.e(Constants.TAG, "session.peerName: " + session.peerName + "\nsession.peerEmail: "
                    + session.peerEmail);

            return session;
        } else {
            Log.e(Constants.TAG, "keyring not found for telnr: " + session.peerTelephoneNumber);
        }
        cursor.close();

        // TODO: check if valid and can encrypt etc

        throw new EmailNotFoundException("No email found!");
    }

}
