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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.cryptocall.util.Constants;

import android.util.Log;

public class CryptoCallSms {
    private String message;

    private String content;

    private String ip;
    private int port;
    private String extra; // TODO: can be used later

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    /* Static pattern and matcher */
    private static final String PRE_MESSAGE_MATCHER = "^" + Constants.SMS_PREFIX;
    private static Pattern mPreMessagePattern;
    private static Matcher mPreMessageMatcher;

    static {
        mPreMessagePattern = Pattern.compile(PRE_MESSAGE_MATCHER);
    }

    /**
     * Constructor
     */
    public CryptoCallSms(String message) {
        this.message = message;
    }

    /**
     * Checks if sms if cryptocall sms
     * 
     * @param message
     * @return true if is cryptocall sms
     */
    public static boolean isCryptoCallSms(String message) {
        mPreMessageMatcher = mPreMessagePattern.matcher(message);

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
     * @return returns true if it was successfully parsed as a cryptocall sms
     */
    public boolean parseMessage() {
        mPreMessageMatcher = mPreMessagePattern.matcher(message);

        // is CryptoCall SMS?
        if (mPreMessageMatcher.find()) {
            // get content
            content = mPreMessageMatcher.replaceFirst("");

            mPreMessageMatcher = mPreMessagePattern.matcher(content);
            int indexSeperator = -1;

            indexSeperator = content.indexOf(Constants.SMS_SEPERATOR);
            // if seperator found go on
            if (indexSeperator != -1) {
                // save ip and strip ip with seperator from message
                ip = content.substring(0, indexSeperator);
                content = content.substring(indexSeperator + 1);

                indexSeperator = content.indexOf(Constants.SMS_SEPERATOR);
                // if seperator found go on
                if (indexSeperator != -1) {
                    // save port and strip port with seperator from message
                    port = Integer.parseInt(content.substring(0, indexSeperator));
                    content = content.substring(indexSeperator + 1);

                    if (!content.equals("")) {
                        // extra is remaining part of message
                        extra = content;

                        // everything okay, return
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            Log.e(Constants.TAG, "No CryptoCall SMS!");
            return false;
        }
    }
}
