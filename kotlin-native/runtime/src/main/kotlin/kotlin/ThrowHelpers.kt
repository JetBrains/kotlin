/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package kotlin

@PublishedApi
internal fun throwUninitializedPropertyAccessException(propertyName: String): Nothing {
    throw UninitializedPropertyAccessException("lateinit property $propertyName has not been initialized")
}

@PublishedApi
internal fun throwUnsupportedOperationException(message: String): Nothing {
    throw UnsupportedOperationException(message)
}

