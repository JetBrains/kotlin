// CURIOUS_ABOUT: writeToParcel, createFromParcel
// RENDER_ANNOTATIONS
// WITH_STDLIB

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
data class Box<T : Parcelable>(val box: T) : Parcelable
