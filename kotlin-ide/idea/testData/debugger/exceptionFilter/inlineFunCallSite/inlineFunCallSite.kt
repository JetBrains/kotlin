fun box() {
    foo()
}

inline fun foo() {
    null!!
}

// NAVIGATE_TO_CALL_SITE
// FILE: inlineFunCallSite.kt
// LINE: 2
