// EXPECTED_REACHABLE_NODES: 499
// See KT-7674
package foo

class A(val a: Int) {
    val plus: (Int)->Int
        get() {
            log("get plus fun")
            return {
                log("do plus")
                a + it
            }
        }
}

inline fun <T : Any> id(x: T): T {
    log(x.toString())
    return x
}

fun box(): String {
    assertEquals(3, A(id(1)).plus(id(2)))
    assertEquals("1;get plus fun;2;do plus;", pullLog())
    return "OK"
}