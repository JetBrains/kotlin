package kotlin

/**
 * Executes the given function [body] the number of times equal to the value of this integer.
 */
public inline fun Int.times(body : () -> Unit) {
    var count = this;
    while (count > 0) {
       body()
       count--
    }
}
