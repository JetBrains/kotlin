interface A {
    val propertyA: Int
    val propertyAForOverride: Int
    fun funA(): Any
    fun funAForOverride(): Any
}

interface B : A {
    val propertyB: Boolean
    fun funB()
    override val propertyAForOverride: Int
        get() = 42

    override fun funAForOverride(): String = ""
}

interface C : B {
    override fun funAForOverride(): String = ""
}

interface D : C