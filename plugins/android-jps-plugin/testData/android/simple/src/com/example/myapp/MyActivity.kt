package com.example.myapp

import android.app.Activity
import android.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View

import kotlinx.android.synthetic.main.*
import kotlinx.android.synthetic.fragment.*
import kotlinx.android.synthetic.fragment.view.*

public class MyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        textField.setText("test")
    }
}

public class MyFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment, container, false)
    }

    override fun onResume() {
        super.onResume()
        fragmentContainer.textField2.setText("test fragment")
    }
}