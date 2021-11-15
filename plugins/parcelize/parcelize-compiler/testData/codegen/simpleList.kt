// CURIOUS_ABOUT: writeToParcel
// WITH_STDLIB

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class Test(val names: List<String>): Parcelable
