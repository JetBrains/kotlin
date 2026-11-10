// To test IC more exhaustively, always allow impossible `is`
@file:Suppress("IMPOSSIBLE_IS_CHECK_ERROR")
package test

fun isBaseClass(c: Class) = c is BaseClass
