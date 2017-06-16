// CURIOUS_ABOUT writeToParcel

import android.util.Size
import kotlinx.android.parcel.*

@MagicParcel
class TestNullable(val a: Size?)

@MagicParcel
class TestNotNull(val a: Size)