// PROBLEM: Accessing non-final property x in constructor
// FIX: none
enum class Foo : Bar {
    ONE {
        override val x = 1
    };

    val double = x<caret>
}

interface Bar {
    val x: Int
}