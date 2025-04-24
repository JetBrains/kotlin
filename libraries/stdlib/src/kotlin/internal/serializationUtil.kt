/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.internal

/**
 * Throws a platform-specific exception from an `readObject()` function.
 */
@InlineOnly
internal expect inline fun throwReadObjectNotSupported(): Nothing

/**
 * Argument type for `readObject` function.
 *
 * It should be `ObjectInputStream` on JVM.
 */
internal expect class ReadObjectParameterType
