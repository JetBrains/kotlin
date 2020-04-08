// "Create enum constant 'D'" "true"
enum class Test {
    A,
    B,
    C;

    fun test() {
    }
}

fun main() {
    Test.D<caret>
}