// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: Inheritance
// FILE: class_delegation.kt

interface DelegatedContract {
    fun delegatedValue(): String
}

private class DelegatedImplementation : DelegatedContract {
    override fun delegatedValue(): String = "delegated-implementation"
}

open class DelegatingBase : DelegatedContract by DelegatedImplementation() {
    open fun localValue(): String = "delegating-base"
}

class DelegatingStorage {
    private var stored: DelegatingBase? = null

    fun store(value: DelegatingBase) {
        stored = value
    }

    fun retrieve(): DelegatingBase? = stored
}

fun callDelegatedValue(value: DelegatedContract): String = value.delegatedValue()
fun callDelegatingLocal(value: DelegatingBase): String = value.localValue()
