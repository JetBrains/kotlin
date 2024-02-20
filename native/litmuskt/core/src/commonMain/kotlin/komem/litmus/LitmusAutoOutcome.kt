package komem.litmus

interface LitmusAutoOutcome {
    fun getOutcome(): LitmusOutcome
}

open class LitmusIOutcome(
    var r1: Int = 0,
) : LitmusAutoOutcome {
    override fun getOutcome() = r1 // single values are handled differently
}

open class LitmusIIOutcome(
    var r1: Int = 0,
    var r2: Int = 0
) : LitmusAutoOutcome {
    override fun getOutcome() = listOf(r1, r2)
}

open class LitmusIIIOutcome(
    var r1: Int = 0,
    var r2: Int = 0,
    var r3: Int = 0,
) : LitmusAutoOutcome {
    override fun getOutcome() = listOf(r1, r2, r3)
}

open class LitmusIIIIOutcome(
    var r1: Int = 0,
    var r2: Int = 0,
    var r3: Int = 0,
    var r4: Int = 0,
) : LitmusAutoOutcome {
    override fun getOutcome() = listOf(r1, r2, r3, r4)
}