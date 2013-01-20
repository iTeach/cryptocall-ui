/*
 * Copyright (C) 2013 Dominik Schürmann <dominik@dominikschuermann.de
 * Copyright (C) 2010 Sam Steele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.cryptocall.syncadapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.cryptocall.util.Constants;
import org.cryptocall.util.ProtectedEmailUtils;
import org.sufficientlysecure.keychain.integration.KeychainContentProviderHelper;
import org.sufficientlysecure.keychain.integration.KeychainUtil;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;

public class ContactsSyncAdapterService extends Service {
    private static SyncAdapterImpl sSyncAdapter = null;
    private static ContentResolver mContentResolver = null;

    /**
     * Definition of CryptoCalls database usage for raw contacts
     */
    public static final class CryptoCallContract {

        private CryptoCallContract() {
        }

        public static final Uri CONTENT_RAW_URI = RawContacts.CONTENT_URI.buildUpon()
                .appendQueryParameter(RawContacts.ACCOUNT_NAME, Constants.SYNC_ACCOUNT_NAME)
                .appendQueryParameter(RawContacts.ACCOUNT_TYPE, Constants.SYNC_ACCOUNT_TYPE)
                .build();

        public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.org.cryptocall.profile";
        public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.org.cryptocall.profile";

        public static String SYNC1_KEYRING_MASTER_KEY_ID = ContactsContract.RawContacts.SYNC1;
        public static String SYNC2_KEYRING_USER_ID = ContactsContract.RawContacts.SYNC2;

        public static String DATA1_MASTER_KEY_ID = ContactsContract.Data.DATA1;
        public static String DATA2_SUMMARY = ContactsContract.Data.DATA2;
        public static String DATA3_KEYRING_NICKNAME = ContactsContract.Data.DATA3;
        public static String DATA4_TELEPHONE_NUMBER = ContactsContract.Data.DATA4;
    }

    public ContactsSyncAdapterService() {
        super();
    }

    private static class SyncAdapterImpl extends AbstractThreadedSyncAdapter {
        private Context mContext;

        public SyncAdapterImpl(Context context) {
            super(context, true);
            mContext = context;
        }

        @Override
        public void onPerformSync(Account account, Bundle extras, String authority,
                ContentProviderClient provider, SyncResult syncResult) {
            try {
                ContactsSyncAdapterService.performSync(mContext, account, extras, authority,
                        provider, syncResult);
            } catch (OperationCanceledException e) {
                Log.e(Constants.TAG, "OperationCanceledException", e);
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        IBinder ret = null;
        ret = getSyncAdapter().getSyncAdapterBinder();
        return ret;
    }

    private SyncAdapterImpl getSyncAdapter() {
        if (sSyncAdapter == null)
            sSyncAdapter = new SyncAdapterImpl(this);
        return sSyncAdapter;
    }

    /**
     * CALLER_IS_SYNCADAPTER is needed to not only set DELTED flag to 1, but really delete!
     * 
     * @param uri
     * @return
     */
    static Uri addCallerIsSyncAdapterParameter(Uri uri) {
        return uri.buildUpon().appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                .build();
    }

    /**
     * Deletes a raw contact.
     * 
     * @param rawId
     */
    private static void deleteRawContact(long rawId) {
        Log.d(Constants.TAG, "Deleteing contact with raw id: " + rawId);
        Uri uri = addCallerIsSyncAdapterParameter(RawContacts.CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(rawId)).build());

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        ContentProviderOperation.Builder builder = null;

        builder = ContentProviderOperation.newDelete(uri);
        operationList.add(builder.build());

        builder = ContentProviderOperation
                .newDelete(addCallerIsSyncAdapterParameter(ContactsContract.Data.CONTENT_URI));
        builder.withSelection(ContactsContract.Data.RAW_CONTACT_ID + " = ?",
                new String[] { String.valueOf(rawId) });
        operationList.add(builder.build());

        try {
            mContentResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception in addContact!", e);
        }
    }

    /**
     * Inserts a contact.
     * 
     * @param account
     * @param displayName
     * @param masterKeyId
     * @param keyringUserId
     * @param telephoneNumber
     * @param rawId
     */
    private static void insertRawContact(Account account, String displayName, Long masterKeyId,
            String keyringUserId, String telephoneNumber) {
        Log.i(Constants.TAG, "Adding contact: " + displayName + " masterKeyId: " + masterKeyId
                + " keyringUserId: " + keyringUserId);
        String[] keyringUserIdSplit = KeychainUtil.splitUserId(keyringUserId);
        String keyringNickname = keyringUserIdSplit[0];

        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();
        ContentProviderOperation.Builder builder = null;

        builder = ContentProviderOperation.newInsert(RawContacts.CONTENT_URI);
        builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
        builder.withValue(RawContacts.ACCOUNT_TYPE, account.type);
        builder.withValue(CryptoCallContract.SYNC1_KEYRING_MASTER_KEY_ID, masterKeyId);
        builder.withValue(CryptoCallContract.SYNC2_KEYRING_USER_ID, keyringUserId);
        operationList.add(builder.build());

        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder.withValueBackReference(
                ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
        builder.withValue(ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        builder.withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, displayName);
        operationList.add(builder.build());

        builder = ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI);
        builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
        builder.withValue(ContactsContract.Data.MIMETYPE, CryptoCallContract.CONTENT_ITEM_TYPE);
        builder.withValue(CryptoCallContract.DATA1_MASTER_KEY_ID, masterKeyId);
        builder.withValue(CryptoCallContract.DATA2_SUMMARY, "CryptoCall Profile");
        builder.withValue(CryptoCallContract.DATA3_KEYRING_NICKNAME, keyringNickname);
        builder.withValue(CryptoCallContract.DATA4_TELEPHONE_NUMBER, telephoneNumber);
        operationList.add(builder.build());

        try {
            mContentResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception in addContact!", e);
        }
    }

    private static void deleteAll(Context context) {
        Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, null,
                ContactsContract.Data.MIMETYPE + "='" + CryptoCallContract.CONTENT_ITEM_TYPE + "'",
                null, null);

        // go through all phone numbers
        while (cursor != null && cursor.moveToNext()) {
            deleteRawContact(cursor.getLong(cursor
                    .getColumnIndex(ContactsContract.Data.RAW_CONTACT_ID)));
        }
    }

    /**
     * Insert raw contact for CryptoCall.
     * 
     * @param context
     * @param account
     * @param masterKeyId
     * @param keyringUserId
     * @param rawId
     */
    private static void syncSingleContact(Context context, Account account, long masterKeyId,
            String keyringUserId) {
        String[] keyringUserIdSplit = KeychainUtil.splitUserId(keyringUserId);
        String keyringEmail = keyringUserIdSplit[1];

        byte[] pgpEmailSalt = null;
        try {
            pgpEmailSalt = ProtectedEmailUtils.getSaltFromProtectedEmail(keyringEmail);
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception", e);
        }

        Cursor phonesCursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI,
                null, ContactsContract.Data.MIMETYPE + "='" + Phone.CONTENT_ITEM_TYPE + "'", null,
                null);

        // go through all phone numbers
        while (phonesCursor != null && phonesCursor.moveToNext()) {
            String displayName = phonesCursor.getString(phonesCursor
                    .getColumnIndex(Phone.DISPLAY_NAME));

            String telephoneNumber = phonesCursor.getString(phonesCursor
                    .getColumnIndex(Phone.NUMBER));

            // normalize telephone number
            telephoneNumber = ProtectedEmailUtils.normalizeTelephoneNumber(telephoneNumber);

            Log.d(Constants.TAG, "normalized telephoneNumber: " + telephoneNumber);

            String generatedEmail = ProtectedEmailUtils.generateProtectedEmail(telephoneNumber,
                    pgpEmailSalt);

            if (generatedEmail != null && generatedEmail.equals(keyringEmail)) {
                Log.d(Constants.TAG, "Found email! pgpEmail: " + keyringEmail
                        + " for telephoneNumber " + telephoneNumber);

                insertRawContact(account, displayName, masterKeyId, keyringUserId, telephoneNumber);
            }

        }
        if (phonesCursor != null) {
            phonesCursor.close();
        }
    }

    private static class SyncEntry {
        public Long rawId = 0L;
        public String keyringUserId = null;
    }

    private static final String KEYCHAIN_COLUMN_MASTER_KEY_ID = "master_key_id";
    private static final String KEYCHAIN_COLUMN_USER_ID = "user_id";

    private static void performSync(Context context, Account account, Bundle extras,
            String authority, ContentProviderClient provider, SyncResult syncResult)
            throws OperationCanceledException {
        performSync(context, account);
    }

    public static void performSync(Context context, Account account) {
        HashMap<Long, SyncEntry> localContacts = new HashMap<Long, SyncEntry>();
        mContentResolver = context.getContentResolver();
        Log.i(Constants.TAG, "performSync: " + account.toString());

        // Load the local contacts
        Cursor c1 = mContentResolver.query(CryptoCallContract.CONTENT_RAW_URI, new String[] {
                BaseColumns._ID, CryptoCallContract.SYNC1_KEYRING_MASTER_KEY_ID,
                CryptoCallContract.SYNC2_KEYRING_USER_ID }, null, null, null);
        while (c1.moveToNext()) {
            SyncEntry entry = new SyncEntry();
            entry.rawId = c1.getLong(c1.getColumnIndex(BaseColumns._ID));

            deleteRawContact(entry.rawId);

            entry.keyringUserId = c1.getString(c1
                    .getColumnIndex(CryptoCallContract.SYNC2_KEYRING_USER_ID));
            localContacts.put(Long.valueOf(c1.getString(c1
                    .getColumnIndex(CryptoCallContract.SYNC1_KEYRING_MASTER_KEY_ID))), entry);
        }

        // debug
        // deleteAll(context);

        /*
         * ACTUAL SYNC
         */
        ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

        // open keyrings cursor
        Uri contentUri = Uri.withAppendedPath(
                KeychainContentProviderHelper.CONTENT_URI_PUBLIC_KEY_RING_BY_LIKE_EMAIL,
                Constants.CRYPTOCALL_DOMAIN);
        Cursor pgpKeyringsCursor = mContentResolver.query(contentUri, new String[] {
                KEYCHAIN_COLUMN_MASTER_KEY_ID, KEYCHAIN_COLUMN_USER_ID }, null, null, null);

        // go through all Keychain emails with @cryptocall in it
        HashSet<Long> existingContacts = new HashSet<Long>();

        if (pgpKeyringsCursor != null && pgpKeyringsCursor.moveToFirst()) {
            do {
                long masterKeyId = pgpKeyringsCursor.getLong(pgpKeyringsCursor
                        .getColumnIndex(KEYCHAIN_COLUMN_MASTER_KEY_ID));
                String keyringUserId = pgpKeyringsCursor.getString(pgpKeyringsCursor
                        .getColumnIndex(KEYCHAIN_COLUMN_USER_ID));

                // collect all key ids from keychain
                existingContacts.add(masterKeyId);

                // check if contact already existing
                if (localContacts.containsKey(masterKeyId)) {
                    Log.d(Constants.TAG, "masterKeyId existing in local contacts: " + masterKeyId);

                    // get corresponding local contact
                    SyncEntry entry = localContacts.get(masterKeyId);

                    // check if keyring user id has changed since last sync
                    if (!keyringUserId.equals(entry.keyringUserId)) {
                        Log.d(Constants.TAG,
                                "user id of masterKeyId has CHANGED in local contacts: "
                                        + masterKeyId);
                        Log.d(Constants.TAG, "keyringUserId: " + keyringUserId);
                        Log.d(Constants.TAG, "entry.keyringUserId: " + entry.keyringUserId);

                        // Update this contact
                        // Delete contact before inserting again if we update
                        Log.d(Constants.TAG, "Delete before inserting again! rawId: " + entry.rawId);
                        deleteRawContact(entry.rawId);
                        Log.d(Constants.TAG, "Insert again! rawId: " + entry.rawId);
                        syncSingleContact(context, account, masterKeyId, keyringUserId);
                    } else {
                        Log.d(Constants.TAG,
                                "masterKeyId has NOT changed user id in local contacts: "
                                        + masterKeyId);
                    }
                } else {
                    Log.d(Constants.TAG, "masterKeyId NOT existing in local contacts: "
                            + masterKeyId);

                    // insert contact
                    syncSingleContact(context, account, masterKeyId, keyringUserId);
                }
            } while (pgpKeyringsCursor.moveToNext());

            /*
             * Delete CryptoCall contacts with masterKeyIds not in OpenPGP Keychain
             */
            HashSet<Long> delete = new HashSet<Long>(localContacts.keySet());
            delete.removeAll(existingContacts);

            // to comma seperated string
            StringBuilder result = new StringBuilder();
            for (Long masterKeyId : delete) {
                result.append(masterKeyId.toString());
                result.append(",");
            }
            String deleteString = result.length() > 0 ? result.substring(0, result.length() - 1)
                    : "";
            Log.d(Constants.TAG, "deleteString: " + deleteString);

            mContentResolver.delete(CryptoCallContract.CONTENT_RAW_URI,
                    CryptoCallContract.SYNC1_KEYRING_MASTER_KEY_ID + " IN (" + deleteString + ")",
                    null);
        }

        if (pgpKeyringsCursor != null) {
            pgpKeyringsCursor.close();
        }

        if (operationList.size() > 0) {
            try {
                mContentResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
            } catch (Exception e) {
                Log.e(Constants.TAG, "Exception in performSync!", e);
            }
        }

    }
}
