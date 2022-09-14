/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import java.io.Serializable

sealed interface IdeaKpmVariant : IdeaKpmFragment, Serializable {
    val platform: IdeaKpmPlatform
    val variantAttributes: Map<String, String>
    val compilationOutputs: IdeaKpmCompilationOutput
}

@InternalKotlinGradlePluginApi
data class IdeaKpmVariantImpl(
    internal val fragment: IdeaKpmFragment,
    override val platform: IdeaKpmPlatform,
    override val variantAttributes: Map<String, String>,
    override val compilationOutputs: IdeaKpmCompilationOutput,
) : IdeaKpmVariant, IdeaKpmFragment by fragment {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
