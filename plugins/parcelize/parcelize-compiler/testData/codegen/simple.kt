// CURIOUS_ABOUT: writeToParcel, createFromParcel, <clinit>, describeContents
// WITH_STDLIB
// LOCAL_VARIABLE_TABLE

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class User(val firstName: String, val lastName: String, val age: Int, val isProUser: Boolean) : Parcelable
