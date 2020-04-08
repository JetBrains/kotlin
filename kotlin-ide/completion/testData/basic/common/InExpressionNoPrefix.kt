class Test {
    fun test() {
        val some : <caret>
    }
}

// EXIST: Any, Nothing, Unit, Int, Number, Array
// EXIST_JAVA_ONLY: Thread