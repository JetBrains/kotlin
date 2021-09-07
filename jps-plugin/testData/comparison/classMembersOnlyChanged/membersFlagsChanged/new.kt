package test

abstract class A {
    abstract val abstractFlagAddedVal: String
    val abstractFlagRemovedVal: String = ""
    abstract val abstractFlagUnchangedVal: String
    abstract fun abstractFlagAddedFun()
    fun abstractFlagRemovedFun() {}
    abstract fun abstractFlagUnchangedFun()

    final val finalFlagAddedVal = ""
    val finalFlagRemovedVal = ""
    final val finalFlagUnchangedVal = ""
    final fun finalFlagAddedFun() {}
    fun finalFlagRemovedFun() {}
    final fun finalFlagUnchangedFun() {}

    @Suppress("INAPPLICABLE_INFIX_MODIFIER")
    infix fun infixFlagAddedFun() {}
    fun infixFlagRemovedFun() {}
    @Suppress("INAPPLICABLE_INFIX_MODIFIER")
    infix fun infixFlagUnchangedFun() {}

    inline fun inlineFlagAddedFun() {}
    fun inlineFlagRemovedFun() {}
    inline fun inlineFlagUnchangedFun() {}

    internal val internalFlagAddedVal = ""
    val internalFlagRemovedVal = ""
    internal val internalFlagUnchangedVal = ""
    internal fun internalFlagAddedFun() {}
    fun internalFlagRemovedFun() {}
    internal fun internalFlagUnchangedFun() {}

    lateinit var lateinitFlagAddedVal: String
    var lateinitFlagRemovedVal: String = ""
    lateinit var lateinitFlagUnchangedVal: String

    open val openFlagAddedVal = ""
    val openFlagRemovedVal = ""
    open val openFlagUnchangedVal = ""
    open fun openFlagAddedFun() {}
    fun openFlagRemovedFun() {}
    open fun openFlagUnchangedFun() {}

    @Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
    operator fun operatorFlagAddedFun() {}
    fun operatorFlagRemovedFun() {}
    @Suppress("INAPPLICABLE_OPERATOR_MODIFIER")
    operator fun operatorFlagUnchangedFun() {}

    private val privateFlagAddedVal = ""
    val privateFlagRemovedVal = ""
    private val privateFlagUnchangedVal = ""
    private fun privateFlagAddedFun() {}
    fun privateFlagRemovedFun() {}
    private fun privateFlagUnchangedFun() {}

    protected val protectedFlagAddedVal = ""
    val protectedFlagRemovedVal = ""
    protected val protectedFlagUnchangedVal = ""
    protected fun protectedFlagAddedFun() {}
    fun protectedFlagRemovedFun() {}
    protected fun protectedFlagUnchangedFun() {}

    public val publicFlagAddedVal = ""
    val publicFlagRemovedVal = ""
    public val publicFlagUnchangedVal = ""
    public fun publicFlagAddedFun() {}
    fun publicFlagRemovedFun() {}
    public fun publicFlagUnchangedFun() {}

    tailrec fun tailrecFlagAddedFun() {}
    fun tailrecFlagRemovedFun() {}
    tailrec fun tailrecFlagUnchangedFun() {}

    val noFlagsUnchangedVal = ""
    fun noFlagsUnchangedFun() {}
}

object O {
    const val constFlagAddedVal = ""
    val constFlagRemovedVal = ""
    const val constFlagUnchangedVal = ""
}