// https://github.com/JetBrains/kotlin-native/issues/3290: FastISel at -O0 miscompiled sign extension of
// small integer types on arm64, so printing a negative Byte crashed in debug builds.

fun byteOf(value: Int): Byte = value.toByte()
fun shortOf(value: Int): Short = value.toShort()

fun byteToInt(value: Byte): Int = value.toInt()
fun byteToLong(value: Byte): Long = value.toLong()
fun shortToInt(value: Short): Int = value.toInt()
fun shortToLong(value: Short): Long = value.toLong()

fun box(): String {
    // The original reproducer.
    println((-1).toByte())

    val byteValues = intArrayOf(-128, -127, -2, -1, 0, 1, 127)
    for (value in byteValues) {
        val b = byteOf(value)
        if (byteToInt(b) != value) return "FAIL byte->int: $value -> ${byteToInt(b)}"
        if (byteToLong(b) != value.toLong()) return "FAIL byte->long: $value -> ${byteToLong(b)}"
        if (b.toString() != value.toString()) return "FAIL byte.toString: $value -> $b"
        val boxed: Any = b
        if (boxed != b || boxed.toString() != value.toString()) return "FAIL boxed byte: $value -> $boxed"
        if ((b < 0) != (value < 0)) return "FAIL byte sign: $value"
    }
    if (byteOf(255) != (-1).toByte() || byteToInt(byteOf(255)) != -1) return "FAIL byte truncation: 255"
    if (byteToInt(byteOf(0x180)) != -128) return "FAIL byte truncation: 0x180"

    val shortValues = intArrayOf(-32768, -32767, -129, -128, -1, 0, 1, 32767)
    for (value in shortValues) {
        val s = shortOf(value)
        if (shortToInt(s) != value) return "FAIL short->int: $value -> ${shortToInt(s)}"
        if (shortToLong(s) != value.toLong()) return "FAIL short->long: $value -> ${shortToLong(s)}"
        if (s.toString() != value.toString()) return "FAIL short.toString: $value -> $s"
        val boxed: Any = s
        if (boxed != s || boxed.toString() != value.toString()) return "FAIL boxed short: $value -> $boxed"
        if ((s < 0) != (value < 0)) return "FAIL short sign: $value"
    }
    if (shortToInt(shortOf(0xFFFF)) != -1) return "FAIL short truncation: 0xFFFF"

    val bytes = byteArrayOf(-1, -128, 127)
    if (bytes.sum() != -2) return "FAIL byte array sum: ${bytes.sum()}"
    if (bytes.joinToString() != "-1, -128, 127") return "FAIL byte array: ${bytes.joinToString()}"
    val shorts = shortArrayOf(-1, -32768, 32767)
    if (shorts.sum() != -2) return "FAIL short array sum: ${shorts.sum()}"
    if (shorts.joinToString() != "-1, -32768, 32767") return "FAIL short array: ${shorts.joinToString()}"

    return "OK"
}
