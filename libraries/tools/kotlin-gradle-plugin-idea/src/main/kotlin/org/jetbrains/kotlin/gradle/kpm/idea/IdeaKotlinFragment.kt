/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.tooling.core.HasExtras
import java.io.Serializable

sealed interface IdeaKotlinFragment : Serializable, HasExtras {
    val coordinates: IdeaKotlinFragmentCoordinates
    val platforms: Set<IdeaKotlinPlatform>
    val languageSettings: IdeaKotlinLanguageSettings?
    val dependencies: List<IdeaKotlinDependency>
    val sourceDirectories: List<IdeaKotlinSourceDirectory>
    val resourceDirectories: List<IdeaKotlinResourceDirectory>
    override val extras: IdeaKotlinExtras
}

val IdeaKotlinFragment.name get() = coordinates.fragmentName

@InternalKotlinGradlePluginApi
data class IdeaKotlinFragmentImpl(
    override val coordinates: IdeaKotlinFragmentCoordinates,
    override val platforms: Set<IdeaKotlinPlatform>,
    override val languageSettings: IdeaKotlinLanguageSettings?,
    override val dependencies: List<IdeaKotlinDependency>,
    override val sourceDirectories: List<IdeaKotlinSourceDirectory>,
    override val resourceDirectories: List<IdeaKotlinResourceDirectory>,
    override val extras: IdeaKotlinExtras
) : IdeaKotlinFragment {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}

