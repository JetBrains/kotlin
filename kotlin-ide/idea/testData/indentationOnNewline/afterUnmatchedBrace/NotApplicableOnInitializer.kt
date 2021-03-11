// WITH_RUNTIME
// WITHOUT_CUSTOM_LINE_INDENT_PROVIDER
fun test(): Int = bar { f<caret>oo()

fun foo() = 42

fun bar(f: () -> Int) = f()
