// EXPECTED_REACHABLE_NODES: 493
package foo

class A() {
    fun f(): String {
        val z = object {
            val c = "OK"
        }
        return z.c
    }
}

fun box() = A().f();
