interface A1 {
    val property1: Int
    fun function1(): Int
}

abstract class A2 : A1 {
    abstract val property2: Int
    abstract fun function2(): Int
}

class A3 : A2() {
    override val property1 = 1
    override val property2 = 1
    val property3 = 1

    override fun function1() = 1
    override fun function2() = 1
    fun function3() = 1
}

interface B1 {
    val property1: Int
    fun function1(): Int
}

interface B2 {
    val property2: Int
    fun function2(): Int
}

class B3 : B1, B2 {
    override val property1 = 1
    override val property2 = 1
    val property3 = 1

    override fun function1() = 1
    override fun function2() = 1
    fun function3() = 1
}

interface C1 {
    val property1: Int
    fun function1(): Int
}

interface C2 {
    val property2: Int
    fun function2(): Int
}

class C3 : C1, C2 {
    override val property1 = 1
    override val property2 = 1
    val property3 = 1

    override fun function1() = 1
    override fun function2() = 1
    fun function3() = 1
}

interface D1 {
    val property1: Int
    fun function1(): Int
}

interface D2 {
    val property2: Int
    fun function2(): Int
}

class D3 : D1, D2 {
    override val property1 = 1
    override val property2 = 1
    val property3 = 1

    override fun function1() = 1
    override fun function2() = 1
    fun function3() = 1
}
