actual public val publicProperty = 1
actual public val publicOrInternalProperty = 1
actual internal val internalProperty = 1
internal val internalOrPrivateProperty = 1
private val privateProperty = 1

actual public fun publicFunction() = 1
actual public fun publicOrInternalFunction() = 1
actual internal fun internalFunction() = 1
internal fun internalOrPrivateFunction() = 1
private fun privateFunction() = 1

actual open class Outer1 actual constructor() {
    actual public val publicProperty = 1
    actual public val publicOrInternalProperty = 1
    actual internal val internalProperty = 1
    internal val internalOrPrivateProperty = 1
    private val privateProperty = 1

    actual public fun publicFunction() = 1
    actual public fun publicOrInternalFunction() = 1
    actual internal fun internalFunction() = 1
    internal fun internalOrPrivateFunction() = 1
    private fun privateFunction() = 1

    actual open class Inner1 actual constructor() {
        actual public val publicProperty = 1
        actual public val publicOrInternalProperty = 1
        actual internal val internalProperty = 1
        internal val internalOrPrivateProperty = 1
        private val privateProperty = 1

        actual public fun publicFunction() = 1
        actual public fun publicOrInternalFunction() = 1
        actual internal fun internalFunction() = 1
        internal fun internalOrPrivateFunction() = 1
        private fun privateFunction() = 1
    }
}

actual open class Outer2 actual constructor() {
    actual public open val publicProperty = 1
    public open val publicOrInternalProperty = 1
    actual internal open val internalProperty = 1
    internal open val internalOrPrivateProperty = 1
    private val privateProperty = 1

    actual public open fun publicFunction() = 1
    public open fun publicOrInternalFunction() = 1
    actual internal open fun internalFunction() = 1
    internal open fun internalOrPrivateFunction() = 1
    private fun privateFunction() = 1

    actual open class Inner2 actual constructor() {
        actual public open val publicProperty = 1
        public open val publicOrInternalProperty = 1
        actual internal open val internalProperty = 1
        internal open val internalOrPrivateProperty = 1
        private val privateProperty = 1

        actual public open fun publicFunction() = 1
        public open fun publicOrInternalFunction() = 1
        actual internal open fun internalFunction() = 1
        internal open fun internalOrPrivateFunction() = 1
        private fun privateFunction() = 1
    }
}
