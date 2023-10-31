/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.viper.ast.Info

sealed interface SourceRole {
    data object ReturnsEffect : SourceRole
    data object ReturnsTrueEffect : SourceRole
    data object ReturnsFalseEffect : SourceRole
    data object ReturnsNullEffect : SourceRole
    data object ReturnsNotNullEffect : SourceRole
}

val SourceRole?.asInfo: Info
    get() = when (this) {
        null -> Info.NoInfo
        else -> Info.Wrapped(this)
    }