abstract class Owner<T, X: List<T>> {
    fun <R: T> <caret>f(t: T, r: R): R = r
}
