// "Remove custom ''CREATOR'' property" "true"
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

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(a)
    }

    override fun describeContents(): Int {
        return 0
    }

<caret>}