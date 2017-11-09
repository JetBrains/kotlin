package test

import kotlinx.android.parcel.Parcelize
import android.os.Parcelable

@Parcelize
class User(val firstName: String, val secondName: String, val age: Int) : Parcelable