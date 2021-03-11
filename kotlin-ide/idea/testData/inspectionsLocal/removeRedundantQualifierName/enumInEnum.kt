// WITH_RUNTIME
enum class B() {
    ;

    fun test() {
        <caret>B.values()
    }
}