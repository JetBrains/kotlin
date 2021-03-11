open class A {
}

// INFO: {"checked": "true"}
interface I {
    fun foo()
}

class <caret>B : A(), I {
    // INFO: {"checked": "true", "toAbstract": "true"}
    override fun foo() {

    }
}