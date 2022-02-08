// CURIOUS_ABOUT describeContents
// WITH_STDLIB

import kotlinx.android.parcel.*
import android.os.Parcelable

abstract class AbstractUser : Parcelable {
    override fun describeContents() = 100
}

@Parcelize
class User(val firstName: String, val lastName: String, val age: Int, val isProUser: Boolean) : AbstractUser()