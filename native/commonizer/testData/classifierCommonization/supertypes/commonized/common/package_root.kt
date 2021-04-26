expect interface A1 {
    val property1: Int
    fun function1(): Int
}

expect abstract class A2() : A1 {
    abstract val property2: Int
    abstract fun function2(): Int
}

expect class A3() : A2 {
    override val property1: Int
    override val property2: Int
    val property3: Int

    override fun function1(): Int
    override fun function2(): Int
    fun function3(): Int
}

expect interface B1 {
    val property1: Int
    fun function1(): Int
}

expect class B3() : B1 {
    override val property1: Int
    open val property2: Int
    val property3: Int

    override fun function1(): Int
    open fun function2(): Int
    fun function3(): Int
}

expect interface C1 {
    val property1: Int
    fun function1(): Int
}

expect class C3() : C1 {
    override val property1: Int
    val property2: Int
    val property3: Int

    override fun function1(): Int
    fun function2(): Int
    fun function3(): Int
}

expect interface D2 {
    val property2: Int
    fun function2(): Int
}

expect class D3() : D2 {
    open val property1: Int
    override val property2: Int
    val property3: Int

    open fun function1(): Int
    override fun function2(): Int
    fun function3(): Int
}
