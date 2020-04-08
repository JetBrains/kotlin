// WITH_RUNTIME
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: value-parameter a: kotlin.Int defined in test

// SIBLING:
fun test(a: Int): Int {
    <selection>println(a)
    return a + 1</selection>
}

fun foo1(): Int {
    val x = 1
    println(x)
    val y = x + 1
    return y
}

fun foo2(): () -> Int {
    val z = 1
    return {
        println(z)
        z + 1
    }
}

fun foo3() {
    var t = 1
    println(t)
    t = t + 1
    println(t)
}

fun foo4(a: Int): Int {
    val t = println(a)
    return a + 1
}

fun foo5(a: Int) {
    println(a)
    a + 1
}