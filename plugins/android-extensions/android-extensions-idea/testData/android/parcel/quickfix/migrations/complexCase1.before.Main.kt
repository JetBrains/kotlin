// "Migrate to ''Parceler'' companion object" "true"
// ERROR: 'CREATOR' definition is not allowed. Use 'Parceler' companion object instead
// ERROR: Overriding 'writeToParcel' is not allowed. Use 'Parceler' companion object instead
// WITH_RUNTIME

package com.myapp.activity

import android.os.*
import kotlinx.android.parcel.Parcelize

@Parcelize
class Foo(val firstName: String, val age: Int) : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readString(), parcel.readInt())

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(firstName)
        parcel.writeInt(age)
    }

    companion object {
        @JvmField
        val <caret>CREATOR: Parcelable.Creator<Foo> = object : Creator() {}
    }

    private abstract class Creator : Parcelable.Creator<Foo> {
        override fun createFromParcel(parcel: Parcel): Foo {
            return Foo(parcel)
        }

        override fun newArray(size: Int): Array<Foo?> {
            return arrayOfNulls(size)
        }
    }
}