// PROBLEM: none
<caret>@Deprecated("")
fun foo() {
    bar() ?: return
}

fun bar(): String? = null