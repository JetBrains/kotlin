package com.example.dagger.kotlin

fun useBuildConfigJava() {
    if (BuildConfig.APPLICATION_ID != "com.example.dagger.kotlin") throw AssertionError()
}