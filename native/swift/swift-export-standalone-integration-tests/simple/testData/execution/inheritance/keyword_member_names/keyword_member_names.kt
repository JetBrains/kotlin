// KIND: STANDALONE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
// MODULE: KeywordMemberNames
// FILE: keyword_member_names.kt

// KT-89702: members whose names are Swift keywords must be escaped in the reverse bridges,
// e.g. `_self.init()` doesn't compile, while `_self.`init`()` does.

open class Base

interface InitFunction {
    fun init(): String
}

fun callInit(value: InitFunction): String = value.init()

interface SelfVarargFunction {
    fun self(vararg values: Int): Int
}

fun callSelf(value: SelfVarargFunction): Int = value.self(1, 2, 3)

interface InitProperty {
    var init: String
}

fun getInit(value: InitProperty): String = value.init
fun setInit(value: InitProperty, newValue: String) {
    value.init = newValue
}

open class OpenInit {
    open fun init(): String = "kotlin"
}

fun callOpenInit(value: OpenInit): String = value.init()
