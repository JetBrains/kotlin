internal class A {
    @Synchronized
    fun foo() {
        bar()
    }

    fun bar() {}
}