package test

val constFlagAddedVal = ""
const val constFlagRemovedVal = ""
const val constFlagUnchangedVal = ""

fun externalFlagAddedFun() {}
external fun externalFlagRemovedFun()
external fun externalFlagUnchangedFun()

fun infixFlagAddedFun() {}
infix fun infixFlagRemovedFun() {}
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
operator fun operatorFlagRemovedFun() {}
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
