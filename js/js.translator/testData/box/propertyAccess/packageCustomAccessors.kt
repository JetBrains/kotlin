// EXPECTED_REACHABLE_NODES: 1250
package foo

var a: Int


    get() {
        return 5
    }
    set(b) {

    }

fun box(): String {
    return if (a == 5) "OK" else "fail: $a"
}