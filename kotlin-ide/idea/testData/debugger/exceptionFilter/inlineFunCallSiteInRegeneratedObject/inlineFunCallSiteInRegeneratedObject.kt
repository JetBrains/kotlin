fun box() {
    foo {
        barNonThrowing()
    }
}

// MAIN_CLASS: InlineFunCallSiteInRegeneratedObjectKt
// NAVIGATE_TO_CALL_SITE
// FILE: inlineFun.kt
// LINE: 3
