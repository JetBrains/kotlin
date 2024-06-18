@JvmInline
value class InvisibleInlineClass internal constructor(private val raw: Int) {
    data class InvisibleNested(val a: Int)
}