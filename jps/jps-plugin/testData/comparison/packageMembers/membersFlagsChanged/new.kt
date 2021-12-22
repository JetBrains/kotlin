package test

const val constFlagAddedVal = ""
val constFlagRemovedVal = ""
const val constFlagUnchangedVal = ""

external fun externalFlagAddedFun()
fun externalFlagRemovedFun() {}
external fun externalFlagUnchangedFun()

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


