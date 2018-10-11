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
    val anno: B
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
        B(value = "aba\ncaba'\"\t\u0001\u0002\uA66E")
    ) Unit {}

    fun parameterTypeAnnotation(p: @JvmNamed("Q_Q") Any): Any = p
}
