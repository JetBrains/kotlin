/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.publication

import groovy.util.Node
import groovy.util.NodeList
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Dependency
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.utils.future

internal class UklibPomDependenciesRewriter {
    data class DependencyGA(
        val group: String?,
        val artifact: String,
    )

    /**
     * FIXME: We actually need to make sure that dependencies recorded in the POM get bumped to the highest version resolved by any platform
     * or metadata compilation
     */
    fun rewriteDependencies(
        pomXml: XmlProvider,
        scopeMapping: Map<DependencyGA, KotlinUsageContext.MavenScope?>,
    ) {
        val dependenciesNode = (pomXml.asNode().get("dependencies") as NodeList).filterIsInstance<Node>().singleOrNull() ?: return
        val dependencyNodes = (dependenciesNode.get("dependency") as? NodeList).orEmpty().filterIsInstance<Node>()

        dependencyNodes.forEach { dependencyNode ->
            val group = ((dependencyNode.get("groupId") as NodeList).singleOrNull() as Node?)?.text() ?: return@forEach
            val artifact = ((dependencyNode.get("artifactId") as NodeList).singleOrNull() as Node?)?.text() ?: return@forEach
            val scope = ((dependencyNode.get("scope") as NodeList).singleOrNull() as Node?)

            // Leave if it's already compile
            if (scope?.text() == "compile") return@forEach
            // FIXME: Drop this and rewrite everything to compile
            scopeMapping[DependencyGA(group, artifact)]?.let {
                when (it) {
                    KotlinUsageContext.MavenScope.COMPILE -> scope?.setValue("compile")
                    KotlinUsageContext.MavenScope.RUNTIME -> scope?.setValue("runtime")
                }
            }
        }
    }

    companion object {
        // FIXME: This is only a PoC, needs a complete rewrite and testing
        fun deriveUklibDependencyScopeMapping(
            project: Project,
            rootComponent: KotlinSoftwareComponent,
        ): Provider<MutableMap<DependencyGA, KotlinUsageContext.MavenScope>> {
            data class ScopedConfigurationDependencies(
                val dependencySet: List<Dependency>,
                val scope: KotlinUsageContext.MavenScope?,
            )

            val map = project.provider {
                val dependencyRemapping = mutableMapOf<DependencyGA, KotlinUsageContext.MavenScope>()
                project.launch {
                    rootComponent.subcomponentTargets().flatMap {
                        it.internal.kotlinComponents
                            .filterIsInstance<KotlinVariant>()
                            .flatMap { publishedComponent ->
                                publishedComponent.internal.usages
                                    .filterIsInstance<DefaultKotlinUsageContext>()
                                    .filter { it.publishOnlyIf.predicate() }
                                    .map { publishedVariant ->
                                        // FIXME: This breaks with project dependencies with PI
                                        ScopedConfigurationDependencies(
                                            project.configurations.getByName(publishedVariant.dependencyConfigurationName)
                                                .allDependencies.toList(),
                                            publishedVariant.mavenScope,
                                        )
                                    }
                            }
                    }.forEach { set ->
                        set.dependencySet.forEach { dependency ->
                            val dep = DependencyGA(dependency.group, dependency.name) // mavenPublication {customArtifact = "custom" group = "custom" }
                            val exScope = dependencyRemapping[dep]
                            when (exScope) {
                                KotlinUsageContext.MavenScope.COMPILE -> {}
                                KotlinUsageContext.MavenScope.RUNTIME -> dependencyRemapping[dep] = set.scope ?: KotlinUsageContext.MavenScope.COMPILE
                                null -> dependencyRemapping[dep] = set.scope ?: KotlinUsageContext.MavenScope.COMPILE
                            }
                        }
                    }
                }
                dependencyRemapping
            }
            return map
        }
    }
}