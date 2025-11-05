/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.jvm.internal

import kotlin.reflect.KTypeParameter

/**
 * The common base class for lite (stdlib-only) and full reflection implementation of type parameters.
 */
public abstract class KTypeParameterBase(
    internal val container: TypeParameterContainer,
) : KTypeParameter {
    override fun equals(other: Any?): Boolean =
        other is KTypeParameterBase && name == other.name && container == other.container

    override fun hashCode(): Int =
        container.hashCode() * 31 + name.hashCode()

    override fun toString(): String =
        TypeParameterReference.toString(this)
}

// ClassReference | FunctionReference | PropertyReference
internal typealias TypeParameterContainer = Any
