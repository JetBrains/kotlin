actual interface A1 {
    actual val property1: Int
    actual fun function1(): Int
}

actual abstract class A2 actual constructor() : A1 {
    actual abstract val property2: Int
    actual abstract fun function2(): Int
}

actual class A3 actual constructor() : A2(), A1 {
    actual override val property1 = 1
    actual override val property2 = 1
    actual val property3 = 1

    actual override fun function1() = 1
    actual override fun function2() = 1
    actual fun function3() = 1
}

actual interface B1 {
    actual val property1: Int
    val property2: Int

    actual fun function1(): Int
    fun function2(): Int
}

actual class B3 actual constructor() : B1 {
    actual override val property1 = 1
    actual override val property2 = 1
    actual val property3 = 1

    actual override fun function1() = 1
    actual override fun function2() = 1
    actual fun function3() = 1
}

actual interface C1 {
    actual val property1: Int
    actual fun function1(): Int
}

actual class C3 actual constructor() : C1 {
    actual override val property1 = 1
    actual val property2 = 1
    actual val property3 = 1

    actual override fun function1() = 1
    actual fun function2() = 1
    actual fun function3() = 1
}

abstract class D1 {
    abstract val property1: Int
    abstract fun function1(): Int
}

actual interface D2 {
    actual val property2: Int
    actual fun function2(): Int
}

actual class D3 actual constructor() : D1(), D2 {
    actual override val property1 = 1
    actual override val property2 = 1
    actual val property3 = 1

    actual override fun function1() = 1
    actual override fun function2() = 1
    actual fun function3() = 1
}
