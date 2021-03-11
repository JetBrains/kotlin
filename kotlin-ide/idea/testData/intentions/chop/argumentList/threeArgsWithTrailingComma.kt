// SET_TRUE: ALLOW_TRAILING_COMMA
// REGISTRY: kotlin.formatter.allowTrailingCommaOnCallSite true

fun f() {
    foo(<caret>1, "a", 2)
}

fun foo(p1: Int, p2: String, p3: Int){}