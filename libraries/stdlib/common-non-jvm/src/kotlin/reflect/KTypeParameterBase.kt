/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.reflect

internal abstract class KTypeParameterBase : KTypeParameter {
    protected abstract val containerFqName: String

    final override fun toString(): String = when (variance) {
        KVariance.INVARIANT -> ""
        KVariance.IN -> "in "
        KVariance.OUT -> "out "
    } + name

    final override fun equals(other: Any?) =
        other is KTypeParameterBase && name == other.name && containerFqName == other.containerFqName

    final override fun hashCode() = containerFqName.hashCode() * 31 + name.hashCode()
}
