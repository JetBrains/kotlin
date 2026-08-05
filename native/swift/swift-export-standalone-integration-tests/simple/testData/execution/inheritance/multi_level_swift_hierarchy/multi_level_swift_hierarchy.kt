// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: multi_level_swift_hierarchy.kt

open class BaseClass {
    open fun baseFunction(): String = "base-class"
}

fun callFunBaseClass(value: BaseClass): String = value.baseFunction()

open class SuperReentryBase {
    open fun callback(): String = "kotlin-callback"
    open fun operation(): String = "kotlin-operation>${callback()}"
}

fun callSuperReentry(value: SuperReentryBase): String = value.operation()
