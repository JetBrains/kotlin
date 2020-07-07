// "Add empty primary constructor" "true"
// WITH_RUNTIME

package com.myapp.activity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class <caret>Test : Parcelable {
    constructor(s: String)
    constructor(s: String, i: Int) : this(s)
}