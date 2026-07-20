package com.example.databinding

import android.app.Activity
import androidx.databinding.DataBindingUtil
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Toast
import com.example.databinding.databinding.ActivityTestBinding

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding: ActivityTestBinding = DataBindingUtil.setContentView(this, R.layout.activity_test)
        val uprof = UserProfile()
        binding.userProfile = uprof
        binding.context = this
        binding.genderPicker.adapter = EnumAdapter(this, Gender::class.java)
        binding.genderPicker.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN)
                spinnerClicked = true
            false
        }
    }

    var spinnerClicked = false
    fun spinnerClicked() {
        spinnerClicked = true
    }
    fun selectionChanged() {
        if (spinnerClicked)
            Toast.makeText(this, "sdds", Toast.LENGTH_LONG).show()
        spinnerClicked = false
    }

    fun clickHandler(uprof: UserProfile): Boolean = true

    fun longClickHandler(uprof: UserProfile): Boolean = true
}