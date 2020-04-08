// PROBLEM: none

interface First {
    fun foo() = 2
}
interface Second {
    fun foo() = 3
}

class Diamond : First, Second {
    override <caret>fun foo(): Int {
        return super<First>.foo()
    }
}
