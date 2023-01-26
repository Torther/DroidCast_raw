package com.torther.droidcasts;

import android.annotation.SuppressLint;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ApplicationInfo info = getApplicationInfo();
        String srcLocation = info.sourceDir;

        TextView textView = findViewById(R.id.text);
        textView.setText("Source apk Dir:" + srcLocation);
    }
}