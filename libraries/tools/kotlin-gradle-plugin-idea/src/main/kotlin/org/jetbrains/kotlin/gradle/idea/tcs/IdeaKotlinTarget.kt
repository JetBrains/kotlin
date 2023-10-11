/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.tcs

import org.jetbrains.kotlin.tooling.core.HasMutableExtras

sealed interface IdeaKotlinTarget : HasMutableExtras {
    val targetName: String
    val compilerType: IdeaKotlinCompilerType
    val compilations: List<IdeaKotlinCompilation>
}
