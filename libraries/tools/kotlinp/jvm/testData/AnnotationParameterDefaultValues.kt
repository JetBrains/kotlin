// WITH_STDLIB

import kotlin.reflect.KClass

annotation class Primitives(
    val z: Boolean = true,
    val c: Char = 'c',
    val b: Byte = 1.toByte(),
    val s: Short = 2.toShort(),
    val i: Int = 42,
    val f: Float = 3.14f,
    val j: Long = Long.MAX_VALUE,
    val d: Double = Double.NaN,
)

annotation class PrimitiveArrays(
    val za: BooleanArray = [true],
    val ca: CharArray = ['c'],
    val ba: ByteArray = [1.toByte()],
    val sa: ShortArray = [2.toShort()],
    val ia: IntArray = [42],
    val fa: FloatArray = [3.14f],
    val ja: LongArray = [Long.MAX_VALUE],
    val da: DoubleArray = [Double.NaN],
)

annotation class Unsigned(
    val ui: UInt = UInt.MAX_VALUE,
    val ub: UByte = UByte.MIN_VALUE,
    val us: UShort = UShort.MIN_VALUE,
    val ul: ULong = ULong.MAX_VALUE,
)

annotation class Other(
    val str: String = "OK",
    val enum: AnnotationTarget = AnnotationTarget.CLASS,
    val klass: KClass<*> = B::class,
    val klass2: KClass<*> = IntArray::class,
    val anno: B = B(value = "B"),
    val stra: Array<String> = ["OK"],
    val ka: Array<KClass<*>> = [Double::class, Unit::class, LongArray::class, Array<String>::class],
    val ea: Array<AnnotationTarget> = [AnnotationTarget.TYPEALIAS, AnnotationTarget.FIELD],
    val aa: Array<B> = [B("1"), B("2")],
)

annotation class B(val value: String)
