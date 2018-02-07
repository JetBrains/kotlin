
annotation class IntRange(val from: Long, val to: Long)

annotation class RequiresPermission(val anyOf: IntArray)

annotation class WithDefaultValue(val value: Int = 42)

annotation class SuppressLint(vararg val value: String)

@RequiresPermission(anyOf = intArrayOf(1, 2, 3))
@IntRange(from = 10, to = 0)
@WithDefaultValue
@SuppressLint("Lorem")
fun foo(): Int = 5

@IntRange(0, 100)
@SuppressLint("Lorem", "Ipsum", "Dolor")
fun bar() = Unit