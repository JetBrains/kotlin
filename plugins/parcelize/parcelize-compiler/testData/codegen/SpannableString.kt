// CURIOUS_ABOUT: writeToParcel, createFromParcel
// WITH_STDLIB

import kotlinx.parcelize.*
import android.os.Parcelable
import android.text.SpannableString

@Parcelize
class MyClass(
    val s: SpannableString,
) : Parcelable
