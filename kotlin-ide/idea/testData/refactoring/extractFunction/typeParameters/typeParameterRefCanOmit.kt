// SUGGESTED_NAMES: i, getA
// PARAM_DESCRIPTOR: value-parameter t: T defined in test
// PARAM_TYPES: T
fun <U> foo(u: U) = 1

fun <T> test(t: T) {
    val a = <selection>foo<T>(t)</selection>
}