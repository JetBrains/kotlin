interface A<T> {
    fun test() = 0

    val testProp: Int
        get() = 2

    fun testWithDefault(x: Int = 1) = x

    fun testGeneric(x: T) = 1

    fun unused() = 1
}
