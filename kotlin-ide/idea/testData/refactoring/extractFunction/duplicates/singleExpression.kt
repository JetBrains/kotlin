// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: value-parameter b: kotlin.Int defined in foo

// SIBLING:
fun foo(a: Int, b: Int): Int {
    return <selection>a + b*a</selection> + 1
}

fun bar() {
    fun f() = 1

    val a = 1
    val b = 2
    val c = 3

    c + b*c
    b + a*b
    a plus a*a
    a + a*b
    a + c
    f() + a*f()
    f() + f().times(f())
}