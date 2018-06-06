// "Migrate to ''Parceler'' companion object" "true"
// ERROR: 'CREATOR' definition is not allowed. Use 'Parceler' companion object instead
// WITH_RUNTIME

package com.myapp.activity

import android.os.*
import kotlinx.android.parcel.Parceler
import kotlinx.android.parcel.Parcelize

@Parcelize
class Foo(val firstName: String, val age: Int) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readInt()) {
    }

    companion object <caret>: Parceler<Foo> {

        fun Foo.write(parcel: Parcel, flags: Int) = TODO()
        override fun create(parcel: Parcel): Foo {
            return Foo(parcel)
        }
    }
}