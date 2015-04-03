package foo

fun run<A, B, C>(a: A, b: B, func: (A, B) -> C): C = js("func(a, b)")

fun box(): String {
    assertEquals(3, run(1, 2) { a, b -> a + b})

    return "OK"
}