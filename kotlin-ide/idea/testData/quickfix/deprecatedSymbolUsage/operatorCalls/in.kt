// "Replace usages of 'contains(String) on C: Boolean' in whole project" "true"

class C

@Deprecated("", ReplaceWith("checkContains(s)"))
operator fun C.contains(s: String) = true

fun C.checkContains(s: String) = true

fun f(c: C) {
    if ("" <caret>!in c) {

    }
}

fun g(c: C) {
    if ("" in c) {

    }
}
