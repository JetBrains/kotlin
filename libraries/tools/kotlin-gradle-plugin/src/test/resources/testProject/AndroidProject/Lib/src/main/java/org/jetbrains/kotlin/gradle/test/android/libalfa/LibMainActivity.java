package org.jetbrains.kotlin.gradle.test.android.libalfa;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class LibMainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.lib_activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.lib_main, menu);
        return true;
    }
    
}
