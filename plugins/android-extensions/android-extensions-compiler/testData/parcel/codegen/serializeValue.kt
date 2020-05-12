// This test checks that we create calls to readValue/writeValue if there is no other
// way of serializing properties. In this case, this would fail at runtime.

// CURIOUS_ABOUT writeToParcel, createFromParcel, <clinit>, describeContents
// WITH_RUNTIME

import kotlinx.android.parcel.*
import android.os.Parcelable

class Value(val x: Int)

@Parcelize
class Test(val value: Value) : Parcelable
