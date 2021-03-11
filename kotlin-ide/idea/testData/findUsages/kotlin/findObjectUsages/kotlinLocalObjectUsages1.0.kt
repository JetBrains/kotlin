// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtObjectDeclaration
// OPTIONS: usages
fun foo(): Any {
    object <caret>Bar

    return Bar
}

val x = Bar

// DISABLE-ERRORS