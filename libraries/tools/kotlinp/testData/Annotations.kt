// IGNORE K2

import kotlin.reflect.KClass

@Target(AnnotationTarget.TYPE)
annotation class A(
    val z: Boolean,
    val c: Char,
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
    val ui_max: UInt,
    val ub_max: UByte,
    val us_max: UShort,
    val ul_max: ULong,
    val za: BooleanArray,
    val ca: CharArray,
    val ba: ByteArray,
    val sa: ShortArray,
    val ia: IntArray,
    val fa: FloatArray,
    val ja: LongArray,
    val da: DoubleArray,
    val str: String,
    val enum: AnnotationTarget,
    val klass: KClass<*>,
    val klass2: KClass<*>,
    val anno: B,
    val stra: Array<String>,
    val ka: Array<KClass<*>>,
    val ea: Array<AnnotationTarget>,
    val aa: Array<B>
)

annotation class B(val value: String)

@Target(AnnotationTarget.TYPE)
annotation class JvmNamed(@get:JvmName("uglyJvmName") val value: String)

class C {
    fun returnTypeAnnotation(): @A(
        true,
        'x',
        1.toByte(),
        42.toShort(),
        42424242,
        -2.72f,
        239239239239239L,
        3.14,
        1u,
        0xFFu,
        3u,
        4uL,
        0xFFFF_FFFFu,
        UByte.MAX_VALUE,
        0xFF_FFu,
        18446744073709551615u,
        [true],
        ['\''],
        [1.toByte()],
        [42.toShort()],
        [42424242],
        [-2.72f],
        [239239239239239L],
        [3.14],
        "aba\ncaba'\"\t\u0001\u0002\uA66E",
        AnnotationTarget.CLASS,
        C::class,
        IntArray::class,
        B(value = "aba\ncaba'\"\t\u0001\u0002\uA66E"),
        ["lmao"],
        [Double::class, Unit::class, LongArray::class, Array<String>::class],
        [AnnotationTarget.TYPEALIAS, AnnotationTarget.FIELD],
        [B("2"), B(value = "3")]
    ) Unit {}

    fun parameterTypeAnnotation(p: @JvmNamed("Q_Q") Any): Any = p
}
