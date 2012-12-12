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
import org.cryptocall.util.Utils;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.ScrollView;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.MenuItem;

public class HelpActivity extends SherlockActivity implements ActionBar.TabListener {
    private SherlockActivity mActivity;
    private ActionBar mActionBar;

    private TextView mHelpText;
    private ScrollView mHelpScrollView;

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
     * Initialize layout with tabs
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mActivity = this;
        mActionBar = getSupportActionBar();

        setContentView(R.layout.help_activity);

        mHelpText = (TextView) findViewById(R.id.help_text);
        mHelpScrollView = (ScrollView) findViewById(R.id.help_scroll_view);

        mActionBar.setDisplayShowTitleEnabled(true);
        mActionBar.setDisplayHomeAsUpEnabled(true);

        mActionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.Tab tab1 = getSupportActionBar().newTab();
        ActionBar.Tab tab2 = getSupportActionBar().newTab();

        // tab names
        tab1.setText(getString(R.string.help_tab_faq));
        tab2.setText(getString(R.string.help_tab_problems));

        tab1.setTag("faq");
        tab2.setTag("problems");

        tab1.setTabListener(this);
        tab2.setTabListener(this);

        getSupportActionBar().addTab(tab1);
        getSupportActionBar().addTab(tab2);
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction transaction) {
    }

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction transaction) {
        // select resource based on selected tab
        int resourceToLoad = -1;
        if (tab.getTag().equals("faq")) {
            resourceToLoad = R.raw.help_faq;
        } else if (tab.getTag().equals("problems")) {
            resourceToLoad = R.raw.help_problems;
        }

        // load html from html file from /res/raw
        String helpText = Utils.readContentFromResource(mActivity, resourceToLoad);

        // set text from resources with html markup
        mHelpText.setText(Html.fromHtml(helpText));
        // make links work
        mHelpText.setMovementMethod(LinkMovementMethod.getInstance());

        // scroll to top
        mHelpScrollView.scrollTo(0, 0);
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction transaction) {
    }

}

// /**
// * Demonstrates combining the action bar with a ViewPager to implement a tab UI that switches
// * between tabs and also allows the user to perform horizontal flicks to move between the tabs.
// */
// public class HelpActivity extends FragmentActivity {
// ViewPager mViewPager;
// TabsAdapter mTabsAdapter;
//
// /**
// * Enabled Home Link in ActionBar
// */
// @Override
// protected void onStart() {
// super.onStart();
// ActionBar actionBar = this.getSupportActionBar();
// actionBar.setDisplayHomeAsUpEnabled(true);
// }
//
// /**
// * Initialize layout with tabs
// */
// @Override
// protected void onCreate(Bundle savedInstanceState) {
// super.onCreate(savedInstanceState);
//
// setContentView(R.layout.help_activity);
// getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
//
// ActionBar.Tab tab1 = getSupportActionBar().newTab();
// ActionBar.Tab tab2 = getSupportActionBar().newTab();
//
// // tab names
// tab1.setText(getString(R.string.help_tab_faq));
// tab2.setText(getString(R.string.help_tab_problems));
//
// mViewPager = (ViewPager) findViewById(R.id.help_activity_pager);
// mTabsAdapter = new TabsAdapter(this, getSupportActionBar(), mViewPager);
//
// // add tabs for faq and problems
// mTabsAdapter.addTab(tab1, HelpFaqFragment.class);
// mTabsAdapter.addTab(tab2, HelpProblemsFragment.class);
//
// if (savedInstanceState != null) {
// getSupportActionBar().setSelectedNavigationItem(savedInstanceState.getInt("index"));
// }
// }
//
// @Override
// protected void onSaveInstanceState(Bundle outState) {
// super.onSaveInstanceState(outState);
// outState.putInt("index", getSupportActionBar().getSelectedNavigationIndex());
// }
//
// /**
// * Menu Items
// */
// @Override
// public boolean onOptionsItemSelected(MenuItem item) {
// switch (item.getItemId()) {
// case android.R.id.home:
// // app icon in Action Bar clicked; go home
// Intent intent = new Intent(this, BaseActivity.class);
// intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
// startActivity(intent);
// return true;
// default:
// return super.onOptionsItemSelected(item);
// }
// }
//
// /**
// * This is a helper class that implements the management of tabs and all details of connecting a
// * ViewPager with associated TabHost. It relies on a trick. Normally a tab host has a simple API
// * for supplying a View or Intent that each tab will show. This is not sufficient for switching
// * between pages. So instead we make the content part of the tab host 0dp high (it is not shown)
// * and the TabsAdapter supplies its own dummy view to show as the tab content. It listens to
// * changes in tabs, and takes care of switch to the correct paged in the ViewPager whenever the
// * selected tab changes.
// */
// public static class TabsAdapter extends FragmentPagerAdapter implements
// ViewPager.OnPageChangeListener, ActionBar.TabListener {
// private final Context mContext;
// private final ActionBar mActionBar;
// private final ViewPager mViewPager;
// private final ArrayList<String> mTabs = new ArrayList<String>();
//
// public TabsAdapter(FragmentActivity activity, ActionBar actionBar, ViewPager pager) {
// super(activity.getSupportFragmentManager());
// mContext = activity;
// mActionBar = actionBar;
// mViewPager = pager;
// mViewPager.setAdapter(this);
// mViewPager.setOnPageChangeListener(this);
// }
//
// public void addTab(ActionBar.Tab tab, Class<?> clss) {
// mTabs.add(clss.getName());
// mActionBar.addTab(tab.setTabListener(this));
// notifyDataSetChanged();
// }
//
// @Override
// public int getCount() {
// return mTabs.size();
// }
//
// @Override
// public Fragment getItem(int position) {
// return Fragment.instantiate(mContext, mTabs.get(position), null);
// }
//
// @Override
// public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
// }
//
// @Override
// public void onPageSelected(int position) {
// mActionBar.setSelectedNavigationItem(position);
// }
//
// @Override
// public void onPageScrollStateChanged(int state) {
// }
//
// @Override
// public void onTabSelected(Tab tab, FragmentTransaction ft) {
// mViewPager.setCurrentItem(tab.getPosition());
// }
//
// @Override
// public void onTabReselected(Tab tab, FragmentTransaction ft) {
// }
//
// @Override
// public void onTabUnselected(Tab tab, FragmentTransaction ft) {
// }
// }
// }
