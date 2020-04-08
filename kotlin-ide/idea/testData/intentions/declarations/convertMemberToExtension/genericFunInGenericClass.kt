abstract class Owner<T> {
    fun <R> <caret>f(t: T, r: R): R = r
}
