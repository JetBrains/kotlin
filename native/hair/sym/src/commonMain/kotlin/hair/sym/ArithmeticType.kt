package hair.sym

enum class ArithmeticType {
    INT, LONG, FLOAT, DOUBLE;

    fun toHairType(): HairType = when (this) {
        INT -> HairType.INT
        LONG -> HairType.LONG
        FLOAT -> HairType.FLOAT
        DOUBLE -> HairType.DOUBLE
    }
}

fun HairType.asArithmeticType(): ArithmeticType =
    asArithmeticTypeOrNull() ?: error("$this is not an arithmetic type")

fun HairType.asArithmeticTypeOrNull(): ArithmeticType? = when (this) {
    HairType.INT -> ArithmeticType.INT
    HairType.LONG -> ArithmeticType.LONG
    HairType.FLOAT -> ArithmeticType.FLOAT
    HairType.DOUBLE -> ArithmeticType.DOUBLE
    else -> null
}
