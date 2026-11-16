// RUN_PIPELINE_TILL: CODEGEN
package test

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.IgnoredOnParcel

@Parcelize
class A(@IgnoredOnParcel val x: String = "OK") : Parcelable
