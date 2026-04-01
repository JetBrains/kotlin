// DUMP_KT_IR

// MODULE: lib
// FILE: A.kt

import kotlin.powerassert.*

interface TypeA {
    @PowerAssert
    fun describe(value: Any): String?
}

abstract class TypeB : TypeA {
    override fun describe(value: Any): String? {
        return "TypeB:\n" + PowerAssert.explanation?.toDefaultMessage()
    }

    override fun toString(): String {
        return "TypeB"
    }
}

open class TypeC : TypeB() {
    override fun describe(value: Any): String? {
        return "TypeC:\n" + super.describe(value)
    }

    override fun toString(): String {
        return "TypeC"
    }
}

data object TypeD : TypeC()

data object TypeE : TypeC() {
    override fun describe(value: Any): String? {
        return "TypeE:\n" + TypeC().describe(value)
    }
}

data object TypeF : TypeC() {
    override fun describe(value: Any): String? {
        return "TypeF:\n" + PowerAssert.explanation?.toDefaultMessage()
    }
}

// MODULE: main(lib)
// FILE: B.kt

fun box(): String = runAllOutput(
    "callTypeA" to ::callTypeA,
    "callTypeB" to ::callTypeB,
    "callTypeC" to ::callTypeC,
    "callTypeD" to ::callTypeD,
    "callTypeE" to ::callTypeE,
    "callTypeF" to ::callTypeF,
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

fun callTypeF(): String {
    val type: TypeF = TypeF
    return type.describe(1 == 2) ?: "no description"
}
