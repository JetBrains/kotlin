// EXPECTED_REACHABLE_NODES: 498
package foo

object A {
    init {
        log("A.init")
    }

    val x = 23
}

inline fun bar(value: Int) {
    log("bar->begin")
    log("value=$value")
    log("bar->end")
}

fun box(): String {
    bar(A.x)
    assertEquals("A.init;bar->begin;value=23;bar->end;", pullLog())
    return "OK"
}