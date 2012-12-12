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

package org.cryptocall.ui;

import org.cryptocall.CryptoCallApplication;
import org.cryptocall.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.support.v4.app.FragmentManager;
import android.view.View;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

public class ShowContactActivity extends SherlockActivity {

    // Intent Extra Variables
    public static final String INTENT_NAME = "NAME";
    public static final String INTENT_EMAIL = "EMAIL";
    public static final String INTENT_LOOKUP_KEY = "LOOKUP_KEY";

    private SherlockActivity mActivity;

    private String mName;
    private String mEmail;
    private String mLookupKey;

    private TextView mEmailTextView;
    private ActionBar mActionBar;

    /**
     * Instantiate View and initialize fragments for this Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.show_contact_activity);

        mActivity = this;

        /* should be called with parameters */
        Bundle extras = mActivity.getIntent().getExtras();
        if (extras != null) {
            mName = extras.getString(INTENT_NAME);
            mEmail = extras.getString(INTENT_EMAIL);
            mLookupKey = extras.getString(INTENT_LOOKUP_KEY);

            // get views
            mEmailTextView = (TextView) findViewById(R.id.show_contact_email);
            mActionBar = getSupportActionBar();

            // set views
            mActionBar.setTitle(mName);
            mEmailTextView.setText(mEmail);
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
        // TODO: start intent
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
