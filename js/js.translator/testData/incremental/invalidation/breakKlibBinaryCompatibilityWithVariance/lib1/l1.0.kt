interface GenericInterface<T>

interface TestInterface {
    fun <T> test(f: () -> GenericInterface<out T>) {}
}
