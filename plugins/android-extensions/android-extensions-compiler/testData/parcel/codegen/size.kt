// CURIOUS_ABOUT writeToParcel, createFromParcel
// WITH_STDLIB

import kotlinx.android.parcel.*
import android.os.Parcelable
import android.util.Size
import android.util.SizeF

@Parcelize
data class Test(val size: Size, val nullable: Size?) : Parcelable

@Parcelize
data class TestF(val size: SizeF, val nullable: SizeF?) : Parcelable