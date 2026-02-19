// CURIOUS_ABOUT: writeToParcel, createFromParcel
// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)

import kotlinx.parcelize.*
import android.os.Parcel
import android.os.Parcelable

data class A(val x: String, val y: Int)

@Parcelize
class A2(val x: String, val y: Int) : Parcelable

@Parcelize
class Aw(val a: @DataClass A) : Parcelable
