package com.myapp

import android.app.Activity
import android.view.View
import android.widget.*

val Activity.MyButton: org.my.cool.Button
    get() = findViewById(0) as org.my.cool.Button

