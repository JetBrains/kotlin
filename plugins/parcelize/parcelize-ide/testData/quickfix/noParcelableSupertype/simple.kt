// "Add ''Parcelable'' supertype" "true"
// WITH_RUNTIME

package com.myapp.activity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
class <caret>Test(val s: String)