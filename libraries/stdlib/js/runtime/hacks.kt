/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

import kotlin.internal.UsedFromCompilerGeneratedCode

@UsedFromCompilerGeneratedCode
internal fun throwKotlinNothingValueException(): Nothing =
    throw KotlinNothingValueException()

@UsedFromCompilerGeneratedCode
internal fun noWhenBranchMatchedException(): Nothing = throw NoWhenBranchMatchedException()

@UsedFromCompilerGeneratedCode
internal fun THROW_ISE(): Nothing {
    throw IllegalStateException()
}

@UsedFromCompilerGeneratedCode
internal fun THROW_CCE(): Nothing {
    throw ClassCastException()
}

@UsedFromCompilerGeneratedCode
internal fun THROW_NPE(): Nothing {
    throw NullPointerException()
}

@UsedFromCompilerGeneratedCode
internal fun THROW_IAE(msg: String): Nothing {
    throw IllegalArgumentException(msg)
}

@UsedFromCompilerGeneratedCode
internal fun <T:Any> ensureNotNull(v: T?): T = if (v == null) THROW_NPE() else v
