/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

@Suppress("DEPRECATION_ERROR")
@PublishedApi
@SinceKotlin("2.3")
@UsedFromCompilerGeneratedCode
internal fun throwUninitializedPropertyAccessException(name: String): Nothing =
    throw UninitializedPropertyAccessException("lateinit property $name has not been initialized")

@PublishedApi
@SinceKotlin("2.3")
@UsedFromCompilerGeneratedCode
internal fun throwUnsupportedOperationException(message: String): Nothing =
    throw UnsupportedOperationException(message)

@UsedFromCompilerGeneratedCode
internal fun staticInitializationFailure(reason: Throwable?, className: String?): Nothing {
    when (reason) {
        is Error -> throw reason
        null -> {
            // TODO(KT-57134): align exact exception hierarchy with jvm
            // in JVM it's NoClassDefFound if reason is null, i.e. this is already failed class
            val message = className?.let { "Could not initialize class $it" } ?: "There was an error during file or class initialization"
            throw ExceptionInInitializerError(message)
        }
        else -> {
            throw ExceptionInInitializerError(reason)
        }
    }
}
