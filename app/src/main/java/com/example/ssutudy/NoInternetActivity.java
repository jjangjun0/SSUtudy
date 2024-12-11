package com.example.ssutudy;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class NoInternetActivity extends AppCompatActivity {

    private static NoInternetActivity instance;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_no_internet);

        // Save instance for later use
        instance = this;
    }

    // Close this activity when internet is restored
    public static void closeActivity() {
        if (instance != null) {
            instance.finish();
            instance = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;
    }
}