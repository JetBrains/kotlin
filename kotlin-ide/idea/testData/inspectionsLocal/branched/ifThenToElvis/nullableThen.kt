// PROBLEM: none

fun foo(p: String?): String? {
    return <caret>if (p != null) p.bar() else "a"
}

fun String.bar(): String? = null