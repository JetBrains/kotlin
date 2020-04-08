// WITH_RUNTIME
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int? defined in foo
// PARAM_TYPES: kotlin.Int?
fun foo(a: Int?): Int {
    <selection>val n = a ?: error("")
    return n + 1</selection>
}