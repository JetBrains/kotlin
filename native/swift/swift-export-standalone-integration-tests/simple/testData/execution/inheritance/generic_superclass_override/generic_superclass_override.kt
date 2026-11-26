// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: generic_superclass_override.kt

// KT-90372: a Kotlin override that substitutes a concrete type argument for the superclass type parameter
// is exported with the concrete type, which Swift does not accept as an override of the erased member.

abstract class GenericBase<T>(val v: T) {
    open fun get(): T = v
    abstract fun run(): T
    open fun put(value: T): String = "base-put($value)"
}

class ConcreteDerived : GenericBase<String>("base") {
    override fun get() = "derived"
    override fun run() = "overridden_abstract"
    override fun put(value: String): String = "derived-put($value)"
}

open class OpenConcreteDerived : GenericBase<String>("open-base") {
    override fun get() = "open-derived"
    override fun run() = "open-run"
}

fun callGet(b: GenericBase<*>): Any? = b.get()
fun callRun(b: GenericBase<*>): Any? = b.run()
