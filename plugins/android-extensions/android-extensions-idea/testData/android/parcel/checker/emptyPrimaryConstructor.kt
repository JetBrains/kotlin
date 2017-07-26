package test

import kotlinx.android.parcel.Parcelize
import android.os.Parcelable

@Parcelize
class User : Parcelable

@Parcelize
class <warning descr="[PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY] The primary constructor is empty, no data will be serialized to 'Parcel'">User2</warning>() : Parcelable