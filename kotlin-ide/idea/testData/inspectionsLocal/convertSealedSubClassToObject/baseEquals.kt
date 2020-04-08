// PROBLEM: none

abstract class Base {
    open val prop: Int
        get() = 13

    override fun equals(other: Any?): Boolean {
        if (other !is Base) return false
        return prop == other.prop
    }
}

sealed class SC : Base() {
    <caret>class U : SC()

    override val prop: Int
        get() = 42
}