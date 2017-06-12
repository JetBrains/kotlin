// EXPECTED_REACHABLE_NODES: 491
package foo

class myInt(a: Int) {
    val value = a;

    operator fun plus(other: myInt): myInt = myInt(value + other.value)
}

fun box(): String {

    return if ((myInt(3) + myInt(5)).value == 8) "OK" else "fail"
}