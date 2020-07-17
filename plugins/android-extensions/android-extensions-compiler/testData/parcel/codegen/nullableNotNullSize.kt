// CURIOUS_ABOUT writeToParcel
// WITH_RUNTIME

import android.util.Size
import kotlinx.android.parcel.*
import android.os.Parcelable

@Parcelize
class TestNullable(val a: Size?) : Parcelable

@Parcelize
class TestNotNull(val a: Size) : Parcelable