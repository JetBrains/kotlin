// !JVM_DEFAULT_MODE: enable
// EXPECTED_ERROR: (kotlin:20:5) modifier private not allowed here

interface Foo {
    fun foo() {
        System.out.println("foo")
    }

    @JvmDefault
    fun foo2(a: Int) {
        System.out.println("foo2")
    }

    fun bar()

    private fun privateMethodWithDefault() {
        System.out.println("privateMethodWithDefault")
    }

    @JvmDefault
    private fun privateMethodWithDefault2() {
        System.out.println("privateMethodWithDefault2")
    }
}
