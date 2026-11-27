// To test IC more exhaustively, always allow impossible casts
@file:Suppress("CAST_NEVER_SUCCEEDS")
package test

fun asBaseClass(c: Class): BaseClass? = try {
    c as BaseClass
} catch (t: Throwable) {
    null
}
