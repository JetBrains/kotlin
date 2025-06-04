// KIND: STANDALONE
// MODULE: overrides
// EXPORT_TO_SWIFT
// FILE: inheritance.kt

open class Parent(val value: String) {
    open fun actuallyOverride(nullable: Int, poly: Child, nullablePoly: Child) = Unit

    open fun nonoverride(): Int = 42

    open fun primitiveTypeFunc(arg: Int): Int = arg
    open fun objectFunc(arg: Child): Parent = arg
    open fun objectOptionalFunc(arg: Child): Parent? = null
    open fun subtypeObjectFunc(arg: Child): Parent = arg
    open fun subtypeOptionalPrimitiveFunc(): Int? = null
    open fun subtypeOptionalObjectFunc(): Parent? = null
    open fun genericReturnTypeFunc(): List<Parent> = emptyList()
//    open fun functionTypeFunc(arg: (Child) -> Parent): (Child) -> Parent = { TODO() }

    open val primitiveTypeVar: Int get() = 42
    open val objectVar: Parent get() = this
    open val objectOptionalVar: Parent? get() = null
    open val subtypeObjectVar: Parent get() = this
    open val subtypeOptionalPrimitiveVar: Int? get() = null
    open val subtypeOptionalObjectVar: Parent? get() = null
//    open fun subtypeFunctionTypeFunc(arg: (Child) -> Parent): (Child) -> Parent = { TODO() }

    open fun hopFunc() = Unit
    open fun finalOverrideFunc() = Unit
    open fun finalOverrideHopFunc() = Unit
    open fun overrideChainFunc() = Unit
}

open class Child(value: Int) : Parent("$value") {
    constructor(nullable: Int, poly: Parent, nullablePoly: Parent): this(42)
    constructor(value: String): this(43)

    open fun actuallyOverride(nullable: Int?, poly: Parent, nullablePoly: Parent?): Unit =
        TODO("This is actually an override in swift")

    override fun nonoverride(): Nothing = TODO("This is not an override in swift")

    override fun primitiveTypeFunc(arg: Int): Int = 43
    override fun objectFunc(arg: Child): Parent = this
    override fun objectOptionalFunc(arg: Child): Parent? = null
    override fun subtypeObjectFunc(arg: Child): Child = this
    override fun subtypeOptionalPrimitiveFunc(): Int = 42
    override fun subtypeOptionalObjectFunc(): Child = this
    override fun genericReturnTypeFunc(): List<Child> = emptyList()
//    override fun functionTypeFunc(arg: (Child) -> Parent): (Child) -> Parent = { TODO() }

    override val primitiveTypeVar: Int get() = 45
    override val objectVar: Parent get() = this
    override val objectOptionalVar: Parent? get() = this
    override val subtypeObjectVar: Child get() = this
    override val subtypeOptionalPrimitiveVar: Int get() = 42
    override val subtypeOptionalObjectVar: Child get() = this
//    override fun subtypeFunctionTypeFunc(arg: (Parent) -> Child): (Parent) -> Child = { TODO() }

    final override fun finalOverrideFunc() {}
    open override fun overrideChainFunc() = Unit
}

class GrandChild(value: Int) : Child(value) {
    override fun hopFunc() {}
    final override fun finalOverrideHopFunc() {}
    final override fun overrideChainFunc() = Unit
}

abstract class AbstractBase {
    constructor() {}
    constructor(x: Int) : this() {}

    abstract val abstractVal: Int

    abstract fun abstractFun1()
    abstract fun abstractFun2()
}

open class OpenDerived1 : AbstractBase {
    constructor() : super() {}
    constructor(x: Int) : this() {}

    override val abstractVal: Int = 11

    override fun abstractFun1() {}
    override fun abstractFun2() {}
}

abstract class AbstractDerived2 : OpenDerived1 {
    constructor() : super() {}
    constructor(x: Int) : this() {}

    override abstract fun abstractFun1()
}

// MODULE: overrides_across_modules(overrides)
// EXPORT_TO_SWIFT
// FILE: overrides_across_modules.kt

open class Cousin(value: String) : Parent(value) {
    override fun primitiveTypeFunc(arg: Int): Int = 10
    override val primitiveTypeVar: Int get() = 20
    final override fun finalOverrideFunc() {}
}

class FinalDerived3 : AbstractDerived2 {
    constructor() : super() {}
    constructor(x: Int) : this() {}

    override fun abstractFun1() {}
}

// MODULE: bad_overrides(overrides)
// EXPORT_TO_SWIFT
// FILE: overrides_across_modules.kt

package weird

open class A {
    @Throws(Throwable::class)
    constructor() {
        // this implicitly [non]overrides the default constructor of KotlinBase
    }

    @Throws(Throwable::class)
    open fun throws() {

    }

    @Deprecated("", level = DeprecationLevel.ERROR)
    open fun foo() {}

    open val bar: Int get() = 42
}

class B: A {
    constructor() : super() {

    }

    override fun throws() {

    }

    override fun foo() {}

    override val bar: Nothing get() = error("42")
}