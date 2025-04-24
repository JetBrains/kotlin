/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

@InlineOnly
internal actual inline fun throwReadObjectNotSupported(): Nothing {
    throw UnsupportedOperationException("Deserialization is supported via proxy only")
}

@InlineOnly
internal actual inline fun wrapAsDeserializationException(action: () -> Unit) = action()

internal actual typealias ReadObjectParameterType = Unit
