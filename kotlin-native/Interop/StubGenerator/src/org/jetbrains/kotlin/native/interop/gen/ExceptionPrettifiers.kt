/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.native.interop.gen

import org.jetbrains.kotlin.konan.util.CInteropHints

class CInteropPrettyException(message: String) : Exception(message)

/**
 * Pattern matching machinery for making exceptions from
 * cinterop nicer.
 */
sealed interface ExceptionPrettifier {
    fun matches(throwable: Throwable): Boolean

    fun prettify(throwable: Throwable): CInteropPrettyException
}

/**
 * Suggests to add `-compiler-option -fmodules` if the exception message hints about it.
 */
object ClangModulesDisabledPrettifier : ExceptionPrettifier {

    private val supportedPatterns = listOf(
            "use of '@import' when modules are disabled"
    )

    override fun matches(throwable: Throwable): Boolean {
        return supportedPatterns.any { throwable.message?.contains(it) == true }
    }

    override fun prettify(throwable: Throwable): CInteropPrettyException {
        return CInteropPrettyException(CInteropHints.fmodulesHint)
    }
}

/**
 * Wraps invocation of [action] into exception handler and makes messages of supported exceptions more user-friendly.
 * Can be optionally [disabled] which is useful when one want to find the root cause of the prettified exception.
 */
inline fun <T> withExceptionPrettifier(disabled: Boolean = false, action: () -> T): T {
    if (disabled) {
        return action()
    }
    return try {
        action()
    } catch (throwable: Throwable) {
        val prettifiers = listOf(
                ClangModulesDisabledPrettifier,
        )
        throw prettifiers.firstOrNull { it.matches(throwable) }?.prettify(throwable) ?: throwable
    }
}