package java.util

typealias ArrayList<T> = kotlin.collections.ArrayList<T>

class Optional<T>(private val value: T?) {
    companion object {
        fun <T> of(value: T) = Optional(value)
    }
}