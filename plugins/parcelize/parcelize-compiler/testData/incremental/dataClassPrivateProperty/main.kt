package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class DataWrapper @OptIn(Experimental::class) constructor(val data: @DataClass Data) : Parcelable
