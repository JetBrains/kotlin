package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class DataWrapper(val data: DataAlias) : Parcelable
