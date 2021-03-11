// "class org.jetbrains.kotlin.idea.quickfix.AddFunctionParametersFix" "false"
// ERROR: Too many arguments for public open fun equals(other: Any?): Boolean defined in java.lang.Object

fun f(d: java.lang.Object) {
    d.equals("a", <caret>"b")
}
