/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

interface IdeaKotlinVariant : IdeaKotlinFragment, Serializable {
    val variantAttributes: Map<String, String>
    val compilationOutputs: IdeaKotlinCompilationOutput
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinVariantImpl(
    internal val fragment: IdeaKotlinFragment,
    override val variantAttributes: Map<String, String>,
    override val compilationOutputs: IdeaKotlinCompilationOutput,
) : IdeaKotlinVariant, IdeaKotlinFragment by fragment
