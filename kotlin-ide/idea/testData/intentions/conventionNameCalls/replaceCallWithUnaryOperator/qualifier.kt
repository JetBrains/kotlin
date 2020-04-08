class C {
    companion object {
        operator fun unaryMinus(): C = C()
    }
}

fun foo() {
    C.<caret>unaryMinus()
}
