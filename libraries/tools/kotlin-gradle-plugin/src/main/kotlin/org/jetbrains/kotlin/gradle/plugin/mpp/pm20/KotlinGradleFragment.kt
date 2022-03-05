/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Provider
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.kpm.KotlinExternalModelContainer
import org.jetbrains.kotlin.gradle.kpm.KotlinMutableExternalModelContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.LanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.mpp.DefaultKotlinDependencyHandler
import org.jetbrains.kotlin.gradle.plugin.mpp.toModuleDependency
import org.jetbrains.kotlin.gradle.plugin.sources.DefaultLanguageSettingsBuilder
import org.jetbrains.kotlin.gradle.plugin.sources.FragmentConsistencyChecker
import org.jetbrains.kotlin.gradle.plugin.sources.FragmentConsistencyChecks
import org.jetbrains.kotlin.gradle.utils.addExtendsFromRelation
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheckWhenEvaluated
import org.jetbrains.kotlin.project.model.KotlinModuleDependency
import org.jetbrains.kotlin.project.model.KotlinModuleFragment
import org.jetbrains.kotlin.project.model.withRefinesClosure
import javax.inject.Inject

open class KotlinGradleFragmentInternal @Inject constructor(
    final override val containingModule: KotlinGradleModule,
    final override val fragmentName: String,
    dependencyConfigurations: KotlinFragmentDependencyConfigurations
) : KotlinGradleFragment,
    KotlinFragmentDependencyConfigurations by dependencyConfigurations {

    final override fun getName(): String = fragmentName

    final override val project: Project // overriding with final to avoid warnings
        get() = super.project

    // TODO pull up to KotlinModuleFragment
    // FIXME apply to compilation
    // FIXME check for consistency
    override val languageSettings: LanguageSettingsBuilder = DefaultLanguageSettingsBuilder()

    internal val external: KotlinMutableExternalModelContainer = KotlinExternalModelContainer.mutable()

    override fun refines(other: KotlinGradleFragment) {
        checkCanRefine(other)
        refines(containingModule.fragments.named(other.name))
    }

    override fun refines(other: NamedDomainObjectProvider<KotlinGradleFragment>) {
        _directRefinesDependencies.add(other)
        other.configure { checkCanRefine(it) }

        project.addExtendsFromRelation(
            this.transitiveApiConfiguration.name, other.get().transitiveApiConfiguration.name
        )

        project.addExtendsFromRelation(
            this.transitiveImplementationConfiguration.name, other.get().transitiveImplementationConfiguration.name
        )

        project.addExtendsFromRelation(
            this.transitiveRuntimeOnlyConfiguration.name, other.get().transitiveRuntimeOnlyConfiguration.name
        )

        project.runProjectConfigurationHealthCheckWhenEvaluated {
            kotlinGradleFragmentConsistencyChecker.runAllChecks(this@KotlinGradleFragmentInternal, other.get())
        }
    }

    private fun checkCanRefine(other: KotlinGradleFragment) {
        check(containingModule == other.containingModule) {
            "Fragments can only refine each other within one module. Can't make $this refine $other"
        }
    }

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        DefaultKotlinDependencyHandler(this, project).run(configure)

    override fun dependencies(configureClosure: Closure<Any?>) =
        dependencies f@{ ConfigureUtil.configure(configureClosure, this@f) }

    private val _directRefinesDependencies = mutableSetOf<Provider<KotlinGradleFragment>>()

    override val directRefinesDependencies: Iterable<KotlinGradleFragment>
        get() = _directRefinesDependencies.map { it.get() }.toSet()

    // TODO: separate the declared module dependencies and exported module dependencies? we need this to keep implementation dependencies
    //       out of the consumer's metadata compilations compile classpath; however, Native variants must expose implementation as API
    //       anyway, so for now all fragments follow that behavior
    override val declaredModuleDependencies: Iterable<KotlinModuleDependency>
        get() = listOf(apiConfiguration, implementationConfiguration).flatMapTo(mutableSetOf()) { exportConfiguration ->
            exportConfiguration.allDependencies.map { dependency -> dependency.toModuleDependency(project) }
        }

    override val kotlinSourceRoots: SourceDirectorySet =
        project.objects.sourceDirectorySet(
            "$fragmentName.kotlin", "Kotlin sources for fragment $fragmentName"
        )

    override fun toString(): String = "fragment $fragmentName in $containingModule"

    private val kotlinGradleFragmentConsistencyChecker =
        FragmentConsistencyChecker(
            unitsName = "fragments",
            name = { name },
            checks = FragmentConsistencyChecks<KotlinGradleFragment>(
                unitName = "fragment",
                languageSettings = { languageSettings }
            ).allChecks
        )
}

val KotlinGradleFragment.refinesClosure: Set<KotlinGradleFragment>
    get() = (this as KotlinModuleFragment).withRefinesClosure.map { it as KotlinGradleFragment }.toSet()
