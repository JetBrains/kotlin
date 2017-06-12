// EXPECTED_REACHABLE_NODES: 496
// See KT-7043, KT-11711
package foo

inline fun foo(b: Any) {
    val t = aa[0]
    val a = b
}

val a: Array<String>
    get() {
        log("a.get")
        return arrayOf("a")
    }

val aa: Array<String>
    get() {
        log("aa.get")
        return arrayOf("aa")
    }

fun box(): String {
    foo(a[0])

    assertEquals("a.get;aa.get;", pullLog())

    return "OK"
}