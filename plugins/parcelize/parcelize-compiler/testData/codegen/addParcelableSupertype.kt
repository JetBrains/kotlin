// CURIOUS_ABOUT: writeToParcel, createFromParcel, <clinit>, describeContents
// WITH_STDLIB

import kotlinx.parcelize.*

@AddParcelableSupertype
@Parcelize
class User(val firstName: String, val age: Int)
