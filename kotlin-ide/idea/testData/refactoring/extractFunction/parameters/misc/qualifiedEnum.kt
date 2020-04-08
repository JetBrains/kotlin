// SIBLING:
class MyClass {
    fun test() {
        <selection>P.A.foo()
        P.A.a</selection>
    }

    enum class P {
        A;

        val a = 1
        fun foo() = 1
    }
}