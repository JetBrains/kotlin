// CURIOUS_ABOUT: writeToParcel, createFromParcel, <clinit>, describeContents
// IMPLICITLY_ADD_PARCELABLE_SUPERTYPE
// WITH_STDLIB

import kotlinx.parcelize.*

@Parcelize
class User(val firstName: String, val age: Int)
