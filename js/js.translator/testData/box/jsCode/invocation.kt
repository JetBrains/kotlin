// FILE: a.kt

fun <A, B, C> run(a: A, b: B, func: (A, B) -> C): C = js("func(a, b)")
fun <A, B> runSpread(args: Array<A>, func: (A, A) -> B): B = js("func(...args)")
fun <A, B> runSpreadComma(args: Array<A>, func: (A, A) -> B): B = js("func(...([5, 6], args))")
fun <A, B> runMixed(a: A, rest: Array<A>, func: (A, A, A) -> B): B = js("func(a, ...rest)")

// FILE: b.kt
// RECOMPILE

fun box(): String {
    assertEquals(3, run(1, 2) { a, b -> a + b })
    assertEquals(3, runSpread(arrayOf(1, 2)) { a, b -> a + b })
    assertEquals(3, runSpreadComma(arrayOf(1, 2)) { a, b -> a + b })
    assertEquals(6, runMixed(1, arrayOf(2, 3)) { a, b, c -> a + b + c })

    return "OK"
}