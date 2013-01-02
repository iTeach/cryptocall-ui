package org.cryptocall.ui;

import org.cryptocall.R;
import org.cryptocall.util.PreferencesHelper;
import org.thialfihar.android.apg.integration.ApgIntentHelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Window;

public class SmsReceivedActivity extends SherlockActivity {
    Activity mActivity;

    /**
     * Executed onCreate of Activity
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.sms_received_activity);

        mActivity = this;

    }
}
