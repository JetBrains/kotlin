// "Add ''Parcelable'' supertype" "true"
// ERROR: No 'Parcelable' supertype
// WITH_RUNTIME

package com.myapp.activity

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class <caret>Test(val s: String)