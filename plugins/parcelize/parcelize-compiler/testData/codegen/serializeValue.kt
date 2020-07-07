// CURIOUS_ABOUT writeToParcel, createFromParcel, <clinit>, describeContents
// WITH_RUNTIME

import kotlinx.parcelize.*
import android.os.Parcelable

class Value(val x: Int)

@Parcelize
class Test(val value: @RawValue Value) : Parcelable
