@file:OptIn(kotlin.experimental.ExperimentalObjCName::class)

package objcNameMangling

interface InterfaceNameManglingI1 {
    val clashingProperty: Int

    fun clashingMethod(): Int

    @ObjCName(name = "interfaceClashingMethodWithObjCNameInI1")
    fun clashingMethodWithObjCNameInI1(): Int

    fun interfaceClashingMethodWithObjCNameInI2(): Int

    @ObjCName(name = "interfaceClashingMethodWithObjCNameInBoth")
    fun clashingMethodWithObjCNameInBoth(): Int
}

fun i1() = object : InterfaceNameManglingI1 {
    override val clashingProperty: Int
        get() = 1

    override fun clashingMethod(): Int = 2

    override fun clashingMethodWithObjCNameInI1(): Int = 3

    override fun interfaceClashingMethodWithObjCNameInI2(): Int = 4

    override fun clashingMethodWithObjCNameInBoth(): Int = 5
}

interface InterfaceNameManglingI2 {
    val clashingProperty: Any

    fun clashingMethod(): Any

    fun interfaceClashingMethodWithObjCNameInI1(): Any

    @ObjCName(name = "interfaceClashingMethodWithObjCNameInI2")
    fun clashingMethodWithObjCNameInI2(): Any

    @ObjCName(name = "interfaceClashingMethodWithObjCNameInBoth")
    fun clashingMethodWithObjCNameInBoth(): Any
}

fun i2() = object : InterfaceNameManglingI2 {
    override val clashingProperty: Any
        get() = "one"

    override fun clashingMethod(): Any = "two"

    override fun interfaceClashingMethodWithObjCNameInI1(): Any = "three"

    override fun clashingMethodWithObjCNameInI2(): Any = "four"

    override fun clashingMethodWithObjCNameInBoth(): Any = "five"
}

class InterfaceNameManglingC1 {
    val clashingProperty: String = "one"

    fun clashingMethod(): String = "two"
}

final class InterfaceNameManglingC2 {
    val clashingProperty: Int = 1

    fun clashingMethod(): Int = 2
}

fun o1() = InterfaceNameManglingC1()

fun o2() = InterfaceNameManglingC2()