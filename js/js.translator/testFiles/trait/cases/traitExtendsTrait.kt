package foo

trait Test {
    fun addFoo(s: String): String {
        return s + "FOO"
    }
}

trait ExtendedTest : Test {
    fun hooray(): String {
        return "hooray"
    }
}

class A() : ExtendedTest {
    fun eval(): String {
        return addFoo(hooray());
    }
}

fun box() = (A().eval() == "hoorayFOO")