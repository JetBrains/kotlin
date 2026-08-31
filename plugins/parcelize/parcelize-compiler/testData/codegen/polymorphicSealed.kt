// CURIOUS_ABOUT: writeToParcel, createFromParcel, <clinit>
// WITH_STDLIB
// LOCAL_VARIABLE_TABLE
// FILE: test.kt
@file:OptIn(kotlinx.parcelize.Experimental::class)
package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
@PolymorphicSealed
sealed class State : Parcelable {
    @ParcelTag(0)
    data object Idle : State()

    @ParcelTag(42)
    data class Loading(val progress: Float) : State()
}

@Parcelize
data class Container(val state: State) : Parcelable
