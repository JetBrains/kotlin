// "class org.jetbrains.kotlin.idea.quickfix.replaceWith.DeprecatedSymbolUsageFix" "false"

@Deprecated("")
fun oldFun() {
}

fun foo() {
    <caret>oldFun()
}
