package kotlin

/**
 * Executes the given function [body] the number of times equal to the value of this integer.
 * The current index is passed to the function each time.
 */
public inline fun Int.times(body: (Int) -> Unit) {
    for (index in 0..this - 1) {
        body(index)
    }
}
