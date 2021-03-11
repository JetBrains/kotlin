// OUT_OF_CODE_BLOCK: FALSE
// TYPE: '4'

fun boo() {
    class InnerBoo() {
        val someValue: Int

        init {
            someValue = <caret>
        }
    }

    val b = InnerBoo()
}
