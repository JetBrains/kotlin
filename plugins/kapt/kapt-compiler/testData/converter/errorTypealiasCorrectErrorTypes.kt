package test

// CORRECT_ERROR_TYPES

open class Base<T>

typealias GA = GeneratedClass

class ErrAlias : Base<GA>()

typealias H<T> = Two<String, T>
class Alias : H<Gen>() {
    fun f(): H<Gen> = TODO()
}
