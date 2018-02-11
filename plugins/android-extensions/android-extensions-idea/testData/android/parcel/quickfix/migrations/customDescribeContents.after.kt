// "Migrate to ''Parceler'' companion object" "true"
// ERROR: 'CREATOR' definition is not allowed. Use 'Parceler' companion object instead
// ERROR: Overriding 'writeToParcel' is not allowed. Use 'Parceler' companion object instead
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

    <caret>override fun describeContents(): Int {
        return 50
    }

    companion object : Parceler<Foo> {

        override fun Foo.write(parcel: Parcel, flags: Int) {
            parcel.writeString(firstName)
            parcel.writeInt(age)
        }

        override fun create(parcel: Parcel): Foo {
            return Foo(parcel)
        }
    }
}