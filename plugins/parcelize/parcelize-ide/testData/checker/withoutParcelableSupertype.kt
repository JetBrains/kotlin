package test

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
class <error descr="[NO_PARCELABLE_SUPERTYPE] No 'Parcelable' supertype">Without</error>(val firstName: String, val secondName: String, val age: Int)

@Parcelize
class With(val firstName: String, val secondName: String, val age: Int) : Parcelable

interface MyParcelableIntf : Parcelable

abstract class MyParcelableCl : Parcelable

@Parcelize
class WithIntfSubtype(val firstName: String, val secondName: String, val age: Int) : MyParcelableIntf

@Parcelize
class WithClSubtype(val firstName: String, val secondName: String, val age: Int) : MyParcelableCl()
