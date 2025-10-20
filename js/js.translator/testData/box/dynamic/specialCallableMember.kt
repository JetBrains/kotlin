// ISSUE: KT-77320
// WITH_STDLIB

package foo

external interface Big {
    fun unaryPlus(): dynamic
    fun unaryMinus(): dynamic
    fun not(): dynamic
    fun plus(other: Big): dynamic
    fun minus(other: Big): dynamic
    fun times(other: Big): dynamic
    fun div(other: Big): dynamic
    fun rem(other: Big): dynamic
    fun and(other: Big): dynamic
    fun or(other: Big): dynamic
    fun equals(other: Big): dynamic
    fun plusAssign(other: Big): dynamic
    fun minusAssign(other: Big): dynamic
    fun timesAssign(other: Big): dynamic
    fun divAssign(other: Big): dynamic
    fun remAssign(other: Big): dynamic
}

class BigImpl(val value: Int): Big {
    override fun unaryPlus(): dynamic = "OK"
    override fun unaryMinus(): dynamic = "OK"
    override fun not(): dynamic = "OK"
    override fun plus(other: Big): dynamic = "OK"
    override fun minus(other: Big): dynamic = "OK"
    override fun times(other: Big): dynamic = "OK"
    override fun div(other: Big): dynamic = "OK"
    override fun rem(other: Big): dynamic = "OK"
    override fun and(other: Big): dynamic = "OK"
    override fun or(other: Big): dynamic = "OK"
    override fun equals(other: Big): dynamic = "OK"
    override fun plusAssign(other: Big): dynamic = "OK"
    override fun minusAssign(other: Big): dynamic = "OK"
    override fun timesAssign(other: Big): dynamic = "OK"
    override fun divAssign(other: Big): dynamic = "OK"
    override fun remAssign(other: Big): dynamic = "OK"
}

fun createBig(value: Int): dynamic = BigImpl(value)

fun box(): String {
    val a = createBig(1)
    val b = createBig(2)
    val results = listOf<Pair<String, dynamic>>(
        "unaryPlus" to a.unaryPlus(),
        "unaryMinus" to a.unaryMinus(),
        "not" to a.not(),
        "plus" to a.plus(b),
        "minus" to a.minus(b),
        "times" to a.times(b),
        "div" to a.div(b),
        "rem" to a.rem(b),
        "and" to a.and(b),
        "or" to a.or(b),
        "equals" to a.equals(b),
        "plusAssign" to a.plusAssign(b),
        "minusAssign" to a.minusAssign(b),
        "timesAssign" to a.timesAssign(b),
        "divAssign" to a.divAssign(b),
        "remAssign" to a.remAssign(b)
    )

    val failed = results.filter { it.second != "OK" }
    if (failed.any()) return failed.map { "OK expected from '${it.first}', got '${it.second}'" }.joinToString("\n")

    return "OK"
}