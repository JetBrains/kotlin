// CURIOUS_ABOUT writeToParcel, createFromParcel

import kotlinx.android.parcel.*
import android.os.Parcelable
import android.util.Size
import android.util.SizeF

@MagicParcel
data class Test(val size: Size, val nullable: Size?) : Parcelable

@MagicParcel
data class TestF(val size: SizeF, val nullable: SizeF?) : Parcelable