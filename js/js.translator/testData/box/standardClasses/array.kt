// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1280
package foo

fun box(): String {

    val a = arrayOfNulls<Int>(2)
    a.set(1, 2)
    return if (a.get(1) == 2) "OK" else "fail"
}

