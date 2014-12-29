package com.example.myapp

import android.app.Activity
import android.os.Bundle

import kotlinx.android.synthetic.main.*

public class MyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        textField.setText("test")
    }
}
