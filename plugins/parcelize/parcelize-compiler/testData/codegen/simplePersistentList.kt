// CURIOUS_ABOUT: createFromParcel
// WITH_STDLIB

import kotlinx.parcelize.*
import android.os.Parcelable
import kotlinx.collections.immutable.*

@Parcelize
class Test(val names: PersistentList<String>): Parcelable
