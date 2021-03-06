package com.adi.ho.jackie.emailapp;

import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.DisplayMetrics;
import android.view.View;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.ActionBar;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.adi.ho.jackie.emailapp.database.MailDatabaseOpenHelper;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import java.util.HashMap;

/**
 * An activity representing a single EmailItem detail screen. This
 * activity is only used narrow width devices. On tablet-size devices,
 * item details are presented side-by-side with a list of items
 * in a {@link EmailItemListActivity}.
 */
public class EmailItemDetailActivity extends AppCompatActivity {

    private String mRecipient;
    private String mSender;
    private String mDate;
    private String mBody;
    private String mId;
    private String mSubject;

    public TextView mSenderText;
    public TextView mRecipientText;
    public TextView mDateText;
    public TextView mBodyText;
    public TextView mSubjectText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_emailitem_detail);
        Toolbar toolbar = (Toolbar) findViewById(R.id.detail_toolbar);
        setSupportActionBar(toolbar);

        mBodyText = (TextView) findViewById(R.id.email_body_detail);
        mDateText = (TextView) findViewById(R.id.email_date_detail);
        mRecipientText = (TextView) findViewById(R.id.email_recipient_detail);
        mSenderText = (TextView) findViewById(R.id.email_sender_detail);
        mSubjectText = (TextView)findViewById(R.id.email_subject_detail);

        mId = getIntent().getStringExtra("EMAILID");


        // Show the Up button in the action bar.
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        new GetEmailFromDBWithIdAsyncTask().execute(mId);

        // savedInstanceState is non-null when there is fragment state
        // saved from previous configurations of this activity
        // (e.g. when rotating the screen from portrait to landscape).
        // In this case, the fragment will automatically be re-added
        // to its container so we don't need to manually add it.
        // For more information, see the Fragments API guide at:
        //
        // http://developer.android.com/guide/components/fragments.html
        //
        if (savedInstanceState == null && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            // Create the detail fragment and add it to the activity
            // using a fragment transaction.
            Bundle arguments = new Bundle();
            arguments.putString("ID",mId);
            EmailItemDetailFragment fragment = new EmailItemDetailFragment();
            fragment.setArguments(arguments);
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.emailitem_detail_container, fragment)
                    .commit();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button. In the case of this
            // activity, the Up button is shown. For
            // more details, see the Navigation pattern on Android Design:
            //
            // http://developer.android.com/design/patterns/navigation.html#up-vs-back
            //
            navigateUpTo(new Intent(this, EmailItemListActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private class GetEmailFromDBWithIdAsyncTask extends AsyncTask<String, Void, HashMap<String, String>> {
        MailDatabaseOpenHelper helper;
        HashMap<String, String> emailIdContents;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            helper = MailDatabaseOpenHelper.getInstance(EmailItemDetailActivity.this);
            emailIdContents = new HashMap<>();
        }

        @Override
        protected HashMap<String, String> doInBackground(String... params) {
            String id = params[0];
            Cursor cursor = helper.getEmailById(id);
            cursor.moveToFirst();
            emailIdContents.put("RECIPIENT", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_RECIPIENT)));
            emailIdContents.put("SENDER", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_SENDER)));
            emailIdContents.put("BODY", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_BODY)));
            emailIdContents.put("DATE", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_DATE)));
            emailIdContents.put("SUBJECT", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_SUBJECT)));
            cursor.close();
            return emailIdContents;
        }

        @Override
        protected void onPostExecute(HashMap hashMap) {
            mBody = hashMap.get("BODY").toString();
            mDate = hashMap.get("DATE").toString();
            mRecipient = hashMap.get("RECIPIENT").toString();
            mSender = hashMap.get("SENDER").toString();
            mSubject = hashMap.get("SUBJECT").toString();


            mSenderText.setText(mSender);
            mRecipientText.setText(mRecipient);
            mDateText.setText("Date Received: " + mDate);
            mSubjectText.setText(mSubject);
            if (mBody.contains("html")) {
                mBodyText.setText(Html.fromHtml(mBody));
            } else {
                mBodyText.setText(mBody);
            }

        }
    }
}
