package test

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
class User(val firstName: String, val secondName: String, val age: Int) : Parcelable