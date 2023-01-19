package com.torther.droidcasts;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent service = new Intent(MainActivity.this, DroidCastSService.class);
        MainActivity.this.startService(service);
    }

    @Override
    protected void onResume() {
        super.onResume();

        Intent service = new Intent(MainActivity.this, DroidCastSService.class);
        MainActivity.this.startService(service);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Intent service = new Intent(MainActivity.this, DroidCastSService.class);
        MainActivity.this.startService(service);
    }
}
