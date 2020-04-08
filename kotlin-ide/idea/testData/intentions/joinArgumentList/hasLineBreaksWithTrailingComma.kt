// INTENTION_TEXT: "Put arguments on one line"
// REGISTRY: kotlin.formatter.allowTrailingCommaOnCallSite true

fun foo(a: Int = 1, b: Int = 1, c: Int = 1) {}

fun bar(a: Int, b: Int, c: Int) {
    foo(
        a,
        b,<caret>
        c,
    )
}