/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

public inline fun <reified T> arrayOfNulls(size: Int): Array<T?> = Array<T?>(size)

public inline fun <T> arrayOf(vararg a: T): Array<T> = a.unsafeCast<Array<T>>()

public inline fun booleanArrayOf(vararg a: Boolean) = a

public inline fun byteArrayOf(vararg a: Byte) = a

public inline fun shortArrayOf(vararg a: Short) = a

public inline fun charArrayOf(vararg a: Char) = a

public inline fun intArrayOf(vararg a: Int) = a

public inline fun floatArrayOf(vararg a: Float) = a

public inline fun doubleArrayOf(vararg a: Double) = a

public inline fun longArrayOf(vararg a: Long) = a


@PublishedApi
internal fun throwUninitializedPropertyAccessException(name: String): Nothing =
    throw UninitializedPropertyAccessException("lateinit property $name has not been initialized")

internal fun noWhenBranchMatchedException(): Nothing = throw NoWhenBranchMatchedException()


fun THROW_ISE() {
    throw IllegalStateException()
}
fun THROW_CCE() {
    throw ClassCastException()
}
fun THROW_NPE() {
    throw NullPointerException()
}