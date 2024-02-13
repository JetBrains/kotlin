// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT
import kotlinx.serialization.*

@Serializable
class C(val values: IntArray) // OK

@Serializable
class B(val values: Array<String>) // OK

@Serializable
class A<T>(val values: <!GENERIC_ARRAY_ELEMENT_NOT_SUPPORTED!>Array<T><!>)
