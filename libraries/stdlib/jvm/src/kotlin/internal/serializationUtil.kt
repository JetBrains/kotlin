/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

import java.io.InvalidObjectException

@InlineOnly
internal actual inline fun throwReadObjectNotSupported(): Nothing {
    throw InvalidObjectException("Deserialization is supported via proxy only")
}
@InlineOnly
internal actual inline fun wrapAsDeserializationException(action: () -> Unit) {
    try {
        action()
    } catch (e: Throwable) {
        throw InvalidObjectException(e.message).initCause(e)
    }
}

internal actual typealias ReadObjectParameterType = java.io.ObjectInputStream
