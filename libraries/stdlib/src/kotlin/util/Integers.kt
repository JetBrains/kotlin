@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("StandardKt")
package kotlin

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