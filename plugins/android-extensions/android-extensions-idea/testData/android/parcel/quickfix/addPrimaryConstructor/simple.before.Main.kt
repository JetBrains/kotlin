// "Add empty primary constructor" "true"
// ERROR: 'Parcelable' should have a primary constructor
// WITH_RUNTIME

package com.myapp.activity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class <caret>Test : Parcelable {
    constructor(a: Int)
}