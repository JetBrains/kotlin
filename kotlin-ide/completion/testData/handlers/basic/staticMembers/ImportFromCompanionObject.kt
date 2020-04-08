import kotlin.text.Regex.Companion.fromLiteral

fun foo() {
    escape<caret>
}

// INVOCATION_COUNT: 1
// ELEMENT_TEXT: "Regex.escapeReplacement"
