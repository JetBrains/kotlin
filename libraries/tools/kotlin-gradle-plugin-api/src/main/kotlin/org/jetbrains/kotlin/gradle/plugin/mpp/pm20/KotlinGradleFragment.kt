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

interface KotlinGradleFragment : KotlinModuleFragment, HasKotlinDependencies, Named {
    override val kotlinSourceRoots: SourceDirectorySet

    override val containingModule: KotlinGradleModule

    override fun getName(): String = fragmentName

    override val languageSettings: LanguageSettingsBuilder

    val project: Project
        get() = containingModule.project

    fun refines(other: KotlinGradleFragment)

    fun refines(other: NamedDomainObjectProvider<KotlinGradleFragment>)

    override fun dependencies(configureClosure: Closure<Any?>) =
        dependencies f@{ project.configure(this@f, configureClosure) }

    companion object {
        const val COMMON_FRAGMENT_NAME = "common"
    }

    /** This configuration includes the dependencies from the refines-parents */
    val transitiveApiConfigurationName: String

    /** This configuration includes the dependencies from the refines-parents */
    val transitiveImplementationConfigurationName: String

    override val relatedConfigurationNames: List<String>
        get() = super.relatedConfigurationNames +
                // TODO: resolvable metadata configurations?
                listOf(transitiveApiConfigurationName, transitiveImplementationConfigurationName)
}

val KotlinGradleFragment.refinesClosure: Set<KotlinGradleFragment>
    get() = (this as KotlinModuleFragment).refinesClosure.mapTo(mutableSetOf()) { it as KotlinGradleFragment }
