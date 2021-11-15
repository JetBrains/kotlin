// "Migrate to ''Parceler'' companion object" "true"
// WITH_STDLIB

package com.myapp.activity

import android.os.*
import kotlinx.parcelize.Parcelize

@Parcelize
class Foo(val firstName: String, val age: Int) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString(), parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(firstName)
        parcel.writeInt(age)
    }

    companion object {
        @JvmField
        val <caret>CREATOR = object : Parcelable.Creator<Foo> {
            override fun createFromParcel(parcel: Parcel): Foo {
                return Foo(parcel)
            }

            override fun newArray(size: Int): Array<Foo?> {
                return arrayOfNulls(size)
            }
        }
    }
}