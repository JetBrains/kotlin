// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: val_to_var_override.kt

// A Kotlin subclass may turn an inherited read-only property into a mutable one.

open class ValToVarBase {
    open val valToVar: String get() = "base"
}

class ValToVarDerived : ValToVarBase() {
    override var valToVar: String = "derived"
}

// Reads the (possibly overridden) property through the base static type.
fun readValToVar(base: ValToVarBase): String = base.valToVar
