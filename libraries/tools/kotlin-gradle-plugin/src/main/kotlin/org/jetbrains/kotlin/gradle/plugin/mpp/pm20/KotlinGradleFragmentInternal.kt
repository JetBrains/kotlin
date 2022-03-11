/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import groovy.lang.Closure
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.provider.Provider
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
import org.jetbrains.kotlin.project.model.refinesClosure
import java.util.LinkedHashSet
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
    override val languageSettings: LanguageSettingsBuilder = DefaultLanguageSettingsBuilder()

    internal val external: KotlinMutableExternalModelContainer = KotlinExternalModelContainer.mutable()

    private val refinesContainer by lazy { RefinesContainer(this) }

    override fun refines(other: KotlinGradleFragment) {
        refinesContainer.refines(containingModule.fragments.named(other.name))
    }

    override fun refines(other: NamedDomainObjectProvider<KotlinGradleFragment>) {
        refinesContainer.refines(other)
    }

    override val directRefinesDependencies: Iterable<KotlinGradleFragment>
        get() = refinesContainer.directRefinesDependencies

    override fun dependencies(configure: KotlinDependencyHandler.() -> Unit): Unit =
        DefaultKotlinDependencyHandler(this, project).run(configure)

    override fun dependencies(configureClosure: Closure<Any?>) =
        dependencies f@{ project.configure(this@f, configureClosure) }

    // TODO: separate the declared module dependencies and exported module dependencies? we need this to keep implementation dependencies
    //       out of the consumer's metadata compilations compile classpath; however, Native variants must expose implementation as API
    //       anyway, so for now all fragments follow that behavior
    override val declaredModuleDependencies: Iterable<KotlinModuleDependency>
        get() = FragmentDeclaredModuleDependenciesBuilder().buildDeclaredModuleDependencies(this)

    override val kotlinSourceRoots: SourceDirectorySet =
        project.objects.sourceDirectorySet(
            "$fragmentName.kotlin", "Kotlin sources for fragment $fragmentName"
        )

    override fun toString(): String = "fragment $fragmentName in $containingModule"
}

val KotlinGradleFragment.refinesClosure: Set<KotlinGradleFragment>
    get() = (this as KotlinModuleFragment).refinesClosure.map { it as KotlinGradleFragment }.toSet()

/**
 * Encapsulates the storage and the operations over the `refines` dependencies of the [owner] fragment.
 * The operations include the validity checks performed over the `refines` relationships between fragments.
 */
internal class RefinesContainer(val owner: KotlinGradleFragment) {
    private val _directRefinesDependencies = mutableSetOf<Provider<KotlinGradleFragment>>()

    val directRefinesDependencies: Iterable<KotlinGradleFragment>
        get() = _directRefinesDependencies.map { it.get() }.toSet()

    fun refines(other: KotlinGradleFragment) {
        checkCanRefine(other)
        refines(owner.containingModule.fragments.named(other.name))
    }

    fun refines(other: NamedDomainObjectProvider<KotlinGradleFragment>) {
        _directRefinesDependencies.add(other)
        other.configure { checkCanRefine(it) }
        val project = owner.project

        project.addExtendsFromRelation(
            owner.transitiveApiConfiguration.name, other.get().transitiveApiConfiguration.name
        )

        project.addExtendsFromRelation(
            owner.transitiveImplementationConfiguration.name, other.get().transitiveImplementationConfiguration.name
        )

        project.addExtendsFromRelation(
            owner.transitiveRuntimeOnlyConfiguration.name, other.get().transitiveRuntimeOnlyConfiguration.name
        )

        project.runProjectConfigurationHealthCheckWhenEvaluated {
            kotlinGradleFragmentConsistencyChecker.runAllChecks(owner, other.get())
        }
    }

    private fun checkCanRefine(other: KotlinGradleFragment) {
        check(owner.containingModule == other.containingModule) {
            "Fragments can only refine each other within one module. Can't make $this refine $other"
        }

        checkForCircularRefinesIfNewEdgeAdded(edgeFrom = owner, edgeTo = other)
    }
}

/** Builds the [KotlinModuleDependency] of a [KotlinGradleFragment], translating the Gradle-specific low-level dependencies model into
 * dependencies on modules in terms of KPM. */
internal class FragmentDeclaredModuleDependenciesBuilder {
    fun buildDeclaredModuleDependencies(fragment: KotlinGradleFragment): Iterable<KotlinModuleDependency> =
        listOf(fragment.apiConfiguration, fragment.implementationConfiguration).flatMapTo(mutableSetOf()) { configuration ->
            configuration.allDependencies.map { it.toModuleDependency(fragment.project) }
        }
}

private val kotlinGradleFragmentConsistencyChecker =
    FragmentConsistencyChecker(
        unitsName = "fragments",
        name = { name },
        checks = FragmentConsistencyChecks<KotlinGradleFragment>(
            unitName = "fragment",
            languageSettings = { languageSettings }
        ).allChecks
    )

private fun checkForCircularRefinesIfNewEdgeAdded(edgeFrom: KotlinModuleFragment, edgeTo: KotlinModuleFragment) {
    // If adding an edge creates a cycle, then the fragment of the edge belongs to the cycle, so run DFS from that node
    // to check whether it became reachable from itself
    val visited = hashSetOf<KotlinModuleFragment>()
    val stack = LinkedHashSet<KotlinModuleFragment>() // Store the stack explicitly to pretty-print the cycle

    stack += edgeFrom

    fun checkEdgeFromReachableRecursively(from: KotlinModuleFragment) {
        stack += from
        visited += from

        val outgoingRefinesEdges = from.directRefinesDependencies.let { edges -> if (from == edgeFrom) edges + edgeTo else edges }

        for (to in outgoingRefinesEdges) {
            if (to == edgeFrom)
                throw InvalidUserCodeException(
                    "Circular 'refines' hierarchy found in ${edgeFrom.containingModule}: " +
                            (stack.toList() + to).joinToString(" -> ") { it.fragmentName }
                )

            if (to !in visited) {
                checkEdgeFromReachableRecursively(to)
            }
        }
        stack -= from
    }

    checkEdgeFromReachableRecursively(edgeFrom)
}
