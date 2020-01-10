package fieldInitializers

object JavaConst {
    val boolValue: Boolean = java.lang.Boolean.TRUE
    val charValue: Char = java.lang.Character.MAX_VALUE
    val byteValue: Byte = java.lang.Byte.MIN_VALUE
    val shortValue: Short = java.lang.Short.MIN_VALUE
    val intValue: Int = java.lang.Byte.SIZE
    val longValue: Long = java.lang.Long.MAX_VALUE
    val floatValue: Float = java.lang.Float.MAX_VALUE
    val floatInf: Float = java.lang.Float.POSITIVE_INFINITY
    val floatNegInf: Float = java.lang.Float.NEGATIVE_INFINITY
    val floatNan: Float = java.lang.Float.NaN
    val doubleValue: Double = java.lang.Double.MAX_VALUE
    val doubleInf: Double = java.lang.Double.POSITIVE_INFINITY
    val doubleNegInf: Double = java.lang.Double.NEGATIVE_INFINITY
    val doubleNan: Double = java.lang.Double.NaN
    val separator: String = java.io.File.separator
}

object KotlinMaxValue {
    val byteMax = Byte.MAX_VALUE
    val shortMax = Short.MAX_VALUE
    val intMax = Int.MAX_VALUE
    val longMax = Long.MAX_VALUE
    val charMax = Char.MAX_VALUE
    val floatMax = Float.MAX_VALUE
    val doubleMax = Double.MAX_VALUE
}

object KotlinConst {
    const val n: Int = 1
    const val b: Boolean = true
    const val s: String = "foo"
}

object KotlinVal {
    val n: Int = 1
    val b: Boolean = true
    val s: String = "foo"
}

object KotlinUsage {
    val nConst = KotlinConst.n
    val bConst = KotlinConst.b
    val sConst = KotlinConst.s

    val n = KotlinVal.n
    val b = KotlinVal.b
    val s = KotlinVal.s
}

val topLevelJava = java.lang.Long.MAX_VALUE
val topLevelKotlin = KotlinConst.n