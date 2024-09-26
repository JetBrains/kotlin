// KIND: STANDALONE
// MODULE: overrides
// EXPORT_TO_SWIFT
// FILE: inheritance.kt

open class Parent() {
    open fun nonoverride(): Int = 42

    open fun primitiveTypeFunc(arg: Int): Int = arg
    open fun objectFunc(arg: Child): Parent = arg
    open fun objectOptionalFunc(arg: Child): Parent? = null
    open fun subtypeObjectFunc(arg: Child): Parent = arg
    open fun subtypeOptionalPrimitiveFunc(): Int? = null
    open fun subtypeOptionalObjectFunc(): Parent? = null

    open val primitiveTypeVar: Int get() = 42
    open val objectVar: Parent get() = this
    open val objectOptionalVar: Parent? get() = null
    open val subtypeObjectVar: Parent get() = this
    open val subtypeOptionalPrimitiveVar: Int? get() = null
    open val subtypeOptionalObjectVar: Parent? get() = null

    open fun hopFunc() = Unit
    open fun finalOverrideFunc() = Unit
    open fun finalOverrideHopFunc() = Unit
    open fun overrideChainFunc() = Unit
}

open class Child : Parent() {
    override fun nonoverride(): Nothing = TODO("This is not an override in swift")

    override fun primitiveTypeFunc(arg: Int): Int = 43
    override fun objectFunc(arg: Child): Parent = this
    override fun objectOptionalFunc(arg: Child): Parent? = null
    override fun subtypeObjectFunc(arg: Child): Child = this
    override fun subtypeOptionalPrimitiveFunc(): Int = 42
    override fun subtypeOptionalObjectFunc(): Child = this

    override val primitiveTypeVar: Int get() = 45
    override val objectVar: Parent get() = this
    override val objectOptionalVar: Parent? get() = this
    override val subtypeObjectVar: Child get() = this
    override val subtypeOptionalPrimitiveVar: Int get() = 42
    override val subtypeOptionalObjectVar: Child get() = this

    final override fun finalOverrideFunc() {}
    open override fun overrideChainFunc() = Unit
}

class GrandChild : Child() {
    override fun hopFunc() {}
    final override fun finalOverrideHopFunc() {}
    final override fun overrideChainFunc() = Unit
}

// MODULE: overrides_across_modules(overrides)
// EXPORT_TO_SWIFT
// FILE: overrides_across_modules.kt

open class Cousin : Parent() {
    override fun primitiveTypeFunc(arg: Int): Int = 10
    override val primitiveTypeVar: Int get() = 20
    final override fun finalOverrideFunc() {}
}