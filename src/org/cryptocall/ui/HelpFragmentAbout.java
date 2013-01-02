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

import java.io.IOException;
import java.io.InputStream;

import org.cryptocall.R;
import org.cryptocall.util.Constants;
import org.cryptocall.util.Log;

import net.nightwhistler.htmlspanner.HtmlSpanner;
import net.nightwhistler.htmlspanner.JellyBeanSpanFixTextView;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;

public class HelpFragmentAbout extends SherlockFragment {

    /**
     * Workaround for Android Bug. See
     * http://stackoverflow.com/questions/8748064/starting-activity-from
     * -fragment-causes-nullpointerexception
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        setUserVisibleHint(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.help_fragment_about, container, false);

        // load html from html file from /res/raw
        InputStream inputStreamText = this.getActivity().getResources()
                .openRawResource(R.raw.help_about);

        TextView versionText = (TextView) view.findViewById(R.id.help_about_version);
        versionText.setText(getString(R.string.help_about_version) + " " + getVersion());

        JellyBeanSpanFixTextView aboutTextView = (JellyBeanSpanFixTextView) view
                .findViewById(R.id.help_about_text);

        // load html into textview
        HtmlSpanner htmlSpanner = new HtmlSpanner();
        htmlSpanner.setStripExtraWhiteSpace(true);
        try {
            aboutTextView.setText(htmlSpanner.fromHtml(inputStreamText));
        } catch (IOException e) {
            Log.e(Constants.TAG, "Error while reading raw resources as stream", e);
        }

        // make links work
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());

        // no flickering when clicking textview for Android < 4
        aboutTextView.setTextColor(getResources().getColor(
                android.R.color.secondary_text_dark_nodisable));

        return view;
    }

    /**
     * Get the current package version.
     * 
     * @return The current version.
     */
    private String getVersion() {
        String result = "";
        try {
            PackageManager manager = getActivity().getPackageManager();
            PackageInfo info = manager.getPackageInfo(getActivity().getPackageName(), 0);

            result = String.format("%s (%s)", info.versionName, info.versionCode);
        } catch (NameNotFoundException e) {
            Log.w(Constants.TAG, "Unable to get application version: " + e.getMessage());
            result = "Unable to get application version.";
        }

        return result;
    }

}