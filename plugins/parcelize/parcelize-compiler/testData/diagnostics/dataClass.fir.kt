
package test

import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import android.os.Parcelable

data class DataClass(val x: String)
data class PrivateField(private val x: String)
data class PrivateConstructor <!DATA_CLASS_COPY_VISIBILITY_WILL_BE_CHANGED_WARNING!>private<!> constructor(val x: String)
data class UnsupportedField(val x: Any)
data class Generic<T>(val x: T)
data class GenericOut<out T>(val x: T)
data class GenericBounded<T : Parcelable>(val x: T)

@Parcelize
class C(
    val a: DataClass,
    val b: <!PARCELABLE_TYPE_NOT_SUPPORTED!>PrivateField<!>,
    val c: <!PARCELABLE_TYPE_NOT_SUPPORTED!>PrivateConstructor<!>,
    val d: <!PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED!>UnsupportedField<!>,
    val e: Generic<String>,
    val f: GenericOut<String>,
    val g: <!PARCELABLE_TYPE_CONTAINS_NOT_SUPPORTED!>Generic<Any><!>,
    val h: GenericBounded<*>,
) : Parcelable
