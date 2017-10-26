// "Remove custom ''writeToParcel()'' function" "true"
// ERROR: 'CREATOR' definition is not allowed. Use 'Parceler' companion object instead
// ERROR: Overriding 'writeToParcel' is not allowed. Use 'Parceler' companion object instead
// WITH_RUNTIME

package com.myapp.activity

import android.os.*
import kotlinx.android.parcel.Parcelize

@Parcelize
class Foo(val a: String) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString()) {
    }

    <caret>override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(a)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Foo> {
        override fun createFromParcel(parcel: Parcel): Foo {
            return Foo(parcel)
        }

        override fun newArray(size: Int): Array<Foo?> {
            return arrayOfNulls(size)
        }
    }

}