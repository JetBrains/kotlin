fun main(args: Array<String>) { // Error

    data class TestUsed(val parameter: CharSequence) {
        private data class Used(val parameter: Any) {
            companion object {
                fun Any.doStuff1() = Used(this)
            }
        }
    }
}