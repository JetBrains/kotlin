// "Migrate to ''Parceler'' companion object" "true"
// ERROR: 'CREATOR' definition is not allowed. Use 'Parceler' companion object instead
// WITH_RUNTIME

package com.myapp.activity

import android.os.*
import kotlinx.android.parcel.Parcelize

@Parcelize
class Foo(val firstName: String, val age: Int) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readInt()) {
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object <caret>CREATOR : Parcelable.Creator<Foo> {
        override fun createFromParcel(parcel: Parcel): Foo {
            return Foo(parcel)
        }

        override fun newArray(size: Int): Array<Foo?> {
            return arrayOfNulls(size)
        }
    }
}