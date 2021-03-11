// "Change parameter 'u' type of function 'takeUInt' to 'Int'" "true"
// WITH_RUNTIME

fun takeUInt(u: UInt) = 0

val b = takeUInt(<caret>1)