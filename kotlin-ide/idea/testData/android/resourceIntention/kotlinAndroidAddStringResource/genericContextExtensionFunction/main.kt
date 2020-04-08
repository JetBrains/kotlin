package com.myapp

import android.content.Context

fun <T: Context> T.getText(): String? = "some <caret>text"
