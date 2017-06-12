// EXPECTED_REACHABLE_NODES: 497
package foo

class A {

}

object test {
    var c = 2;
    var b = 1;
}

object aWrapper {
    var a = A();
}

fun box(): String {
    if (test.c != 2) return "fail1: ${test.c}"

    if (test.b != 1) return "fail2: ${test.b}"
    test.c += 10
    if (test.c != 12) "fail3: ${test.c}"
    if (aWrapper.a !is A) "fail4"

    return "OK"
}