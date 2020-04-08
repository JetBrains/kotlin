// "class org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix" "false"

@Deprecated("", ReplaceWith("="))
fun oldFun() {
}

fun foo() {
    <caret>oldFun()
}
