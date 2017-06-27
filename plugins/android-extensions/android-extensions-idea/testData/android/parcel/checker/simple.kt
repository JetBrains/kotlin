package test

import kotlinx.android.parcel.MagicParcel
import android.os.Parcelable

@MagicParcel
class User(val firstName: String, val secondName: String, val age: Int) : Parcelable