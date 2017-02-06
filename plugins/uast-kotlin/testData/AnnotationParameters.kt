
annotation class IntRange(val from: Long, val to: Long)

@IntRange(from = 10, to = 0)
fun foo(): Int = 5