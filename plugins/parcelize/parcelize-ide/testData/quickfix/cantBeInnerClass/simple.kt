// "Remove 'inner' modifier" "true"
// WITH_STDLIB

package com.myapp.activity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

class Foo {
    @Parcelize
    <caret>inner class Bar : Parcelable
}