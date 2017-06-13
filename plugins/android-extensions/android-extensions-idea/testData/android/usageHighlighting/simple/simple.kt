package com.myapp

import android.app.Activity
import android.os.Bundle
import java.io.File
import kotlinx.android.synthetic.main.layout.*

class MyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        <info descr="null"><caret>login</info>.setOnClickListener {

        }

        <info descr="null">login</info>.text = "Login"

        val login = "Login"
        val login42 = login + "42"
    }

    fun foo(): String {
        val login = "Login"
        return login + "42"
    }
}

class AnotherActivity : Activity() {
    private val login: String = "login"

    fun test() {
        login.length
        (this as Activity).<info descr="null">login</info>.text = "login"
    }
}