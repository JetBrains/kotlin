// CURIOUS_ABOUT writeToParcel
// WITH_STDLIB

import android.util.Size
import kotlinx.android.parcel.*
import android.os.Parcelable

@Parcelize
class TestNullable(val a: Size?) : Parcelable

@Parcelize
class TestNotNull(val a: Size) : Parcelable