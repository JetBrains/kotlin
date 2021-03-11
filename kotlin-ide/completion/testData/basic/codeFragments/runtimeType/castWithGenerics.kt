fun main(args: Array<String>) {
    val b: Base<String> = Derived()
    <caret>val a = 1
}

open class Base<T> {
    fun funInBase(t: T): T { return t }

    open fun funWithOverride(t: T): T { return t }
    open fun funWithoutOverride(t: T): T { return t }
}

class Derived: Base<String>() {
    override fun funWithOverride(t: String): String { return "a" }
}

// INVOCATION_COUNT: 1
// EXIST: funInBase, funWithOverride, funWithoutOverride
// NOTHING_ELSE

// RUNTIME_TYPE: Derived
