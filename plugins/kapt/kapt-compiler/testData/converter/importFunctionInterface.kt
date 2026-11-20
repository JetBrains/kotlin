package test

// CORRECT_ERROR_TYPES

open class Base<T>

class Fn : Base<Gen>(), () -> Unit {
    override fun invoke() {}
}
