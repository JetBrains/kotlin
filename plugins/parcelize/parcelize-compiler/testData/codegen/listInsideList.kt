// CURIOUS_ABOUT writeToParcel
// WITH_RUNTIME

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class Test(val names: List<List<ArrayList<String>>>): Parcelable