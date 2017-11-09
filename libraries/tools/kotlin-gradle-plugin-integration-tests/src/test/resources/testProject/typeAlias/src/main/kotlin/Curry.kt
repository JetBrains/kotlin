package foo

typealias FN2<T, R> = (T, T) -> R

class Curry<T, R>(private val f: FN2<T, R>, private val arg1: T) {
    operator fun invoke(arg2: T): R =
            f(arg1, arg2)
}
