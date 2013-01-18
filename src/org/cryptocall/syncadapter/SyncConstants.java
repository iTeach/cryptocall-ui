package org.cryptocall.syncadapter;

import android.accounts.Account;

public class SyncConstants {
    public static final String ACCOUNT_NAME = "CryptoCall";
    public static final String ACCOUNT_TYPE = "org.cryptocall.account";
    public static final String CONTENT_AUTHORITY = "com.android.contacts";

    public static final Account ACCOUNT = new Account(ACCOUNT_NAME, ACCOUNT_TYPE);
}
