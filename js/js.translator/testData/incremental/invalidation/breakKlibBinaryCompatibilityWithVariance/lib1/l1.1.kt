interface GenericInterface<out T>

interface TestInterface {
    fun <T> test(f: () -> GenericInterface<T>) {}
}
