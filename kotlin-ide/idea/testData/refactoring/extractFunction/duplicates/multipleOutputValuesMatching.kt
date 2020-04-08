// WITH_RUNTIME
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in test

// SIBLING:
fun test(a: Int): Int {
    <selection>val b = a + 1
    val c = a - 1</selection>
    return b*c
}

fun foo1(a: Int) {
    var x = a + 1
    var y = a - 1
    println(x + y)
}

fun foo2(): Int {
    var p: Int = 1
    var q: Int
    p = p + 1
    q = p - 1
    return p + q
}

fun foo4(a: Int) {
    var b = a
    b = b + 1
    return b - 1
}
