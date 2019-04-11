/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin

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