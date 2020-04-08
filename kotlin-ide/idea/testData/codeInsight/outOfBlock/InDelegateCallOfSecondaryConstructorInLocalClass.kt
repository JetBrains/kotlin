// TYPE: '4'
// OUT_OF_CODE_BLOCK: FALSE

fun bar() {
    class InnerBoo(val someValue: Int) {
        constructor() : this(<caret>) {
        }
    }

    val b = InnerBoo()
}