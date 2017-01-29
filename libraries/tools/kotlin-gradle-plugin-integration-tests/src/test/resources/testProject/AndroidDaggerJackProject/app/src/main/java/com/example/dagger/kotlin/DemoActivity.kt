package com.example.dagger.kotlin

import android.app.Activity
import android.os.Bundle

abstract class DemoActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Perform injection so that when this call returns all dependencies will be available for use.
        (application as DemoApplication).component.inject(this)
    }
}
