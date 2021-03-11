// IS_APPLICABLE: false
// COMPILER_ARGUMENTS: -XXLanguage:-FunctionReferenceWithDefaultValueAsOtherType

fun foo(z: Int, y: Int = 0) = y + z

val x = { arg: Int <caret>-> foo(arg) }