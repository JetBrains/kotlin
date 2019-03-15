// EXPECTED_REACHABLE_NODES: 1283
package foo

fun test(): String {
    fun f(): String = "OK"

    val funLit = { f() }
    return funLit()
}

fun box(): String {
    return test()
}