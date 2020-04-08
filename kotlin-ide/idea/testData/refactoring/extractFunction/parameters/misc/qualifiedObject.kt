// SIBLING:
class MyClass {
    fun test() {
        <selection>P.foo()
        P.a</selection>
    }

    object P {
        val a = 1
        fun foo() = 1
    }
}