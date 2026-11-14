package library

open class Base {
    open fun value(): Int = 1
}

fun value(base: Base) = base.value()
