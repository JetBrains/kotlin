// DUMP_KT_IR

import kotlinx.powerassert.*

interface TypeA {
    fun describe(value: Any): String?
}

abstract class TypeB : TypeA {
    @PowerAssert // TODO should we even support this?
    override fun describe(value: Any): String? {
        return PowerAssert.explanation?.toDefaultMessage()
    }

    override fun toString(): String {
        return "TypeB"
    }
}

open class TypeC : TypeB() {
    override fun describe(value: Any): String? {
        return super.describe(value)
    }

    override fun toString(): String {
        return "TypeC"
    }
}

data object TypeD : TypeC()

data object TypeE : TypeC() {
    override fun describe(value: Any): String? {
        return TypeC().describe(value)
    }
}

fun box(): String = runAllOutput(
    "callTypeA" to ::callTypeA,
    "callTypeB" to ::callTypeB,
    "callTypeC" to ::callTypeC,
    "callTypeD" to ::callTypeD,
    "callTypeE" to ::callTypeE,
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

fun callTypeE(): String {
    val type: TypeE = TypeE
    return type.describe(1 == 2) ?: "no description"
}
