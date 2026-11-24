// https://issuetracker.google.com/590764741
// CURIOUS_ABOUT: writeToParcel
// WITH_STDLIB
@file:OptIn(kotlinx.parcelize.Experimental::class)

import android.os.Parcelable
import kotlinx.parcelize.DataClass
import kotlinx.parcelize.Parcelize

data class Box<T : List<Int>>(val x: T)

@Parcelize
class Holder(val box: @DataClass Box<in ArrayList<Int>>) : Parcelable
