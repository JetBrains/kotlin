// FILE: utils.kt
/**
 * NOTE: everything here is inline.
 */
@file:Suppress("NOTHING_TO_INLINE")

package test

inline fun twice(x: Int): Int = x * 2

// FILE: plain.kt
/**
 * KDoc of the first declaration.
 */
package test

fun plain(): Int = 0
