package foo

class Curry<T, R>(private val f: FN2, private val arg1: T) {
    typealias FN2 = (T, T) -> R

    operator fun invoke(arg2: T): R =
            f(arg1, arg2)
}
