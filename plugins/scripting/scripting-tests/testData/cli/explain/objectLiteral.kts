class MyClass {
    fun foo() = 1
    override fun toString(): String {
        return "MyClass()"
    }
}

MyClass().foo()

interface I {
    fun foo(): Int
}

val i = object : I {
    override fun foo(): Int = 1

    override fun toString(): String {
        return "`<no name provided>`()"
    }
}
i.foo()