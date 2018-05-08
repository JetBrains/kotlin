// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1374
package foo


fun box(): String {
    val a = ArrayList<Int>();
    a.add(1)
    a.add(2)
    if (a.size != 2) return "fail1: ${a.size}"
    if (a.get(1) != 2) return "fail2"
    if (a.get(0) != 1) return "fail3"

    return "OK"
}