// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: var b: kotlin.Int defined in foo
// SIBLING:
fun foo(a: Int): Int {
    var b: Int = 1

    <selection>if (n == 10) throw Exception("")
    b++</selection>

    return b
}