// EXPECTED_REACHABLE_NODES: 505
package foo

interface Test {
    fun addFoo(s: String): String {
        return s + "FOO"
    }

    fun addBar(s: String): String {
        return s + "BAR"
    }
}


class A() : Test {
    val string = "TEST"
    fun value(): String {
        return addBar(addFoo(string))
    }
}

fun box() = if (A().value() == "TESTFOOBAR") "OK" else "fail"