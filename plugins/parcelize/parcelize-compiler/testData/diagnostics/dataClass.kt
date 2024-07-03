// FIR_IDENTICAL
// WITH_STDLIB

package test

import kotlinx.parcelize.*
import android.os.Parcelable

data class Accepted(val x: String)
data class PrivateField(private val x: String)
@ConsistentCopyVisibility
data class PrivateConstructor private constructor(val x: String)
data class UnsupportedField(val x: Any)
data class Generic<T>(val x: T)
data class GenericOut<out T>(val x: T)
data class GenericBounded<T : Parcelable>(val x: T)

@Parcelize
class C(
    val disabled: <!PARCELABLE_TYPE_NOT_SUPPORTED!>Accepted<!>,
    val a: @DataClass Accepted,
    val b: <!PARCELABLE_TYPE_NOT_SUPPORTED!>@DataClass PrivateField<!>,
    val c: <!PARCELABLE_TYPE_NOT_SUPPORTED!>@DataClass PrivateConstructor<!>,
    val d: <!PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED!>@DataClass UnsupportedField<!>,
    val e: @DataClass Generic<String>,
    val f: @DataClass GenericOut<String>,
    val g: <!PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED!>@DataClass Generic<Any><!>,
    val h: @DataClass GenericBounded<*>,
) : Parcelable
