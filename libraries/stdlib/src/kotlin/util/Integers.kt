@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StandardKt")
package kotlin

/**
 * Executes the given function [body] the number of times equal to the value of this integer.
 */
@Deprecated("Use repeat(n) { body } instead.")
public inline fun Int.times(body : () -> Unit) {
    var count = this;
    while (count > 0) {
       body()
       count--
    }
}


/**
 * Executes the given function [body] specified number of [times].
 *
 * A zero-based index of current iteration is passed as a parameter to [body].
 */
public inline fun repeat(times: Int, body: (Int) -> Unit) {
    for (index in 0..times - 1) {
        body(index)
    }
}