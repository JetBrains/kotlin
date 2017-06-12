// EXPECTED_REACHABLE_NODES: 489
package foo

class A() {

}

fun box(): String {
    var a: A? = null
    when(a) {
        is A? -> return "OK"
        else -> return "fail"
    }
}