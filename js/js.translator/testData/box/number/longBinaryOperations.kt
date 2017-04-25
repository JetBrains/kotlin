// EXPECTED_REACHABLE_NODES: 493
package foo

fun fact(n: Int): Long = if (n == 1) 1L else n * fact(n - 1)

fun fib(n: Int): Long {
    var a = 0L
    var b = 1L
    for (i in 2..n) {
        var tmp = a
        a = b
        b = b + tmp
    }
    return b
}

fun box(): String {

    assertEquals(30.0, 10L + 20.0)
    assertEquals(30.0f, 10L + 20.0f)
    assertEquals(30L, 10L + 20L)
    assertEquals(30L, 10L + 20)
    assertEquals(30L, 10L + 20.toShort())
    assertEquals(30L, 10L + 20.toByte())

    assertEquals(30.0, 20.0 + 10L)
    assertEquals(30.0f, 20.0f + 10L)
    assertEquals(20L, 10 + 10L)
    assertEquals(20L, 10.toShort() + 10L)
    assertEquals(20L, 10.toByte() + 10L)

    assertEquals(20L, 30 - 10L)

    assertEquals(100L, 10 * 10L)
    assertEquals(100.0, 10.0 * 10L)

    assertEquals(100L, 10L * 10)
    assertEquals(100.0, 10L * 10.0)

    assertEquals(100L, 1000L / 10)
    assertEquals(100L, 1000 / 10L)

    assertEquals(100.0, 1000L / 10.0)
    assertEquals(100.0, 1000.0 / 10L)

    assertEquals(2L, 100L % 7)
    assertEquals(2L, 100 % 7L)

    assertEquals(2432902008176640000L, fact(20))
    assertEquals(12586269025L, fib(50))
    assertEquals(7540113804746346429L, fib(92))

    return "OK"
}