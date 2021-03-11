open class A {
}

interface I {
    fun foo()
}

class <caret>B : A(), I {
    // INFO: {"checked": "true", "toAbstract": "false"}
    override fun foo() {

    }
}