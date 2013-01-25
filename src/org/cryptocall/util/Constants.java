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

import java.util.regex.Pattern;

import android.accounts.Account;

public class Constants {
    /* Debug constants */
    public static final boolean DEBUG = false;

    /* Tag for logging */
    public static final String TAG = "CryptoCall";

    public static final String PREFS_NAME = "preferences";

    public static final String CRYPTOCALL_DOMAIN = "@cryptocall.org";

    /* SMS */
    public static final short DATA_SMS_PORT = 6662;
    public static final String SMS_PREFIX = "---CryptoCall---";
    public static final String SMS_SEPERATOR = "$";

    /* Regex to get telphoneNumber out of CryptoCall Email */
    public static final Pattern cryptoCallPattern = Pattern
            .compile(".*<(\\S+)@cryptocall\\.org>.*");
    public static final Pattern cryptoCallPattern2 = Pattern.compile("(\\S+)@cryptocall\\.org");

    /* Sync Adapter */
    public static final String SYNC_ACCOUNT_NAME = "CryptoCall";
    public static final String SYNC_ACCOUNT_TYPE = "org.cryptocall.account";
    public static final String SYNC_CONTENT_AUTHORITY = "com.android.contacts";

    public static final Account SYNC_ACCOUNT = new Account(SYNC_ACCOUNT_NAME, SYNC_ACCOUNT_TYPE);
}