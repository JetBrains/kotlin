package test

import kotlinx.android.parcel.MagicParcel
import android.os.Parcelable

@MagicParcel
class User : Parcelable

@MagicParcel
class <warning descr="[PARCELABLE_PRIMARY_CONSTRUCTOR_IS_EMPTY] The primary constructor is empty, no data will be serialized to 'Parcel'">User2</warning>() : Parcelable