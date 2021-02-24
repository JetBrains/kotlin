// CURIOUS_ABOUT writeToParcel, createFromParcel
// WITH_RUNTIME

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
data class Box<T : Parcelable>(val box: T) : Parcelable
