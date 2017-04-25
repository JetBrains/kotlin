// EXPECTED_REACHABLE_NODES: 543
package foo

var g: Any?
    get() {
        log("g.get")
        return null
    }
    set(v) {
        log("g.set")
    }

public inline fun Array<String>.boo() {
    var a = g
    for (element in this);
}

public inline fun Iterable<String>.boo(i: Any?) {
    var a = i
    for (element in this);
}

fun test1(f: () -> Array<String>) {
    f().boo()
}

fun test2(f: () -> Iterable<String>) {
    f().boo(g)
}


fun box(): String {
    test1 { log("lambda1"); arrayOf() }
    assertEquals("lambda1;g.get;", pullLog())

    test2 { log("lambda2"); listOf() }
    assertEquals("lambda2;g.get;", pullLog())

    return "OK"
}