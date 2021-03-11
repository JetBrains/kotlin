// WITH_RUNTIME
// PARAM_TYPES: kotlin.Int
// PARAM_DESCRIPTOR: val z: kotlin.Int defined in test

// SIBLING:
fun test(): () -> Int {
    val z = 1
    return {
        <selection>println(z)
        z + 1</selection>
    }
}

fun foo1(a: Int): Int {
    val t = println(a)
    return a + 1
}

fun foo2(a: Int) {
    println(a)
    a + 1
}