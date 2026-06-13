package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
data class Bar(
  val name: String,
) : Parcelable
