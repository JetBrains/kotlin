// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
package test

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.IgnoredOnParcel

@Parcelize
class A(@IgnoredOnParcel val x: String = "OK") : Parcelable
