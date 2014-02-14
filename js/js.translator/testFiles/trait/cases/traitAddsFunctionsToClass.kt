package foo

trait Test {
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

fun box() = A().value() == "TESTFOOBAR"