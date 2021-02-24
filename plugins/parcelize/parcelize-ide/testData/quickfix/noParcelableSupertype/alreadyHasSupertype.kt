// "Add ''Parcelable'' supertype" "false"
// IGNORE_IRRELEVANT_ACTIONS
// WITH_RUNTIME

package com.myapp.activity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

abstract class AbstractParcelable : Parcelable

@Parcelize
class <caret>Test(val s: String) : AbstractParcelable()