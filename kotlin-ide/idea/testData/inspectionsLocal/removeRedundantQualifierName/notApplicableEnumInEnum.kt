// PROBLEM: none
// WITH_RUNTIME
enum class A

enum class B() {
    ;

    fun test() {
        <caret>A.values()
    }
}