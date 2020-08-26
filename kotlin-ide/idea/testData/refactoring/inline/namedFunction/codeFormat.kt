class FlowContextTest {
    fun <T> flow(block: suspend Int.() -> Unit) {}
    fun withScope(block: suspend () -> Unit) {}

    fun reproducer() = withScope {
        inlineMe()
    }

    private fun inli<caret>neMe() {
        flow<Int> {
            println(this)
        }
    }

}