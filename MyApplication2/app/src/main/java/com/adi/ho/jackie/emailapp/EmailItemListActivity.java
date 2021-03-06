package com.adi.ho.jackie.emailapp;

import android.accounts.AccountManager;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.adi.ho.jackie.emailapp.Fragments.ComposeFragment;
import com.adi.ho.jackie.emailapp.database.MailDatabaseOpenHelper;
import com.adi.ho.jackie.emailapp.recyclerlistitems.DividerItemDecoration;
import com.adi.ho.jackie.emailapp.recyclerlistitems.EmailRecyclerAdapter;
import com.adi.ho.jackie.emailapp.recyclerlistitems.EmailViewHolder;
import com.adi.ho.jackie.emailapp.recyclerlistitems.VerticalSpaceItemDecoration;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.api.client.auth.openidconnect.IdToken;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.repackaged.org.apache.commons.codec.binary.StringUtils;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailRequest;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Draft;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.ListMessagesResponse;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.jsoup.Jsoup;

import javax.net.ssl.HttpsURLConnection;

/**
 * An activity representing a list of EmailItems. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a {@link EmailItemDetailActivity} representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
public class EmailItemListActivity extends AppCompatActivity implements ComposeFragment.SendEmailTaskListener, EmailViewHolder.MakeSecondFragmentListener, ComposeFragment.SaveDraftsListener {
    public GoogleAccountCredential mCredential;
    ProgressDialog mProgress;
    EmailRecyclerAdapter mEmailRecyclerAdapter;
    private List<String> mEmailIdsList;
    private ArrayList<Message> mEmailMessages;
    SearchView searchView;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {GmailScopes.GMAIL_READONLY, GmailScopes.GMAIL_COMPOSE, GmailScopes.GMAIL_INSERT, GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_MODIFY, GmailScopes.GMAIL_SEND, GmailScopes.MAIL_GOOGLE_COM};
    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    public boolean mTwoPane;
    RecyclerView emaillistRecycler;
    private MailDatabaseOpenHelper mHelper;
    private EmailRecyclerAdapter emailRecyclerAdapter;
    private List<Email> mRecyclerViewList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Gmail API ...");
        setContentView(R.layout.activity_emailitem_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle(getTitle());
        mEmailMessages = new ArrayList<>();
        mRecyclerViewList = new ArrayList<>();
        emaillistRecycler = (RecyclerView) findViewById(R.id.emailitem_list);
        final FrameLayout composeFragmentContainer = (FrameLayout) findViewById(R.id.compose_fragment_container);
        final FrameLayout frameLayout = (FrameLayout) findViewById(R.id.frameLayout);


        final FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ComposeFragment composeFragment = new ComposeFragment();
                // fragmentTransaction.commit();
                android.app.FragmentManager fm = getFragmentManager();
                composeFragment.show(fm, "Compose");
            }
        });


        if (findViewById(R.id.emailitem_detail_container) != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            mTwoPane = true;
        }


        // Initialize credentials and service object.
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setOnQueryTextListener(queryListener);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_refresh) {
            if (isGooglePlayServicesAvailable()) {
                refreshResults();
            }
        } else if (id == R.id.action_logout) {

            List<Email> emptyList = new ArrayList<>();
            mEmailRecyclerAdapter = new EmailRecyclerAdapter(EmailItemListActivity.this, mRecyclerViewList);
            emaillistRecycler.setAdapter(emailRecyclerAdapter);
            mEmailRecyclerAdapter.animateTo(emptyList);
            chooseAccount();
            mHelper.clearDb();
            if (isGooglePlayServicesAvailable()) {
                refreshResults();
            }
    } else if (id == R.id.action_search) {

        }

        return super.onOptionsItemSelected(item);
    }


    SearchView.OnQueryTextListener queryListener = new SearchView.OnQueryTextListener() {
        @Override
        public boolean onQueryTextSubmit(String query) {
            if (query.trim().isEmpty() || query == null){
                new LoadEmailsFromDbAsyncTask().execute();
            } else {
                new SearchEmailAsyncTask().execute(query);
            }
            return true;
        }

        @Override
        public boolean onQueryTextChange(String newText) {
            return false;
        }
    };


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    isGooglePlayServicesAvailable();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        mCredential.setSelectedAccountName(accountName);
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(EmailItemListActivity.this, "Account unspecified", Toast.LENGTH_SHORT).show();
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode != RESULT_OK) {
                    chooseAccount();
                }
                break;
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void refreshResults() {
        if (mCredential.getSelectedAccountName() == null) {
            chooseAccount();

        } else {
            if (isDeviceOnline()) {
                new DownloadEmailAsyncTask(mCredential).execute();
                //new MakeRequestTask(mCredential).execute();

            } else {
                Toast.makeText(EmailItemListActivity.this, "No network connection", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void chooseAccount() {
        startActivityForResult(
                mCredential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);

    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    @Override
    protected void onResume() {
        super.onResume();
        //Check if db is empty
        mHelper = MailDatabaseOpenHelper.getInstance(EmailItemListActivity.this);
        String count = "SELECT count(*) FROM EMAILS";
        Cursor cursorCount = mHelper.getReadableDatabase().rawQuery(count, null);
        cursorCount.moveToFirst();
        int icount = cursorCount.getInt(0);

        if (isGooglePlayServicesAvailable() && icount == 0) {
            refreshResults();
        } else if (icount > 0) {
            new LoadEmailsFromDbAsyncTask().execute();
        }
    }

    private boolean isGooglePlayServicesAvailable() {
        final int connectionStatusCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (GooglePlayServicesUtil.isUserRecoverableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
            return false;
        } else if (connectionStatusCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;
    }

    /**
     * Display an error dialog showing that Google Play Services is missing
     * or out of date.
     *
     * @param connectionStatusCode code describing the presence (or lack of)
     *                             Google Play Services on this device.
     */
    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
                connectionStatusCode,
                EmailItemListActivity.this,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    public void sendEmail(HashMap<String, String> hashMap) {
        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
        mCredential = GoogleAccountCredential.usingOAuth2(
                getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff())
                .setSelectedAccountName(settings.getString(PREF_ACCOUNT_NAME, null));
        new SendEmailAsyncTask(mCredential).execute(hashMap);
    }

    @Override
    public void makeSecondFragment(String emailId) {

        //For two panes
        Bundle bundle = new Bundle();
        bundle.putString("ID", emailId);
        EmailItemDetailFragment emailItemDetailFragment = new EmailItemDetailFragment();
        emailItemDetailFragment.setArguments(bundle);
        FragmentManager fragmentManager = getSupportFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.emailitem_detail_container, emailItemDetailFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private class MakeRequestTask extends AsyncTask<ArrayList<String>, Email, List<Email>> {
        private com.google.api.services.gmail.Gmail mService = null;
        private Exception mLastError = null;
        private HashMap<String, String> emailHash;
        String date;
        private GoogleAccountCredential usercred;

        public MakeRequestTask(GoogleAccountCredential credential) {

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            com.google.api.client.json.JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Gmail API Android")
                    .build();
            usercred = credential;
            emailHash = new HashMap<>();

        }

        @Override
        protected List<Email> doInBackground(ArrayList<String>... params) {

            List<Email> emailHeaderList = new ArrayList<>();
            ArrayList<String> idStrings = new ArrayList<>();
            idStrings = params[0];
            try {
                for (String id : idStrings) {
                    Message message = mService.users().messages().get("me", id).execute();
                    for (MessagePartHeader messagePartHeader : message.getPayload().getHeaders()) {

                        if (messagePartHeader.getName().equals("From")) {
                            String sender = messagePartHeader.getValue();
                            emailHash.put("SENDER", sender);
                        }
                        if (messagePartHeader.getName().equals("Date")) {
                            date = messagePartHeader.getValue().substring(0, 11);
                            emailHash.put("DATE", date);
                        }

                        if (messagePartHeader.getName().equals("Delivered-To")) {
                            emailHash.put("RECIPIENT", messagePartHeader.getValue());
                        }
                        if (messagePartHeader.getName().equals("Subject")) {
                            emailHash.put("SUBJECT", messagePartHeader.getValue());
                        }
                    }


                    MessagePart firstMessagePart;
                    if (message.getPayload().getParts() != null) {
                        firstMessagePart = message.getPayload().getParts().get(0);
                        String emailBody = StringUtils.newStringUtf8(Base64.decodeBase64(firstMessagePart.getBody().getData()));
                        emailHash.put("BODY", emailBody);
                    } else {
                        String emailBody = StringUtils.newStringUtf8(Base64.decodeBase64(message.getPayload().getBody().getData()));
                        emailHash.put("BODY", emailBody);

                    }

                    emailHash.put("SNIPPET", message.getSnippet());
                    emailHash.put("ID", id);
                    emailHash.put("DRAFT", "0");

                    Email email = new Email(emailHash);
                    emailHeaderList.add(email);
                    publishProgress(email);

                    Log.i("EMAILS", "Added email id: " + id);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return emailHeaderList;
        }

        //Insert each email into database
        @Override
        protected void onProgressUpdate(Email... values) {
            super.onProgressUpdate(values);
            new InsertEmailDBAsyncTask().execute(values[0]);
        }

        @Override
        protected void onPreExecute() {
            //   mOutputText.setText("");
            mProgress.show();
        }

        @Override
        protected void onPostExecute(List<Email> output) {
            mProgress.hide();
            if (output == null || output.size() == 0) {
                Toast.makeText(EmailItemListActivity.this, "No emails", Toast.LENGTH_SHORT).show();
                //  mOutputText.setText("No results returned.");
            } else {
                setRecyclerView(output);
                //mOutputText.setText(TextUtils.join("\n", output));
            }
        }

        @Override
        protected void onCancelled() {
            mProgress.hide();
            if (mLastError != null) {
                if (mLastError instanceof GooglePlayServicesAvailabilityIOException) {
                    showGooglePlayServicesAvailabilityErrorDialog(
                            ((GooglePlayServicesAvailabilityIOException) mLastError)
                                    .getConnectionStatusCode());
                } else if (mLastError instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(
                            ((UserRecoverableAuthIOException) mLastError).getIntent(),
                            EmailItemListActivity.REQUEST_AUTHORIZATION);
                } else {
                    // mOutputText.setText("The following error occurred:\n"
//                            + mLastError.getMessage());
                }
            } else {
                //              mOutputText.setText("Request cancelled.");
            }
        }
    }

    private class DownloadEmailAsyncTask extends AsyncTask<Void, Void, ArrayList<String>> {
        private com.google.api.services.gmail.Gmail mService = null;
        String emailIdData;
        ArrayList<String> emailIds;

        public DownloadEmailAsyncTask(GoogleAccountCredential credential) {

            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            com.google.api.client.json.JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Gmail API Android")
                    .build();
            emailIds = new ArrayList<>();
        }

        @Override
        protected ArrayList<String> doInBackground(Void... params) {
            try {
                ArrayList<Message> messageList = new ArrayList<>();
                List<String> labelIds = new ArrayList<>();
                labelIds.add("INBOX"); //Retrieve inbox messages only
                ListMessagesResponse messages = mService.users().messages().list("me").setLabelIds(labelIds).execute();

                for (int i = 0; i < 100; i++) {
                    messageList.add(messages.getMessages().get(i));
                }

                for (Message message : messageList) {
                    emailIds.add(message.getId());
                    //mEmailMessages.add(message);
                }

                int i = 2;

            } catch (Throwable tho) {
                if (tho instanceof UserRecoverableAuthIOException) {
                    startActivityForResult(((UserRecoverableAuthIOException) tho).getIntent(), REQUEST_AUTHORIZATION);
                } else {
                    tho.printStackTrace();
                }
            }

            return emailIds;
        }

        @Override
        protected void onPostExecute(ArrayList<String> ids) {
            //Send ids to list
            mEmailIdsList = ids;
            new MakeRequestTask(mCredential).execute(ids);

        }
    }

    //Save emails to database
    private class InsertEmailDBAsyncTask extends AsyncTask<Email, Void, Void> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            //mHelper = MailDatabaseOpenHelper.getInstance(EmailItemListActivity.this);

        }

        @Override
        protected Void doInBackground(Email... params) {
            mHelper.addEmailsToDatabase(params[0]);
            return null;
        }
    }

    private class LoadEmailsFromDbAsyncTask extends AsyncTask<Void, Void, List<Email>> {
        HashMap<String, String> emailHashFromDb;
        List<Email> emailListFromDb;

        public LoadEmailsFromDbAsyncTask() {
            emailHashFromDb = new HashMap<>();
            emailListFromDb = new ArrayList<>();
        }

        @Override
        protected List<Email> doInBackground(Void... params) {
            //mHelper = MailDatabaseOpenHelper.getInstance(EmailItemListActivity.this);
            Cursor cursor = mHelper.getAllEmailsFromDb();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                emailHashFromDb.clear();
                emailHashFromDb.put("SENDER", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_SENDER)));
                emailHashFromDb.put("DATE", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_DATE)));
                emailHashFromDb.put("SUBJECT", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_SUBJECT)));
                emailHashFromDb.put("ID", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_ID)));
                emailHashFromDb.put("SNIPPET", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_SNIPPET)));
                emailHashFromDb.put("DRAFT", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_DRAFT)));
                emailListFromDb.add(new Email(emailHashFromDb));
                cursor.moveToNext();
            }
            cursor.close();
            return emailListFromDb;
        }

        @Override
        protected void onPostExecute(List<Email> emailList) {
//            mCursor.close();
            if (emailList == null || emailList.size() == 0) {
                //TODO: display message
            } else {
                setRecyclerView(emailList);
            }
        }
    }

    private class SendEmailAsyncTask extends AsyncTask<HashMap, Void, Void> {
        private com.google.api.services.gmail.Gmail mService = null;
        private GoogleAccountCredential googleCredentials;

        public SendEmailAsyncTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            com.google.api.client.json.JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Gmail API Android")
                    .build();
            googleCredentials = credential;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Snackbar.make(findViewById(android.R.id.content), "Sending", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }

        @Override
        protected Void doInBackground(HashMap... params) {
            HashMap<String, String> emailContents = params[0];
            String recipient = emailContents.get("RECIPIENT");
            String subject = emailContents.get("SUBJECT");
            String body = emailContents.get("BODY");
            String sender = "me"; // stupid!!
            try {
                Message message = createMessageWithEmail(createEmail(recipient, sender, subject, body));

                mService.users().messages().send(sender, message).execute();
                Log.i("EMAIL", "Email sent to: " + recipient);
            } catch (MessagingException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;

        }
    }

    private class SaveDraftAsyncTask extends AsyncTask<HashMap, Void, Void> {
        private com.google.api.services.gmail.Gmail mService = null;

        public SaveDraftAsyncTask(GoogleAccountCredential credential) {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            com.google.api.client.json.JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            mService = new com.google.api.services.gmail.Gmail.Builder(
                    transport, jsonFactory, credential)
                    .setApplicationName("Gmail API Android")
                    .build();
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Toast.makeText(EmailItemListActivity.this, "Draft saved.", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Void doInBackground(HashMap... params) {
            HashMap<String, String> draftContents = params[0];
            String subject = "";
            String recipient = "";
            String body = "";
            String sender = "me";

            //detect whether

            if (draftContents.get("SUBJECT") != null) {
                subject = draftContents.get("SUBJECT");
            } else {
                draftContents.put("SUBJECT", "");
            }
            if (draftContents.get("BODY") != null) {
                body = draftContents.get("BODY");
            } else {
                draftContents.put("BODY", "");
            }
            if (draftContents.get("RECIPIENT") != null) {
                recipient = draftContents.get("RECIPIENT");
            } else {
                draftContents.put("RECIPIENT", "");
            }

            try {
                MimeMessage mimeMessage = createEmail(recipient, sender, subject, body);
                Draft draft = createDraft(mService, sender, mimeMessage);
                String draftId = draft.getId();
                draftContents.put("ID", draftId);
                mHelper.saveDraftToDb(draftContents);

            } catch (MessagingException e) {
                e.printStackTrace();
                cancel(true);
            } catch (IOException e) {
                e.printStackTrace();
                cancel(true);
            }

            return null;
        }
    }

    private class SearchEmailAsyncTask extends AsyncTask<String, Void, List<Email>> {

        private String id;
        private String snippet;
        private String date;
        private String subject;
        private String sender;
        HashMap<String, String> searchHash;
        List<Email> searchList;

        HashMap<String, String> emailHashFromDb;
        List<Email> emailListFromDb;

        public SearchEmailAsyncTask() {
            searchHash = new HashMap<>();
            searchList = new ArrayList<>();
            emailHashFromDb = new HashMap<>();
            emailListFromDb = new ArrayList<>();
        }

        @Override
        protected List<Email> doInBackground(String... params) {
            mHelper = MailDatabaseOpenHelper.getInstance(EmailItemListActivity.this);
            String query = params[0];
            Cursor searchCursor = mHelper.searchEmailDb(query);
            searchCursor.moveToFirst();
            while (!searchCursor.isAfterLast()) {
                searchHash.clear();
                sender = searchCursor.getString(searchCursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_SENDER));
                date = searchCursor.getString(searchCursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_DATE));
                subject = searchCursor.getString(searchCursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_SUBJECT));
                id = searchCursor.getString(searchCursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_ID));
                snippet = searchCursor.getString(searchCursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_SNIPPET));

                searchHash.put(MailDatabaseOpenHelper.MAIL_SENDER, sender);
                searchHash.put(MailDatabaseOpenHelper.MAIL_SUBJECT, subject);
                searchHash.put(MailDatabaseOpenHelper.MAIL_SNIPPET, snippet);
                searchHash.put(MailDatabaseOpenHelper.MAIL_ID, id);
                searchHash.put(MailDatabaseOpenHelper.MAIL_DATE, date);

                searchList.add(new Email(searchHash));

                searchCursor.moveToNext();

            }

            Cursor cursor = mHelper.getAllEmailsFromDb();
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                emailHashFromDb.clear();
                emailHashFromDb.put("SENDER", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_SENDER)));
                emailHashFromDb.put("DATE", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_DATE)));
                emailHashFromDb.put("SUBJECT", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_SUBJECT)));
                emailHashFromDb.put("ID", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_ID)));
                emailHashFromDb.put("SNIPPET", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_SNIPPET)));
                emailHashFromDb.put("DRAFT", cursor.getString(cursor.getColumnIndex(MailDatabaseOpenHelper.MAIL_DRAFT)));
                emailListFromDb.add(new Email(emailHashFromDb));
                cursor.moveToNext();
            }
            searchCursor.close();
            cursor.close();
            mRecyclerViewList = emailListFromDb;
            return searchList;
        }

        @Override
        protected void onPostExecute(List<Email> emailList) {
            mEmailRecyclerAdapter = new EmailRecyclerAdapter(EmailItemListActivity.this, mRecyclerViewList);
            emaillistRecycler.setAdapter(mEmailRecyclerAdapter);
            mEmailRecyclerAdapter.animateTo(emailList);
            emaillistRecycler.scrollToPosition(0);

        }
    }


    public static MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        InternetAddress tAddress = new InternetAddress(to);
        InternetAddress fAddress = new InternetAddress(from);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    public static Message createMessageWithEmail(MimeMessage email) throws MessagingException, IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        email.writeTo(bytes);
        String encodedEmail = com.google.api.client.util.Base64.encodeBase64URLSafeString(bytes.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    public static Draft createDraft(Gmail service, String userId, MimeMessage email) throws MessagingException, IOException {
        Message message = createMessageWithEmail(email);
        Draft draft = new Draft();
        draft.setMessage(message);
        draft = service.users().drafts().create(userId, draft).execute();

        System.out.println("draft id:" + draft.getId());
        return draft;

    }

    public void setRecyclerView(List<Email> recyclerList) {
        mRecyclerViewList = recyclerList;

        emaillistRecycler.setHasFixedSize(true);
        emaillistRecycler.setLayoutManager(new LinearLayoutManager(EmailItemListActivity.this));
        emaillistRecycler.addItemDecoration(new VerticalSpaceItemDecoration(20));
        emaillistRecycler.addItemDecoration(new DividerItemDecoration(EmailItemListActivity.this, R.drawable.divider));
        emailRecyclerAdapter = new EmailRecyclerAdapter(EmailItemListActivity.this, mRecyclerViewList);
        emaillistRecycler.setAdapter(emailRecyclerAdapter);
    }

    public void saveDraft(HashMap<String, String> draftMap) {
        new SaveDraftAsyncTask(mCredential).execute(draftMap);
    }


    //For the case of backing in compose fragment

    @Override
    public void onBackPressed() {

        int count = getSupportFragmentManager().getBackStackEntryCount();

        if (count == 0) {
            super.onBackPressed();

        } else {
            getSupportFragmentManager().popBackStack();
        }
    }


}
