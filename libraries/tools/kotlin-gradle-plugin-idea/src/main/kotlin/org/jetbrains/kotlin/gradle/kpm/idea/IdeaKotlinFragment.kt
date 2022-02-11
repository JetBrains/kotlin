/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import java.io.Serializable

interface IdeaKotlinFragment : Serializable {
    val name: String
    val languageSettings: IdeaKotlinLanguageSettings?
    val dependencies: Collection<IdeaKotlinFragmentDependency>
    val directRefinesDependencies: Collection<IdeaKotlinFragment>
    val sourceDirectories: Collection<IdeaKotlinSourceDirectory>
    val resourceDirectories: Collection<IdeaKotlinResourceDirectory>
}

data class IdeaKotlinFragmentImpl @KotlinGradlePluginApi constructor(
    override val name: String,
    override val languageSettings: IdeaKotlinLanguageSettings?,
    override val dependencies: Collection<IdeaKotlinFragmentDependency>,
    override val directRefinesDependencies: Collection<IdeaKotlinFragment>,
    override val sourceDirectories: Collection<IdeaKotlinSourceDirectory>,
    override val resourceDirectories: Collection<IdeaKotlinResourceDirectory>
) : IdeaKotlinFragment


