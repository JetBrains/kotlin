fun box() {
    foo()
}

inline fun foo() {
    val unused = 1
    null!!
}

// !LANGUAGE: +CorrectSourceMappingSyntax
// MAIN_CLASS: InlineFunCallSiteNewSmapSyntaxKt
// NAVIGATE_TO_CALL_SITE
// FILE: inlineFunCallSiteNewSmapSyntax.kt
// LINE: 2
