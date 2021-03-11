// INTENTION_TEXT: "Import members from 'java.util.regex.Pattern'"
// WITH_RUNTIME
// ERROR: Unresolved reference: unresolved

import java.util.regex.Pattern

fun foo() {
    Pattern.matches("", "")

    val field = <caret>Pattern.CASE_INSENSITIVE

    Pattern.compile("")

    val fieldFqn = java.util.regex.Pattern.CASE_INSENSITIVE

    Pattern.unresolved
}
