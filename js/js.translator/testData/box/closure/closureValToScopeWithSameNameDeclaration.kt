// EXPECTED_REACHABLE_NODES: 489
package foo

val f = true

fun box(): String {
    val bar = "test "
    val boo = "another "

    fun baz(): String {
        var result = bar

        if (f) {
            val bar = 42
            result += bar

            val boo = 7
            result += boo
        }

        result += boo
        result += bar

        return result
    }

    val r = baz()
    if (r != "test 427another test ") return r;

    return "OK"
}
