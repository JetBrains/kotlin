import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class A(
    val b: Byte,
    val s: Short,
    val i: Int,
    val f: Float,
    val j: Long,
    val d: Double,
    val ui: UInt,
    val ub: UByte,
    val us: UShort,
    val ul: ULong,
)

class C {
    fun maxValues(): @A(
        Byte.MAX_VALUE,
        Short.MAX_VALUE,
        Int.MAX_VALUE,
        Float.MAX_VALUE,
        Long.MAX_VALUE,
        Double.MAX_VALUE,
        UInt.MAX_VALUE,
        UByte.MAX_VALUE,
        UShort.MAX_VALUE,
        ULong.MAX_VALUE,
    ) Unit {}

    fun minValues(): @A(
        Byte.MIN_VALUE,
        Short.MIN_VALUE,
        Int.MIN_VALUE,
        Float.MIN_VALUE,
        Long.MIN_VALUE,
        Double.MIN_VALUE,
        UInt.MIN_VALUE,
        UByte.MIN_VALUE,
        UShort.MIN_VALUE,
        ULong.MIN_VALUE,
    ) Unit {}
}

typealias Byte0 = Byte
typealias Short0 = Short
typealias Int0 = Int
typealias Float0 = Float
typealias Long0 = Long
typealias Double0 = Double
typealias UInt0 = UInt
typealias UByte0 = UByte
typealias UShort0 = UShort
typealias ULong0 = ULong

@Target(AnnotationTarget.TYPE)
annotation class Aliased(
    val b: Byte0,
    val s: Short0,
    val i: Int0,
    val f: Float0,
    val j: Long0,
    val d: Double0,
    val ui: UInt0,
    val ub: UByte0,
    val us: UShort0,
    val ul: ULong0,
)

class D {
    fun maxValues(): @Aliased(
        Byte.MAX_VALUE,
        Short.MAX_VALUE,
        Int.MAX_VALUE,
        Float.MAX_VALUE,
        Long.MAX_VALUE,
        Double.MAX_VALUE,
        UInt.MAX_VALUE,
        UByte.MAX_VALUE,
        UShort.MAX_VALUE,
        ULong.MAX_VALUE,
    ) Unit {}

    fun minValues(): @Aliased(
        Byte.MIN_VALUE,
        Short.MIN_VALUE,
        Int.MIN_VALUE,
        Float.MIN_VALUE,
        Long.MIN_VALUE,
        Double.MIN_VALUE,
        UInt.MIN_VALUE,
        UByte.MIN_VALUE,
        UShort.MIN_VALUE,
        ULong.MIN_VALUE,
    ) Unit {}
}
