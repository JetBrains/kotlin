// PROBLEM: none
// FIX: none
enum class Fo(val b: Int) {
    ONE(5) {
        override val x = b
    };

    abstract val x: Int

    val dos = <caret>toString()
}