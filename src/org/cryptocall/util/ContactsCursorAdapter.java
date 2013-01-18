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

import org.cryptocall.R;
import org.cryptocall.syncadapter.ContactsSyncAdapterService.CryptoCallContract;
import org.cryptocall.ui.SmsSendingActivity;
import org.sufficientlysecure.keychain.integration.KeychainContentProviderHelper;
import org.sufficientlysecure.keychain.integration.KeychainUtil;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class ContactsCursorAdapter extends SimpleCursorAdapter {
    private int layout;

    public ContactsCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to,
            int flags) {
        super(context, layout, c, from, to, flags);
        this.layout = layout;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(layout, parent, false);

        return v;
    }

    /**
     * Bind cursor to view with extra binding of call ImageButton
     */
    @Override
    public void bindView(View v, Context context, Cursor c) {
        super.bindView(v, context, c);

        // extra binding for ImageButton
        ImageButton callImageButton = (ImageButton) v
                .findViewById(R.id.base_contacts_list_call_image);

        // bind email from cursor to tag of imageButton in list item
        final int masterKeyIdColumnIndex = c.getColumnIndex(CryptoCallContract.DATA1_MASTER_KEY_ID);
        Long masterKeyId = c.getLong(masterKeyIdColumnIndex);
        callImageButton.setTag(masterKeyId);

        callImageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Context context = v.getContext();
                // get email from tag

                // TODO: directly use masterKeyId in SmsSendingActivity?
                Long masterKeyId = (Long) v.getTag();
                String userId = (new KeychainContentProviderHelper(context)).getUserId(masterKeyId,
                        false);
                String email = KeychainUtil.splitUserId(userId)[1];
                Log.d(Constants.TAG, "email: " + email);

                // start sending sms activity
                Intent activityIntent = new Intent();
                activityIntent.setClass(context, SmsSendingActivity.class);
                activityIntent.putExtra(SmsSendingActivity.EXTRA_CRYPTOCALL_EMAIL, email);
                context.startActivity(activityIntent);
            }
        });
    }
}