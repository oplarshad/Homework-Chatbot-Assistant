package com.scottquach.homeworkchatbotassistant.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.scottquach.homeworkchatbotassistant.BaseApplication;
import com.scottquach.homeworkchatbotassistant.ExtensionsKt;
import com.scottquach.homeworkchatbotassistant.MessageHandler;
import com.scottquach.homeworkchatbotassistant.fragments.ChatFragment;
import com.scottquach.homeworkchatbotassistant.fragments.NavigationFragment;
import com.scottquach.homeworkchatbotassistant.R;

import ai.api.AIListener;
import ai.api.model.AIError;
import ai.api.model.AIResponse;
import ai.api.model.Result;
import timber.log.Timber;

import com.google.gson.JsonElement;

import java.util.Map;


public class MainActivity extends AppCompatActivity implements AIListener,
        NavigationFragment.NavigationFragmentInterface, ChatFragment.ChatInterface {

    private DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference();
    private FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_main);
        TextView toolbarTitle = (TextView) findViewById(R.id.toolbar_title);
        toolbarTitle.setText("Chat");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);


        if (BaseApplication.getInstance().isFirstOpen()) {
            Timber.d("first open");
            MessageHandler handler = new MessageHandler(this);
            handler.receiveWelcomeMessage();
            BaseApplication.getInstance().getSharePref().edit().putBoolean("first_open", false).apply();
        }

        if (savedInstanceState == null) {
            ChatFragment fragment = new ChatFragment();
            ExtensionsKt.changeFragmentRightAnimated(getSupportFragmentManager(),
                    R.id.fragment_container_main, fragment, false, false);
        }

        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Timber.d("Retrieved DataSnapshot");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Timber.d("error retrieving data" + databaseError.toString());
            }
        });

        toolbar.findViewById(R.id.toolbar_menu_icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openNavigation();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        requestPermissions();
    }

    @Override
    public void onResult(AIResponse response) {
        Log.d("stuff", "on response was called");
        Result result = response.getResult();
        // Get parameters
        String parameterString = "";
        if (result.getParameters() != null && !result.getParameters().isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : result.getParameters().entrySet()) {
                parameterString += "(" + entry.getKey() + ", " + entry.getValue() + ") ";
            }
        }
    }

    @Override
    public void onError(AIError error) {
        Toast.makeText(this, error.toString(), Toast.LENGTH_SHORT).show();
        Timber.e(error.toString());
    }

    @Override
    public void onAudioLevel(float level) {

    }

    @Override
    public void onListeningStarted() {

    }

    @Override
    public void onListeningCanceled() {

    }

    @Override
    public void onListeningFinished() {

    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 0);
        }
    }

    private void openNavigation() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        TextView toolbarTitle = (TextView) findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getString(R.string.navigation));
        ImageView toolbarIcon = (ImageView) findViewById(R.id.toolbar_menu_icon);
        toolbarIcon.setVisibility(View.INVISIBLE);

        NavigationFragment fragment = new NavigationFragment();
        ExtensionsKt.changeFragmentLeftAnimated(getSupportFragmentManager(),
                R.id.fragment_container_main, fragment, true, true);
    }

    @Override
    public void startClassScheduleActivity() {
        startActivity(new Intent(MainActivity.this, ClassScheduleActivity.class));
    }

    @Override
    public void startDisplayHomeworkActivity() {
        startActivity(new Intent(MainActivity.this, DisplayAssignmentsActivity.class));
    }

    @Override
    public void startMainActivity() {
        ImageView toolbarIcon = (ImageView) findViewById(R.id.toolbar_menu_icon);
        toolbarIcon.setVisibility(View.VISIBLE);
        TextView toolbarTitle = (TextView) findViewById(R.id.toolbar_title);
        toolbarTitle.setText(getString(R.string.chat));

        ChatFragment fragment = new ChatFragment();
        ExtensionsKt.changeFragmentRightAnimated(getSupportFragmentManager(),
                R.id.fragment_container_main, fragment, false, true);
    }
}
