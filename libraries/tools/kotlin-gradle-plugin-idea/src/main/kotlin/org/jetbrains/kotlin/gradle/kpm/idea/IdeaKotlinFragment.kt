/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.KotlinExternalModelContainer
import java.io.Serializable

interface IdeaKotlinFragment : Serializable {
    val name: String
    val moduleIdentifier: IdeaKotlinModuleIdentifier
    val languageSettings: IdeaKotlinLanguageSettings?
    val dependencies: List<IdeaKotlinFragmentDependency>
    val directRefinesDependencies: List<IdeaKotlinFragment>
    val sourceDirectories: List<IdeaKotlinSourceDirectory>
    val resourceDirectories: List<IdeaKotlinResourceDirectory>
    val external: KotlinExternalModelContainer
}

fun IdeaKotlinFragment.deepCopy(interner: Interner = Interner.default()): IdeaKotlinFragment {
    return IdeaKotlinFragmentImpl(
        name = interner.intern(name),
        languageSettings = interner.intern(languageSettings?.deepCopy(interner)),
        dependencies = interner.internList(dependencies.map { it.deepCopy(interner) }),
        directRefinesDependencies = interner.internList(directRefinesDependencies.map { it.deepCopy(interner) }),
        sourceDirectories = interner.internList(sourceDirectories.map { it.deepCopy(interner) }),
        resourceDirectories = interner.internList(resourceDirectories.map { it.deepCopy(interner) }),
        external = external.copy()
    )
}

@InternalKotlinGradlePluginApi
data class IdeaKotlinFragmentImpl(
    override val name: String,
    override val moduleIdentifier: IdeaKotlinModuleIdentifier,
    override val languageSettings: IdeaKotlinLanguageSettings?,
    override val dependencies: List<IdeaKotlinFragmentDependency>,
    override val directRefinesDependencies: List<IdeaKotlinFragment>,
    override val sourceDirectories: List<IdeaKotlinSourceDirectory>,
    override val resourceDirectories: List<IdeaKotlinResourceDirectory>,
    override val external: KotlinExternalModelContainer
) : IdeaKotlinFragment {

    @InternalKotlinGradlePluginApi
    companion object {
        private const val serialVersionUID = 0L
    }
}
