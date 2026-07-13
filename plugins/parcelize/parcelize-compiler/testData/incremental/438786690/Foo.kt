// CURIOUS_ABOUT: writeToParcel

package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
data class Foo(
  val bar: Bar,
  val name: String = "v0"
) : Parcelable
