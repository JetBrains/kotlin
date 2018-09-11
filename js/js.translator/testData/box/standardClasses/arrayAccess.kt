// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1280
package foo

fun box(): String {
    val a = arrayOfNulls<Int>(4)
    a[1] = 2
    a[2] = 3
    return if ((a[1] == 2) && (a[2] == 3)) "OK" else "fail"
}