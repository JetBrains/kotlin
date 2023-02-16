// FIR_IDENTICAL
package test

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.IgnoredOnParcel

class T(val x: Int)

@Parcelize
class A(
    // T is not parcelable, but we don't need it to be since it is not being serialized.
    @IgnoredOnParcel val x: T = T(0)
) : Parcelable {
    @IgnoredOnParcel val y: T = x
}
