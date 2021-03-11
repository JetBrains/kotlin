package test

@Deprecated("hidden", level = DeprecationLevel.HIDDEN)
fun hiddenFun(){}

fun notHiddenFun(){}

@Deprecated("hidden", level = DeprecationLevel.HIDDEN)
var hiddenProperty: Int = 1

var notHiddenProperty: Int = 1

@Deprecated("hidden", level = DeprecationLevel.HIDDEN)
val String.hiddenExtension: Int get() = 1

// ALLOW_AST_ACCESS