// WITH_RUNTIME
// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: var b: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1
    var c: Int = 1

    val t = 1 + <selection>if (a > 0) {
        b += a
        a + 1
    }
    else a</selection>

    return b + c
}