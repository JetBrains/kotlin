// WITH_RUNTIME
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: var k: kotlin.Int defined in foo
// SIBLING:
fun foo() {
    var k = 0
    <selection>val a = 1
    k++
    val b = 2</selection>
    println(a + b - k)
}