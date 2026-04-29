package kotlin

public fun error(message: Any): Nothing = throw IllegalStateException(message.toString())

public inline val Char.code: Int get() = this.toInt()

public infix fun Int.until(to: Int): kotlin.ranges.IntRange = kotlin.ranges.IntRange(this, to - 1)
