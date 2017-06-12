// EXPECTED_REACHABLE_NODES: 491
package foo

fun box(): String {
    var boo = "OK"
    var foo = object {
        val bar = object {
            val baz = boo
        }
    }

    return foo.bar.baz
}

