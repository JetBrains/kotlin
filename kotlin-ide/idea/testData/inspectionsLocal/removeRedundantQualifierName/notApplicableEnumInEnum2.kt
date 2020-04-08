// PROBLEM: none
// WITH_RUNTIME
enum class A

enum class B(val x: Int) {
    BB(<caret>A.values().size)
}