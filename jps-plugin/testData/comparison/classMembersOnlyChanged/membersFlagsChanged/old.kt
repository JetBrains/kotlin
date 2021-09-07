package test

abstract class A {
    val abstractFlagAddedVal: String = ""
    abstract val abstractFlagRemovedVal: String
    abstract val abstractFlagUnchangedVal: String
    fun abstractFlagAddedFun() {}
    abstract fun abstractFlagRemovedFun()
    abstract fun abstractFlagUnchangedFun()

    val finalFlagAddedVal = ""
    final val finalFlagRemovedVal = ""
    final val finalFlagUnchangedVal = ""
    fun finalFlagAddedFun() {}
    final fun finalFlagRemovedFun() {}
    final fun finalFlagUnchangedFun() {}

    fun infixFlagAddedFun() {}
    @Suppress("INAPPLICABLE_INFIX_MODIFIER")
    infix fun infixFlagRemovedFun() {}
    @Suppress("INAPPLICABLE_INFIX_MODIFIER")
    infix fun infixFlagUnchangedFun() {}

    fun inlineFlagAddedFun() {}
    inline fun inlineFlagRemovedFun() {}
    inline fun inlineFlagUnchangedFun() {}

    val internalFlagAddedVal = ""
    internal val internalFlagRemovedVal = ""
    internal val internalFlagUnchangedVal = ""
    fun internalFlagAddedFun() {}
    internal fun internalFlagRemovedFun() {}
    internal fun internalFlagUnchangedFun() {}

    var lateinitFlagAddedVal = ""
    lateinit var lateinitFlagRemovedVal: String
    lateinit var lateinitFlagUnchangedVal: String

    val openFlagAddedVal = ""
    open val openFlagRemovedVal = ""
    open val openFlagUnchangedVal = ""
    fun openFlagAddedFun() {}
    open fun openFlagRemovedFun() {}
    open fun openFlagUnchangedFun() {}

    fun operatorFlagAddedFun() {}
    @Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
    operator fun operatorFlagRemovedFun() {}
    @Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
    operator fun operatorFlagUnchangedFun() {}

    val privateFlagAddedVal = ""
    private val privateFlagRemovedVal = ""
    private val privateFlagUnchangedVal = ""
    fun privateFlagAddedFun() {}
    private fun privateFlagRemovedFun() {}
    private fun privateFlagUnchangedFun() {}

    val protectedFlagAddedVal = ""
    protected val protectedFlagRemovedVal = ""
    protected val protectedFlagUnchangedVal = ""
    fun protectedFlagAddedFun() {}
    protected fun protectedFlagRemovedFun() {}
    protected fun protectedFlagUnchangedFun() {}

    val publicFlagAddedVal = ""
    public val publicFlagRemovedVal = ""
    public val publicFlagUnchangedVal = ""
    fun publicFlagAddedFun() {}
    public fun publicFlagRemovedFun() {}
    public fun publicFlagUnchangedFun() {}

    fun tailrecFlagAddedFun() {}
    tailrec fun tailrecFlagRemovedFun() {}
    tailrec fun tailrecFlagUnchangedFun() {}

    val noFlagsUnchangedVal = ""
    fun noFlagsUnchangedFun() {}
}

object O {
    val constFlagAddedVal = ""
    const val constFlagRemovedVal = ""
    const val constFlagUnchangedVal = ""
}