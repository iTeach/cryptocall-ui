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

import org.cryptocall.R;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesHelper {

    public static boolean getFirstStart(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME, 0);
        return prefs.getBoolean(context.getString(R.string.pref_first_start_key),
                Boolean.parseBoolean(context.getString(R.string.pref_first_start_def)));
    }

    public static void setFirstStart(Context context, boolean value) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(context.getString(R.string.pref_first_start_key), value);
        editor.commit();
    }

    public static long getPgpMasterKeyId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        return prefs.getLong(context.getString(R.string.pref_pgp_masterkeyid_key), 0);
    }

    public static void setPgpMasterKeyId(Context context, long value) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong(context.getString(R.string.pref_pgp_masterkeyid_key), value);
        editor.commit();
    }

    public static String getPgpEmail(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        return prefs.getString(context.getString(R.string.pref_pgp_email_key), null);
    }

    public static void setPgpEmail(Context context, String value) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.pref_pgp_email_key), value);
        editor.commit();
    }

    public static String getTelephoneNumber(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        return prefs.getString(context.getString(R.string.pref_telephone_number_key),
                context.getString(R.string.pref_telephone_number_def));
    }

    public static void setTelephoneNumber(Context context, String value) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.PREFS_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(context.getString(R.string.pref_telephone_number_key), value);
        editor.commit();
    }

}