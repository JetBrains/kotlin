// EXPECTED_REACHABLE_NODES: 514
package foo

interface A {
    fun addFoo(s: String): String {
        return s + "K"
    }
}

interface B {
    fun hooray(): String {
        return "O"
    }
}

interface AD : A, B {

}

class Test() : AD {
    fun eval(): String {
        return addFoo(hooray());
    }
}

fun box() = Test().eval()
