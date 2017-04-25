// EXPECTED_REACHABLE_NODES: 487
package foo

fun box(): String {
    var a = 0
    var i = 0
    when(i++) {
        -100 -> a++
        100 -> a++
        else -> a++
    }

    return if ((a == 1) && (i == 1)) "OK" else "fail"
}