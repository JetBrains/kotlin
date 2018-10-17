// EXPECTED_REACHABLE_NODES: 1285
package foo

var flag = false
fun toggle(): Boolean {
    flag = !flag

    return flag
}

inline fun run(noinline f: () -> Int): Int {
    return f()
}

fun box(): String {
    run({ toggle(); 4 })
    assertEquals(true, flag)

    return "OK"
}