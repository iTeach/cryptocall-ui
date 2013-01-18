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

package org.cryptocall.ui;

import org.cryptocall.R;
import org.cryptocall.syncadapter.ContactsSyncAdapterService.CryptoCallContract;
import org.cryptocall.util.Constants;
import org.cryptocall.util.Log;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

public class ShowContactActivity extends SherlockActivity {
    private SherlockActivity mActivity;

    private String mDisplayName;
    private String mNickname;
    private String mTelephoneNumber;
    private String mLookupKey;

    private TextView mNicknameTextView;
    private TextView mTelephoneNumberTextView;
    private ActionBar mActionBar;

    /**
     * Instantiate View and initialize fragments for this Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.show_contact_activity);

        mActivity = this;

        Uri uri = getIntent().getData();
        if (uri != null) {

            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor.moveToFirst()) {
                mDisplayName = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
                mNickname = cursor.getString(cursor
                        .getColumnIndex(CryptoCallContract.DATA3_KEYRING_NICKNAME));
                mTelephoneNumber = cursor.getString(cursor
                        .getColumnIndex(CryptoCallContract.DATA4_TELEPHONE_NUMBER));
                mLookupKey = cursor.getString(cursor.getColumnIndex(Contacts.LOOKUP_KEY));

                // get views
                mNicknameTextView = (TextView) findViewById(R.id.show_contact_nickname);
                mTelephoneNumberTextView = (TextView) findViewById(R.id.show_contact_number);
                mActionBar = getSupportActionBar();

                // set views
                mActionBar.setTitle(mDisplayName);
                mNicknameTextView.setText(mNickname);
                mTelephoneNumberTextView.setText(mTelephoneNumber);
            }
        } else {
            Log.e(Constants.TAG, "getIntent().getData() is null!");
            finish();
        }
    }

    /**
     * Enabled Home Link in ActionBar
     */
    @Override
    protected void onStart() {
        super.onStart();
        ActionBar actionBar = this.getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
    }

    /**
     * Menu Items
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case android.R.id.home:
            // app icon in Action Bar clicked; go home
            Intent intent = new Intent(this, BaseActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    /**
     * hand over onClick events, defined in layout from Activity to Fragment
     */
    public void callOnClick(View view) {
        // start sending sms activity
        Intent activityIntent = new Intent();
        activityIntent.setClass(mActivity, SmsSendingActivity.class);
        // TODO use masterKeyId
        activityIntent.putExtra(SmsSendingActivity.EXTRA_CRYPTOCALL_EMAIL, mTelephoneNumber);
        mActivity.startActivity(activityIntent);
    }

    /**
     * hand over onClick events, defined in layout from Activity to Fragment
     */
    public void androidContactOnClick(View view) {
        Uri look = Uri.withAppendedPath(Contacts.CONTENT_LOOKUP_URI, mLookupKey);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(look);

        startActivity(intent);
    }
}
