// EXPECTED_REACHABLE_NODES: 488
package foo

fun test():Any {
    val a: Any = "OK"
    val f: Any =
            if (true) {
                when {
                    false -> "1"
                    ((a as? String)?.size ?: 0 > 0) -> a
                    else -> "2"
                }
            }
            else {
                "3"

            }

    return f
}

fun box(): Any {
    var result = test()
    return result
}

