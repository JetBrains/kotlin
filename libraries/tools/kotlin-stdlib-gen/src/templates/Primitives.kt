package templates

enum class PrimitiveType(val name: String) {
    Boolean: PrimitiveType("Boolean")
    Byte: PrimitiveType("Byte")
    Char: PrimitiveType("Char")
    Short: PrimitiveType("Short")
    Int: PrimitiveType("Int")
    Long: PrimitiveType("Long")
    Float: PrimitiveType("Float")
    Double: PrimitiveType("Double")
}

private fun PrimitiveType.zero() = when (this) {
    PrimitiveType.Int -> "0"
    PrimitiveType.Byte -> "0.toByte()"
    PrimitiveType.Short -> "0.toShort()"
    PrimitiveType.Long -> "0.toLong()"
    PrimitiveType.Double -> "0.0"
    PrimitiveType.Float -> "0.toFloat()"
    else -> null
}

fun sumFunction(primitive: PrimitiveType) = primitive.zero()?.let { zero ->
    val sum = when (primitive) {
        PrimitiveType.Byte -> "(a+b).toByte()"
        PrimitiveType.Short -> "(a+b).toShort()"
        else -> "a+b"
    }
    f("sum()") {
        doc = "Sums up the elements"
        isInline = false
        Family.Iterables.customReceiver("Iterable<${primitive.name}>")
        Family.Arrays.customReceiver("Array<${primitive.name}>")
        returns(primitive.name)
        body {
            "return fold($zero, {a,b -> $sum})"
        }
    }
}