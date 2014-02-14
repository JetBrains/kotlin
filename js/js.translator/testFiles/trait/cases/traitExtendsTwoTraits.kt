package foo

trait A {
    fun addFoo(s: String): String {
        return s + "FOO"
    }
}

trait B {
    fun hooray(): String {
        return "hooray"
    }
}

trait AD : A, B {

}

class Test() : AD {
    fun eval(): String {
        return addFoo(hooray());
    }
}

fun box() = (Test().eval() == "hoorayFOO")
