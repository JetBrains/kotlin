// JVM_DEFAULT_MODE: disable
// JVM_TARGET: 11

interface Foo {
    fun foo() {
        System.out.println("foo")
    }

    fun foo2(a: Int) {
        System.out.println("foo2")
    }

    fun bar()

    private fun privateMethod() {
        System.out.println("privateMethod")
    }

    private fun privateDefaultMethod(x: String? = null) {
        System.out.println("privateDefaultMethod")
    }
}
