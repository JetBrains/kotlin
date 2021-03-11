// PARAM_TYPES: kotlin.Int
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in foo
// PARAM_DESCRIPTOR: value-parameter b: kotlin.Int defined in foo

// SIBLING:
fun foo(a: Int, b: Int) {
    <selection>println("a = $a")
    println("b = $b")
    println(a + b*a)</selection>
}

fun bar() {
    val x = 1
    val y = 2

    println("a = $x")
    println("b = $y")
    println(x + y*x)
}