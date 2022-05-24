/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.utils.getOrPutRootProjectProperty
import org.jetbrains.kotlin.project.model.KpmModuleDependency
import org.jetbrains.kotlin.project.model.KpmModuleIdentifier

open class GradleKpmComponentResultCachingResolver {
    private val cachedResultsByRequestingModule = mutableMapOf<GradleKpmModule, Map<KpmModuleIdentifier, ResolvedComponentResult>>()

    protected open fun configurationToResolve(requestingModule: GradleKpmModule): Configuration =
        configurationToResolveMetadataDependencies(requestingModule.project, requestingModule)

    protected open fun resolveDependencies(module: GradleKpmModule): Map<KpmModuleIdentifier, ResolvedComponentResult> {
        val allComponents = configurationToResolve(module).incoming.resolutionResult.allComponents
        // FIXME handle multi-component results
        return allComponents.flatMap { component -> component.toKpmModuleIdentifiers().map { it to component } }.toMap()
    }

    private fun getResultsForModule(module: GradleKpmModule): Map<KpmModuleIdentifier, ResolvedComponentResult> =
        cachedResultsByRequestingModule.getOrPut(module) { resolveDependencies(module) }

    fun resolveModuleDependencyAsComponentResult(
        requestingModule: GradleKpmModule,
        moduleDependency: KpmModuleDependency
    ): ResolvedComponentResult? =
        getResultsForModule(requestingModule)[moduleDependency.moduleIdentifier]

    companion object {
        fun getForCurrentBuild(project: Project): GradleKpmComponentResultCachingResolver {
            val extraPropertyName = "org.jetbrains.kotlin.dependencyResolution.gradleComponentResolver.${project.getKotlinPluginVersion()}"
            return project.getOrPutRootProjectProperty(extraPropertyName) {
                GradleKpmComponentResultCachingResolver()
            }
        }
    }
}
