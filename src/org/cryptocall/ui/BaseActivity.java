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
import org.cryptocall.util.Constants;
import org.cryptocall.util.ContactsUtils;
import org.cryptocall.util.PreferencesHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class BaseActivity extends SherlockFragmentActivity {
    private Activity mActivity;

    private ActionBar mActionBar;
    private ActionBar.Tab mTabContacts;
    private ActionBar.Tab mTabInformation;
    private ActionBar.Tab mTabManualConnection;

    /**
     * Inflate Menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.base, menu);
        return true;
    }

    /**
     * Menu Options
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {

        case R.id.base_menu_sync:
            syncContacts();
            return true;

        case R.id.base_menu_help:
            startActivity(new Intent(mActivity, HelpActivity.class));
            return true;

        case R.id.base_menu_preferences:
            startActivity(new Intent(mActivity, WizardActivity.class));
            return true;

        default:
            return super.onOptionsItemSelected(item);
        }
    }

    private void syncContacts() {
        AsyncTask<Void, Void, Void> sync = new AsyncTask<Void, Void, Void>() {

            @Override
            protected Void doInBackground(Void... unused) {
                ContactsUtils.syncContacts(mActivity);

                // return nothing as type is Void
                return null;
            }

            @Override
            protected void onPreExecute() {
                super.onPreExecute();

                setProgressBarIndeterminateVisibility(Boolean.TRUE);
            }

            @Override
            protected void onPostExecute(Void unused) {
                super.onPostExecute(unused);

                setProgressBarIndeterminateVisibility(Boolean.FALSE);
            }
        };

        sync.execute();
    }

    /**
     * Executed onCreate of Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Start wizard for first use
        if (PreferencesHelper.getFirstStart(this)) {
            Intent wizardIntent = new Intent(this, WizardActivity.class);
            wizardIntent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivity(wizardIntent);
            finish();
        }

        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);

        setContentView(R.layout.base_activity);

        mActivity = this;

        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(false);
        mActionBar.setHomeButtonEnabled(false);
        mActionBar.setSubtitle(R.string.app_subtitle);

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mTabContacts = getSupportActionBar().newTab();
        mTabInformation = getSupportActionBar().newTab();

        mTabContacts.setTabListener(new TabListener<ContactsFragment>(this, "contacts",
                ContactsFragment.class));
        mTabInformation.setTabListener(new TabListener<BaseInformationFragment>(this,
                "information", BaseInformationFragment.class));

        mTabContacts.setText(getString(R.string.base_tab_contacts));
        mTabInformation.setText(getString(R.string.base_tab_information));

        mActionBar.addTab(mTabContacts);
        mActionBar.addTab(mTabInformation);

        // add manual connection fragment when in debug mode
        if (Constants.DEBUG) {
            mTabManualConnection = getSupportActionBar().newTab();
            mTabManualConnection.setTabListener(new TabListener<BaseManualConnectionFragment>(this,
                    "manual", BaseManualConnectionFragment.class));
            mTabManualConnection.setText(getString(R.string.base_tab_manual_connection));
            mActionBar.addTab(mTabManualConnection);
        }
    }

    private static class TabListener<T extends Fragment> implements ActionBar.TabListener {
        private Fragment mFragment;
        private final Activity mActivity;
        private final String mTag;
        private final Class<T> mClass;

        /**
         * Constructor used each time a new tab is created.
         * 
         * @param activity
         *            The host Activity, used to instantiate the fragment
         * @param tag
         *            The identifier tag for the fragment
         * @param clz
         *            The fragment's Class, used to instantiate the fragment
         */
        public TabListener(Activity activity, String tag, Class<T> clz) {
            mActivity = activity;
            mTag = tag;
            mClass = clz;
        }

        @Override
        public void onTabReselected(Tab tab, FragmentTransaction ft) {
        }

        /**
         * Open Fragment based on selected Tab
         */
        @Override
        public void onTabSelected(Tab tab, FragmentTransaction ignoredFt) {
            // bug in compatibility lib:
            // http://stackoverflow.com/questions/8645549/null-fragmenttransaction-being-passed-to-tablistener-ontabselected
            FragmentManager fragMgr = ((FragmentActivity) mActivity).getSupportFragmentManager();
            FragmentTransaction ft = fragMgr.beginTransaction();

            mFragment = Fragment.instantiate(mActivity, mClass.getName());
            ft.replace(R.id.base_activity_tabs_container, mFragment, mTag);
            ft.commit();
        }

        @Override
        public void onTabUnselected(Tab tab, FragmentTransaction ignoredFt) {
            FragmentManager fragMgr = ((FragmentActivity) mActivity).getSupportFragmentManager();
            FragmentTransaction ft = fragMgr.beginTransaction();

            if (mFragment != null) {
                // Remove the fragment
                ft.remove(mFragment);
            }

            ft.commit();
        }
    }
}
