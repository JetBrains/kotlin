fun <X> adjust(f1: () -> X, f2: () -> X) {}
fun f1(): Any = 1
fun f<caret>a(): Any = "a" // Inline and remove.

fun callAdjust() {
    adjust(::f1, ::fa)
    adjust(::fa, ::f1)
    adjust(::f1, ::f1)
    adjust(::fa, ::fa)
    adjust({ f1() }, { fa() })
}