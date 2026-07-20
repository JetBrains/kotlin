package com.example.databinding

import androidx.databinding.BindingMethod
import androidx.databinding.BindingMethods
import android.widget.ImageView


@BindingMethods(
    BindingMethod(
        type = ImageView::class,
        attribute = "app:srcCompat",
        method = "setImageResource"
    )
)
class MyBindingMethods
