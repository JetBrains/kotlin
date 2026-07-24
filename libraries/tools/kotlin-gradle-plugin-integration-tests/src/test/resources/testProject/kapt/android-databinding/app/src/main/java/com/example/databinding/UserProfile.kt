package com.example.databinding

import android.content.res.Resources
import androidx.databinding.BaseObservable

class UserProfile : BaseObservable() {
    open var gender: Gender = Gender.Female
}

enum class Gender(val display: String): Displayable {
    Male("male"), Female("female");
    override fun displayString(res: Resources): String = display
}