package test

import kotlinx.android.parcel.MagicParcel
import android.os.Parcelable

@MagicParcel
class <error descr="[NO_PARCELABLE_SUPERTYPE] No 'Parcelable' supertype">Without</error>(val firstName: String, val secondName: String, val age: Int)

@MagicParcel
class With(val firstName: String, val secondName: String, val age: Int) : Parcelable

interface MyParcelableIntf : Parcelable

abstract class MyParcelableCl : Parcelable

@MagicParcel
class WithIntfSubtype(val firstName: String, val secondName: String, val age: Int) : MyParcelableIntf

@MagicParcel
class WithClSubtype(val firstName: String, val secondName: String, val age: Int) : MyParcelableCl()