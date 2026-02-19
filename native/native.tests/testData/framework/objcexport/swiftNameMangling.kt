@file:OptIn(kotlin.experimental.ExperimentalObjCName::class)

package swiftNameMangling

interface SwiftNameManglingI1 {
    val clashingProperty: Int

    fun clashingMethod(): Int

    @ObjCName(swiftName = "swiftClashingMethodWithObjCNameInI1")
    fun clashingMethodWithObjCNameInI1(): Int

    fun swiftClashingMethodWithObjCNameInI2(): Int

    @ObjCName(swiftName = "swiftClashingMethodWithObjCNameInBoth")
    fun clashingMethodWithObjCNameInBoth(): Int
}

fun i1() = object : SwiftNameManglingI1 {
    override val clashingProperty: Int
        get() = 1

    override fun clashingMethod(): Int = 2

    override fun clashingMethodWithObjCNameInI1(): Int = 3

    override fun swiftClashingMethodWithObjCNameInI2(): Int = 4

    override fun clashingMethodWithObjCNameInBoth(): Int = 5
}

interface SwiftNameManglingI2 {
    val clashingProperty: Any

    fun clashingMethod(): Any

    fun swiftClashingMethodWithObjCNameInI1(): Any

    @ObjCName(swiftName = "swiftClashingMethodWithObjCNameInI2")
    fun clashingMethodWithObjCNameInI2(): Any

    @ObjCName(swiftName = "swiftClashingMethodWithObjCNameInBoth")
    fun clashingMethodWithObjCNameInBoth(): Any
}

fun i2() = object : SwiftNameManglingI2 {
    override val clashingProperty: Any
        get() = "one"

    override fun clashingMethod(): Any = "two"

    override fun swiftClashingMethodWithObjCNameInI1(): Any = "three"

    override fun clashingMethodWithObjCNameInI2(): Any = "four"

    override fun clashingMethodWithObjCNameInBoth(): Any = "five"
}
