// DUMP_KT_IR

import kotlin.explain.*

interface TypeA {
    fun describe(value: Any): String?
}

abstract class TypeB : TypeA {
    @ExplainCall // TODO should we even support this?
    override fun describe(value: Any): String? {
        return ExplainCall.explanation?.toDefaultMessage()
    }
}

abstract class TypeC : TypeB() {
    override fun describe(value: Any): String? {
        return super.describe(value)
    }
}

data object TypeD : TypeC()

fun box(): String = runAllOutput(
    "callTypeA" to ::callTypeA,
    "callTypeB" to ::callTypeB,
    "callTypeC" to ::callTypeC,
    "callTypeD" to ::callTypeD,
)

fun callTypeA(): String {
    val type: TypeA = TypeD
    return type.describe(1 == 2) ?: "no description"
}

fun callTypeB(): String {
    val type: TypeB = TypeD
    return type.describe(1 == 2) ?: "no description"
}

fun callTypeC(): String {
    val type: TypeC = TypeD
    return type.describe(1 == 2) ?: "no description"
}

fun callTypeD(): String {
    val type: TypeD = TypeD
    return type.describe(1 == 2) ?: "no description"
}
