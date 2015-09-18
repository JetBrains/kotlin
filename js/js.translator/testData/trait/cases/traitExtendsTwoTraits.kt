package foo

interface A {
    fun addFoo(s: String): String {
        return s + "FOO"
    }
}

interface B {
    fun hooray(): String {
        return "hooray"
    }
}

interface AD : A, B {

}

class Test() : AD {
    fun eval(): String {
        return addFoo(hooray());
    }
}

fun box() = (Test().eval() == "hoorayFOO")
