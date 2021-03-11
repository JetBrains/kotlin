// PROBLEM: none
// FIX: none
enum class Foo : Bar {
    ONE {
        override fun x() = 1
    };

    val double = <caret>x(2)
}

interface Bar {
    fun x(): Int = 42
    fun x(i: Int): Int = 24
}