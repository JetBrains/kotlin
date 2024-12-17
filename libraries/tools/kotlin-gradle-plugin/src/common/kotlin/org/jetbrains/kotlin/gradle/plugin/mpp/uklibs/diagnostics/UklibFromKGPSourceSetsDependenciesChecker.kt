/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.uklibs.diagnostics

import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.jetbrains.kotlin.gradle.internal.KOTLIN_DOM_API_MODULE_NAME
import org.jetbrains.kotlin.gradle.internal.KOTLIN_MODULE_GROUP
import org.jetbrains.kotlin.gradle.internal.stdlibModules
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal

internal object UklibFromKGPSourceSetsDependenciesChecker {

    data class DependencyDeclarationViolation(
        val configuration: Configuration,
        val uniqueDependencies: Set<Dependency>,
    )

    /**
     * Check that dependencies are specified consistently in all compilations which are going to be published as if we already has library to
     * library dependencies.
     *
     * This check unfortunately can't be accurate unless the entire graph of dependencies resolves to Uklibs, so we don't even try to resolve
     * these configurations and only look at the declared dependencies.
     */
    fun findInconsistentDependencyDeclarations(
        uklibPublishedPlatformCompilations: List<KotlinCompilation<*>>,
        publishedMetadataCompilations: List<KotlinCompilation<*>>,
    ): Set<DependencyDeclarationViolation> {
        // FIXME: This filtering is not accurate if these are specified manually
        val ignoreDependenciesInsertedByDefault = setOf(
            KOTLIN_DOM_API_MODULE_NAME,
        ) + stdlibModules

        fun Configuration.declaredDependencies() = incoming.dependencies.filterNot {
            it.group == KOTLIN_MODULE_GROUP && it.name in ignoreDependenciesInsertedByDefault
        }.toSet()

        val compilationDependencies = uklibPublishedPlatformCompilations.associate {
            val conf = it.internal.configurations.compileDependencyConfiguration
            conf to conf.declaredDependencies()
        } + publishedMetadataCompilations.associate {
            // For metadata compilations we always resolve resolvableMetadataConfiguration instead of the compileDependencyConfiguration
            val conf = it.defaultSourceSet.internal.resolvableMetadataConfiguration
            conf to conf.declaredDependencies()
        }

        // Ignore dependencies that are shared between all compilations
        val sharedDependencies = compilationDependencies.values.first().toMutableSet()
        compilationDependencies.values.forEach {
            sharedDependencies.retainAll(it)
        }
        val violations = mutableListOf<DependencyDeclarationViolation>()
        compilationDependencies.forEach {
            val configuration = it.key
            val dependencies = it.value

            val uniqueDependencies = dependencies - sharedDependencies
            if (uniqueDependencies.isNotEmpty()) {
                violations.add(
                    DependencyDeclarationViolation(
                        configuration,
                        uniqueDependencies,
                    )
                )
            }
        }
        return violations.toSet()
    }

}