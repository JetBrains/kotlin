// CURIOUS_ABOUT: describeContents
// WITH_STDLIB

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class User(val firstName: String, val lastName: String, val age: Int, val isProUser: Boolean) : Parcelable {
    override fun describeContents() = 100
}
