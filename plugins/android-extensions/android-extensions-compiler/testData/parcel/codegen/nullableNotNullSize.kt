// CURIOUS_ABOUT writeToParcel

import android.util.Size
import kotlinx.android.parcel.*

@Parcelize
class TestNullable(val a: Size?)

@Parcelize
class TestNotNull(val a: Size)