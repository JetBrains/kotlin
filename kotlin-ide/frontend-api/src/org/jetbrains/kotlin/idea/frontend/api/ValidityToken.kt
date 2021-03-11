/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api

abstract class ValidityToken {
    abstract fun isValid(): Boolean
    abstract fun getInvalidationReason(): String

    abstract fun isAccessible(): Boolean
    abstract fun getInaccessibilityReason(): String
}

@Suppress("NOTHING_TO_INLINE")
inline fun ValidityToken.assertIsValidAndAccessible() {
    if (!isValid()) {
        throw InvalidEntityAccessException("Access to invalid $this: ${getInvalidationReason()}")
    }
    if (!isAccessible()) {
        throw InaccessibleEntityAccessException("$this is inaccessible: ${getInaccessibilityReason()}")
    }
}

abstract class BadEntityAccessException(): IllegalStateException()

class InvalidEntityAccessException(override val message: String) : BadEntityAccessException()
class InaccessibleEntityAccessException(override val message: String): BadEntityAccessException()

