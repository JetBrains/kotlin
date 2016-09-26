package org.jetbrains.kotlin.gradle.test.androidalfa

import android.content.Intent
import android.os.Bundle
import android.app.Activity
import android.view.Menu
import android.view.View
import android.widget.Button
import org.jetbrains.kotlin.gradle.test.androidalfa.R
import lib.*

open class MainActivity2: Activity() {

    protected override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        libUtil()

        var next: Button = findViewById(R.id.Button02) as Button
        next.setOnClickListener(object: View.OnClickListener {
            public override fun onClick(view: View): Unit {
                val intent: Intent = Intent()
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        })
    }

    public override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_activity2, menu);
        return true
    }
    
}

fun foo() {
    bar()
}

fun bar() {

}
