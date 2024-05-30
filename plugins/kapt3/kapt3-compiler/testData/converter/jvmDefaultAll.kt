// JVM_DEFAULT_MODE: all
// EXPECTED_ERROR_K1: (kotlin:16:13) modifier private not allowed here
// EXPECTED_ERROR_K2: (kotlin:16:5) modifier private not allowed here

interface Foo {
    fun foo() {
        System.out.println("foo")
    }

    fun foo2(a: Int) {
        System.out.println("foo2")
    }

    fun bar()

    private fun privateMethodWithDefault() {
        System.out.println("privateMethodWithDefault")
    }
}
