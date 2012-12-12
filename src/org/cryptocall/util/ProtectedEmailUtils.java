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

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import android.telephony.PhoneNumberUtils;
import android.util.Base64;

public class ProtectedEmailUtils {
    static final int iterationCount = 1000;
    static final int saltLength = 8; // bytes; 64 bits
    static final int keyLength = 256;
    static final String DOMAIN = "cryptocall.org";

    /**
     * Out of a phone number like "+491609999999", this produces a Base64 encoded
     * "salt+hash@cryptocall.org" using PBKDF2 with HMAC, SHA1
     * 
     * @param internationalPhoneNumber
     * @return
     */
    public static String generateProtectedEmail(String internationalPhoneNumber, byte[] salt) {
        Log.d(Constants.TAG, "input phone number: " + internationalPhoneNumber);

        // basic formatting
        internationalPhoneNumber = PhoneNumberUtils.formatNumber(internationalPhoneNumber);

        // strip whitespace, -, etc.
        internationalPhoneNumber = PhoneNumberUtils.stripSeparators(internationalPhoneNumber);

        Log.d(Constants.TAG, "formatted phone number: " + internationalPhoneNumber);

        // hash it!
        String output = null;

        String saltBase64 = Base64.encodeToString(salt, Base64.NO_PADDING | Base64.NO_WRAP
                | Base64.URL_SAFE);

        // http://nelenkov.blogspot.de/2012/04/using-password-based-encryption-on.html
        KeySpec keySpec = new PBEKeySpec(internationalPhoneNumber.toCharArray(), salt,
                iterationCount, keyLength);
        SecretKeyFactory keyFactory;
        try {
            keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

            byte[] keyBytes = keyFactory.generateSecret(keySpec).getEncoded();

            String keyBase64 = Base64.encodeToString(keyBytes, Base64.NO_PADDING | Base64.NO_WRAP
                    | Base64.URL_SAFE);

            // generate output as "salt+key"
            output = saltBase64 + "+" + keyBase64;
        } catch (NoSuchAlgorithmException e) {
            Log.e(Constants.TAG, "NoSuchAlgorithmException", e);
        } catch (InvalidKeySpecException e) {
            Log.e(Constants.TAG, "InvalidKeySpecException", e);
        }

        String email = output + "@" + DOMAIN;

        Log.d(Constants.TAG, "protected email: " + email);

        return email;
    }

    /**
     * Generates email using random salt. see generateProtectedEmail(String
     * internationalPhoneNumber, byte[] salt)
     * 
     * @param internationalPhoneNumber
     * @return
     */
    public static String generateProtectedEmail(String internationalPhoneNumber) {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[saltLength];
        random.nextBytes(salt);

        return generateProtectedEmail(internationalPhoneNumber, salt);
    }

    public static byte[] getSaltFromProtectedEmail(String email) {
        int atIndex = email.indexOf("@");
        String hasedPhoneNumber = email.substring(0, atIndex);
        
        int plusIndex = hasedPhoneNumber.indexOf("+");
        String saltStr = hasedPhoneNumber.substring(0, plusIndex);

        return Base64.decode(saltStr, Base64.NO_PADDING | Base64.NO_WRAP | Base64.URL_SAFE);
    }

}
