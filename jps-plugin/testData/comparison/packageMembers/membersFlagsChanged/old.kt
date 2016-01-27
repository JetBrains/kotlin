package test

val constFlagAddedVal = ""
const val constFlagRemovedVal = ""
const val constFlagUnchangedVal = ""

fun externalFlagAddedFun() {}
external fun externalFlagRemovedFun()
external fun externalFlagUnchangedFun()

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
