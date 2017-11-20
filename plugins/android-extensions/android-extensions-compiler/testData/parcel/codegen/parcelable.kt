// CURIOUS_ABOUT writeToParcel, createFromParcel, <clinit>
// WITH_RUNTIME

package test

import kotlinx.android.parcel.*
import android.os.Parcelable

@Parcelize
class Foo(val parcelable: Parcelable): Parcelable