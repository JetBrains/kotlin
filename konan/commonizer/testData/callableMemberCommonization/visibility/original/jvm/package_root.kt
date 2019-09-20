public val publicProperty = 1
internal val publicOrInternalProperty = 1
internal val internalProperty = 1
private val internalOrPrivateProperty = 1
private val privateProperty = 1

public fun publicFunction() = 1
internal fun publicOrInternalFunction() = 1
internal fun internalFunction() = 1
private fun internalOrPrivateFunction() = 1
private fun privateFunction() = 1

open class Outer1 {
    public val publicProperty = 1
    internal val publicOrInternalProperty = 1
    internal val internalProperty = 1
    private val internalOrPrivateProperty = 1
    private val privateProperty = 1

    public fun publicFunction() = 1
    internal fun publicOrInternalFunction() = 1
    internal fun internalFunction() = 1
    private fun internalOrPrivateFunction() = 1
    private fun privateFunction() = 1

    open class Inner1 {
        public val publicProperty = 1
        internal val publicOrInternalProperty = 1
        internal val internalProperty = 1
        private val internalOrPrivateProperty = 1
        private val privateProperty = 1

        public fun publicFunction() = 1
        internal fun publicOrInternalFunction() = 1
        internal fun internalFunction() = 1
        private fun internalOrPrivateFunction() = 1
        private fun privateFunction() = 1
    }
}

open class Outer2 {
    public open val publicProperty = 1
    internal open val publicOrInternalProperty = 1
    internal open val internalProperty = 1
    private val internalOrPrivateProperty = 1
    private val privateProperty = 1

    public open fun publicFunction() = 1
    internal open fun publicOrInternalFunction() = 1
    internal open fun internalFunction() = 1
    private fun internalOrPrivateFunction() = 1
    private fun privateFunction() = 1

    open class Inner2 {
        public open val publicProperty = 1
        internal open val publicOrInternalProperty = 1
        internal open val internalProperty = 1
        private val internalOrPrivateProperty = 1
        private val privateProperty = 1

        public open fun publicFunction() = 1
        internal open fun publicOrInternalFunction() = 1
        internal open fun internalFunction() = 1
        private fun internalOrPrivateFunction() = 1
        private fun privateFunction() = 1
    }
}
