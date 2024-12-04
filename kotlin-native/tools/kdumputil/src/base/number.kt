package base

fun Int.align(alignment: Int) = alignment.dec().let { mask -> this.plus(mask).and(mask.inv()) }

fun Byte.toIntUnsigned(): Int = toInt().and(0xff)
fun Short.toIntUnsigned(): Int = toInt().and(0xffff)

fun Byte.toLongUnsigned(): Long = toLong().and(0xff)
fun Short.toLongUnsigned(): Long = toLong().and(0xffff)
fun Int.toLongUnsigned(): Long = toLong().and(0xffffffffL)
