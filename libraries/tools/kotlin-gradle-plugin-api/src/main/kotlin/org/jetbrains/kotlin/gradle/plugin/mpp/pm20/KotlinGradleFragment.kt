/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import groovy.lang.Closure
import org.gradle.api.Named
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.jetbrains.kotlin.gradle.plugin.HasKotlinDependencies
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.project.model.KotlinModuleFragment
import org.jetbrains.kotlin.project.model.refinesClosure

interface KotlinGradleFragment : KotlinModuleFragment, HasKotlinDependencies, KotlinFragmentDependencyConfigurations, Named {
    override val kotlinSourceRoots: SourceDirectorySet

    override val containingModule: KotlinGradleModule

    override fun getName(): String = fragmentName

    override val languageSettings: LanguageSettingsBuilder

    val project: Project
        get() = containingModule.project

    fun refines(other: KotlinGradleFragment)

    fun refines(other: NamedDomainObjectProvider<KotlinGradleFragment>)

    override val directRefinesDependencies: Iterable<KotlinGradleFragment>

    override fun dependencies(configureClosure: Closure<Any?>) =
        dependencies f@{ project.configure(this@f, configureClosure) }

    companion object {
        const val COMMON_FRAGMENT_NAME = "common"
    }

    override val apiConfigurationName: String
        get() = apiConfiguration.name

    override val implementationConfigurationName: String
        get() = implementationConfiguration.name

    override val compileOnlyConfigurationName: String
        get() = compileOnlyConfiguration.name

    override val runtimeOnlyConfigurationName: String
        get() = runtimeOnlyConfiguration.name

    override val relatedConfigurationNames: List<String>
        get() = super.relatedConfigurationNames +
                // TODO: resolvable metadata configurations?
                listOf(transitiveApiConfiguration.name, transitiveImplementationConfiguration.name)
}

val KotlinGradleFragment.refinesClosure: Set<KotlinGradleFragment>
    get() = (this as KotlinModuleFragment).refinesClosure.mapTo(mutableSetOf()) { it as KotlinGradleFragment }
