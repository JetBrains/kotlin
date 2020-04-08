// INTENTION_TEXT: "Add import for 'kotlin.text.Regex.Companion.escape'"
// WITH_RUNTIME

import kotlin.text.Regex

fun foo() {
    Regex.<caret>escape("")
    Regex.Companion.escape("")
}