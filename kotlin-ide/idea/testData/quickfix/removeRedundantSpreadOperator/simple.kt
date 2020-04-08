// "Remove redundant *" "true"
// LANGUAGE_VERSION: 1.4

fun takeVararg(vararg s: String) {}

fun test(strings: Array<String>) {
    takeVararg(s = *<caret>strings)
}