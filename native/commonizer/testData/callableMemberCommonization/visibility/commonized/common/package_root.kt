expect public val publicProperty: Int
expect internal val publicOrInternalProperty: Int
expect internal val internalProperty: Int

expect public fun publicFunction(): Int
expect internal fun publicOrInternalFunction(): Int
expect internal fun internalFunction(): Int

expect open class Outer1() {
    public val publicProperty: Int
    internal val publicOrInternalProperty: Int
    internal val internalProperty: Int

    public fun publicFunction(): Int
    internal fun publicOrInternalFunction(): Int
    internal fun internalFunction(): Int

    open class Inner1() {
        public val publicProperty: Int
        internal val publicOrInternalProperty: Int
        internal val internalProperty: Int

        public fun publicFunction(): Int
        internal fun publicOrInternalFunction(): Int
        internal fun internalFunction(): Int
    }
}

expect open class Outer2() {
    public open val publicProperty: Int
    internal open val internalProperty: Int

    public open fun publicFunction(): Int
    internal open fun internalFunction(): Int

    open class Inner2() {
        public open val publicProperty: Int
        internal open val internalProperty: Int

        public open fun publicFunction(): Int
        internal open fun internalFunction(): Int
    }
}
