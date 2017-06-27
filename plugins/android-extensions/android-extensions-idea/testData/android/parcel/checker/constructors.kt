package test

import kotlinx.android.parcel.MagicParcel
import android.os.Parcelable

@MagicParcel
class A : Parcelable

@MagicParcel
class B(val firstName: String, val secondName: String) : Parcelable

@MagicParcel
class C(val firstName: String, <error descr="[PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR] 'Parcelable' constructor parameter should be 'val' or 'var'">secondName</error>: String) : Parcelable

@MagicParcel
class D(val firstName: String, vararg val secondName: String) : Parcelable

@MagicParcel
class E(val firstName: String, val secondName: String) : Parcelable {
    constructor() : this("", "")
}

@MagicParcel
class <error descr="[PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR] 'Parcelable' should have a primary constructor">F</error> : Parcelable {
    constructor(a: String) {
        println(a)
    }
}