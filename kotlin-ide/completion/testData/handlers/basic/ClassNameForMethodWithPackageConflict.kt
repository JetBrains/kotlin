// WITH_RUNTIME

fun buildTemplates() {
    val kotlin = 42
    printl<caret>
}

// ELEMENT: println
// TAIL_TEXT: "() (kotlin.io)"