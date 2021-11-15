// WITH_STDLIB
package test

import kotlinx.parcelize.Parcelize
import android.os.Parcelable

@Parcelize
class A : Parcelable

@Parcelize
class B(val firstName: String, val secondName: String) : Parcelable

@Parcelize
class C(val firstName: String, <error descr="[PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR] 'Parcelable' constructor parameter should be 'val' or 'var'">secondName</error>: String) : Parcelable

@Parcelize
class D(val firstName: String, vararg val secondName: String) : Parcelable

@Parcelize
class E(val firstName: String, val secondName: String) : Parcelable {
    constructor() : this("", "")
}

@Parcelize
class <error descr="[PARCELABLE_SHOULD_HAVE_PRIMARY_CONSTRUCTOR] 'Parcelable' should have a primary constructor">F</error> : Parcelable {
    constructor(a: String) {
        println(a)
    }
}
