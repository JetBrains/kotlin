class TestClass : TestInterface {
    override fun <T> test(f: () -> GenericInterface<out T>) {
        f()
    }
}
