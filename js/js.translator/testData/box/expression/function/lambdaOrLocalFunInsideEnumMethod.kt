// EXPECTED_REACHABLE_NODES: 524
package foo

enum class Foo {
    BAR;

    fun test(): () -> String {
        fun local() = 29
        val lambda = { "OK" + local() }

        assertEquals(29, local())
        assertEquals("OK29", lambda())

        return lambda
    }
}

fun box(): String {
    assertEquals("OK29", Foo.BAR.test()())

    return "OK"
}