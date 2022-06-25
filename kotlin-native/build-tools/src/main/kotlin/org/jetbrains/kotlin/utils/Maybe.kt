/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.utils

/**
 * Wrapper over [T]?.
 *
 * Useful for some generalized functions that you want to pass [T]? into, but they forbid nullable types.
 * For example, [ObjectFactory.newInstance][org.gradle.api.model.ObjectFactory.newInstance] cannot be used
 * to construct a type that has [T]? as a constructor argument.
 *
 * @property orNull get underlying value.
 */
data class Maybe<T>(val orNull: T?)

/**
 * Construct [Maybe]<[T]> from [T]?.
 */
inline val <T> T?.asMaybe
    get() = Maybe(this)
