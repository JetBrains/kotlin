actual public val publicProperty = 1
actual internal val publicOrInternalProperty = 1
actual internal val internalProperty = 1
private val internalOrPrivateProperty = 1
private val privateProperty = 1

actual public fun publicFunction() = 1
actual internal fun publicOrInternalFunction() = 1
actual internal fun internalFunction() = 1
private fun internalOrPrivateFunction() = 1
private fun privateFunction() = 1

actual open class Outer1 actual constructor() {
    actual public val publicProperty = 1
    actual internal val publicOrInternalProperty = 1
    actual internal val internalProperty = 1
    private val internalOrPrivateProperty = 1
    private val privateProperty = 1

    actual public fun publicFunction() = 1
    actual internal fun publicOrInternalFunction() = 1
    actual internal fun internalFunction() = 1
    private fun internalOrPrivateFunction() = 1
    private fun privateFunction() = 1

    actual open class Inner1 actual constructor() {
        actual public val publicProperty = 1
        actual internal val publicOrInternalProperty = 1
        actual internal val internalProperty = 1
        private val internalOrPrivateProperty = 1
        private val privateProperty = 1

        actual public fun publicFunction() = 1
        actual internal fun publicOrInternalFunction() = 1
        actual internal fun internalFunction() = 1
        private fun internalOrPrivateFunction() = 1
        private fun privateFunction() = 1
    }
}

actual open class Outer2 actual constructor() {
    actual public open val publicProperty = 1
    internal open val publicOrInternalProperty = 1
    actual internal open val internalProperty = 1
    private val internalOrPrivateProperty = 1
    private val privateProperty = 1

    actual public open fun publicFunction() = 1
    internal open fun publicOrInternalFunction() = 1
    actual internal open fun internalFunction() = 1
    private fun internalOrPrivateFunction() = 1
    private fun privateFunction() = 1

    actual open class Inner2 actual constructor() {
        actual public open val publicProperty = 1
        internal open val publicOrInternalProperty = 1
        actual internal open val internalProperty = 1
        private val internalOrPrivateProperty = 1
        private val privateProperty = 1

        actual public open fun publicFunction() = 1
        internal open fun publicOrInternalFunction() = 1
        actual internal open fun internalFunction() = 1
        private fun internalOrPrivateFunction() = 1
        private fun privateFunction() = 1
    }
}
