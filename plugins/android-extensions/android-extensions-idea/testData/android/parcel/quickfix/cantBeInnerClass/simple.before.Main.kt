// "Remove 'inner' modifier" "true"
// ERROR: 'Parcelable' can't be an inner class
// WITH_STDLIB

package com.myapp.activity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

class Foo {
    @Parcelize
    <caret>inner class Bar : Parcelable
}