// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: overrides
open class A<T>(t: T) {
    open var <caret>foo: T = t
}

open class B: A<String>("") {
    override var foo: String
        get() {
            println("get")
            return super<A>.foo
        }
        set(value: String) {
            println("set:" + value)
            super<A>.foo = value
        }

    fun baz(a: A<String>) {
        a.foo = ""
        println(a.foo)
    }
}

open class D: A<String>("") {
    override var foo: String = ""
}
