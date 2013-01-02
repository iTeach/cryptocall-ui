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

import org.cryptocall.R;
import org.cryptocall.util.Constants;
import org.cryptocall.util.ContactsCursorAdapter;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;

import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.app.LoaderManager;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.Contacts;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

/**
 * Demonstration of the use of a CursorLoader to load and display contacts data in a fragment.
 */
public class ContactsFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

    // This is the Adapter being used to display the list's data.
    ContactsCursorAdapter mAdapter;

    // If non-null, this is the current filter the user has provided.
    String mCurFilter;

    Activity mActivity;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mActivity = getActivity();

        // Give some text to display if there is no data. In a real
        // application this would come from a resource.
        setEmptyText(getString(R.string.base_contacts_list_emty));

        // We have a menu item to show in action bar.
        setHasOptionsMenu(true);

        // Create an empty adapter we will use to display the loaded data.
        // mAdapter = new SimpleCursorAdapter(getActivity(), android.R.layout.simple_list_item_2,
        // null, new String[] { Contacts.DISPLAY_NAME, Contacts.CONTACT_STATUS }, new int[] {
        // android.R.id.text1, android.R.id.text2 }, 0);

        mAdapter = new ContactsCursorAdapter(mActivity, R.layout.base_contacts_list_item, null,
                new String[] { Contacts.DISPLAY_NAME, Email.DATA }, new int[] {
                        R.id.base_contacts_list_name, R.id.base_contacts_list_email }, 0);
        setListAdapter(mAdapter);

        // set design to fast scroll, meaning if many items are available show scroll slider
        getListView().setFastScrollEnabled(true);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader. Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Place an action bar item for searching.
        // MenuItem item = menu.add("Search");
        // item.setIcon(android.R.drawable.ic_menu_search);
        // item.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
        // SearchView sv = new SearchView(getActivity());
        // sv.setOnQueryTextListener(this);
        // item.setActionView(sv);
    }

    public boolean onQueryTextChange(String newText) {
        // Called when the action bar search text has changed. Update
        // the search filter, and restart the loader to do a new query
        // with this filter.
        mCurFilter = !TextUtils.isEmpty(newText) ? newText : null;
        getLoaderManager().restartLoader(0, null, this);
        return true;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        // Insert desired behavior here.
        Log.i("FragmentComplexList", "Item clicked: " + id);

        Cursor cursor = mAdapter.getCursor();

        final int nameColumnIndex = cursor.getColumnIndex(Contacts.DISPLAY_NAME);
        String name = cursor.getString(nameColumnIndex);

        final int emailColumnIndex = cursor.getColumnIndex(Email.DATA);
        String email = cursor.getString(emailColumnIndex);

        final int lookupKeyColumnIndex = cursor.getColumnIndex(Contacts.LOOKUP_KEY);
        String lookupKey = cursor.getString(lookupKeyColumnIndex);

        // start show Contact
        Intent activityIntent = new Intent();
        activityIntent.setClass(mActivity, ShowContactActivity.class);
        activityIntent.putExtra(ShowContactActivity.EXTRA_NAME, name);
        activityIntent.putExtra(ShowContactActivity.EXTRA_EMAIL, email);
        activityIntent.putExtra(ShowContactActivity.EXTRA_LOOKUP_KEY, lookupKey);
        mActivity.startActivity(activityIntent);
    }

    // These are the Contacts rows that we will retrieve.
    // static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] { Contacts._ID,
    // Contacts.DISPLAY_NAME, Contacts.CONTACT_STATUS, Contacts.CONTACT_PRESENCE,
    // Contacts.PHOTO_ID, Contacts.LOOKUP_KEY, };

    static final String[] CONTACTS_SUMMARY_PROJECTION = new String[] { Contacts._ID,
            Contacts.DISPLAY_NAME, Email.DATA, Contacts.LOOKUP_KEY };

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created. This
        // sample only has one Loader, so we don't care about the ID.
        // First, pick the base URI to use depending on whether we are
        // currently filtering.
        // Uri baseUri;
        // if (mCurFilter != null) {
        // baseUri = Uri.withAppendedPath(Contacts.CONTENT_FILTER_URI, Uri.encode(mCurFilter));
        // } else {
        // baseUri = Contacts.CONTENT_URI;
        // }

        // Now create and return a CursorLoader that will take care of
        // creating a Cursor for the data being displayed.
        // String select = "((" + Contacts.DISPLAY_NAME + " NOTNULL) AND ("
        // + Contacts.HAS_PHONE_NUMBER + "=1) AND (" + Contacts.DISPLAY_NAME + " != '' ))";
        // return new CursorLoader(getActivity(), baseUri, CONTACTS_SUMMARY_PROJECTION, select,
        // null,
        // Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");

        // String select = "((" + Contacts.DISPLAY_NAME + " NOTNULL))";

        // select only Contacts with email like 12345@cryptocall.org
        String select = Email.DATA + " LIKE '%" + Constants.CRYPTOCALL_DOMAIN + "'";

        return new CursorLoader(mActivity, Email.CONTENT_URI, CONTACTS_SUMMARY_PROJECTION, select,
                null, Contacts.DISPLAY_NAME + " COLLATE LOCALIZED ASC");
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Swap the new cursor in. (The framework will take care of closing the
        // old cursor once we return.)
        mAdapter.swapCursor(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // This is called when the last Cursor provided to onLoadFinished()
        // above is about to be closed. We need to make sure we are no
        // longer using it.
        mAdapter.swapCursor(null);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // The CursorLoader example doesn't do this, but if we get an update while the UI is
        // destroyed, it will crash. Why is this necessary?
        // http://stackoverflow.com/questions/6757156/android-cursorloader-crash-on-non-topmost-fragment
        getLoaderManager().destroyLoader(0);
    }

}