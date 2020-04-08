fun box() {
    bar {
        foo()
    }
}

inline fun foo() {
    null!!
}

inline fun bar(x: () -> Unit) {
    x()
}

// NAVIGATE_TO_CALL_SITE
// FILE: inlineFunCallSiteInInlineLambda.kt
// LINE: 3
