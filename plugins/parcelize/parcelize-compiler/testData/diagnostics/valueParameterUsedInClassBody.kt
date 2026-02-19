// WITH_STDLIB

package test

import kotlinx.parcelize.*
import android.os.Parcelable

@Parcelize
class Foo(var i1: Int, var i2: Int): Parcelable

@Parcelize
open class Base(
    val i: String,
    <!PARCELABLE_CONSTRUCTOR_PARAMETER_SHOULD_BE_VAL_OR_VAR!>x<!>: String,
    val foo: Foo
): Parcelable

@Parcelize
class Derived(val x: String, i: String, foo: Foo): Base(i, x, foo) {

    @IgnoredOnParcel var x2 = <!VALUE_PARAMETER_USED_IN_CLASS_BODY!>i<!>
    init {
        println(<!VALUE_PARAMETER_USED_IN_CLASS_BODY!>foo<!>)
        <!VALUE_PARAMETER_USED_IN_CLASS_BODY!>foo<!>.i1 = 20
        x2 = <!VALUE_PARAMETER_USED_IN_CLASS_BODY!>i<!>
        val <!NAME_SHADOWING!>foo<!> = "hello"
        x2 = foo
    }
}
