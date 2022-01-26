/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

private typealias IsMarkedNullable = Boolean

internal interface TypeNullabilityCommonizer : AssociativeCommonizer<IsMarkedNullable>

internal fun TypeNullabilityCommonizer(options: TypeCommonizer.Context): TypeNullabilityCommonizer {
    return if (options.enableCovariantNullabilityCommonization) CovariantTypeNullabilityCommonizer
    else EqualTypeNullabilityCommonizer
}

private object CovariantTypeNullabilityCommonizer : TypeNullabilityCommonizer {
    override fun commonize(first: IsMarkedNullable, second: IsMarkedNullable): IsMarkedNullable {
        return first || second
    }
}

private object EqualTypeNullabilityCommonizer : TypeNullabilityCommonizer {
    override fun commonize(first: IsMarkedNullable, second: IsMarkedNullable): IsMarkedNullable? {
        if (first != second) return null
        return first
    }
}