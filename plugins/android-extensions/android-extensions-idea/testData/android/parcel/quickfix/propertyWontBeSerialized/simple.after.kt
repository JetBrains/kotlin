// "Add ''@IgnoredOnParcel'' annotation" "true"
// WITH_RUNTIME

package com.myapp.activity

import android.os.Parcelable
import kotlinx.android.parcel.IgnoredOnParcel
import kotlinx.android.parcel.Parcelize

@Parcelize
class Test : Parcelable {
    @IgnoredOnParcel
    val <caret>a = 5
}