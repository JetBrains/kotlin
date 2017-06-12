// EXPECTED_REACHABLE_NODES: 489
package foo

fun box(): String {
    val bar = 12

    val baz = {
        var result = "test1 "
        if (true) {
            val bar = "some text"
            result += bar
        }
        result += bar
        result
    }

    val r1 = baz()
    if (r1 != "test1 some text12") return "r1 = $r1";

    val boo = {
        var result = "test2 "
        result += bar
        if (true) {
            val bar = 4
            result += bar
        }
        result
    }

    val r2 = boo()
    if (r2 != "test2 124") return "r2 = $r2";

    return "OK"
}