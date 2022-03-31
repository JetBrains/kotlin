/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

internal fun THROW_CCE(): Nothing {
    throw ClassCastException()
}

internal fun THROW_NPE(): Nothing {
    throw NullPointerException()
}

internal fun THROW_ISE(): Nothing {
    throw IllegalStateException()
}

internal fun THROW_IAE(message: String): Nothing {
    throw IllegalArgumentException(message)
}

internal fun throwNoBranchMatchedException(): Nothing {
    throw NoWhenBranchMatchedException()
}

internal fun rangeCheck(index: Int, size: Int) {
  if (index < 0 || index >= size) throw IndexOutOfBoundsException()
}

@PublishedApi
internal fun throwUninitializedPropertyAccessException(name: String): Nothing {
    throw UninitializedPropertyAccessException("lateinit property $name has not been initialized")
}