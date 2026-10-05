/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("TYPEALIAS_EXPANSION_DEPRECATION")

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.jetbrains.kotlin.gradle.dsl.kotlinExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.targets.js.npm.NPM_DEP_FILE_VERSION_PREFIX
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyDeprecated
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmDependencyScopeDeprecated
import org.jetbrains.kotlin.gradle.utils.normalizedAbsoluteFile
import java.io.File

/**
 * Warn about NPM dependencies that are passed to a Gradle dependency scope of a source set,
 * as in `implementation(npm("is-odd-even", "1.0.0"))`.
 */
internal object NpmDependencyInGradleScopeChecker : KotlinGradleProjectChecker {

    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        val sourceSets = project.kotlinExtensionOrNull?.sourceSets ?: return

        val usages = sourceSets.flatMap { sourceSet -> npmDependencyUsages(sourceSet) }
        if (usages.isEmpty()) return

        collector.report(diagnosticsContext, KotlinToolingDiagnostics.NpmDependencyInGradleScope(usages))
    }

    /**
     * Get all NPM dependencies that are declared in a Gradle dependency scope of [sourceSet].
     *
     * Fetches Configurations leniently, just in case a plugin (e.g. AGP) isn't configured correctly.
     */
    private fun KotlinGradleProjectCheckerContext.npmDependencyUsages(
        sourceSet: KotlinSourceSet,
    ): List<NpmDependencyInGradleScopeUsage> {
        val dependencyScopes = listOf(
            "api" to sourceSet.apiConfigurationName,
            "implementation" to sourceSet.implementationConfigurationName,
            "compileOnly" to sourceSet.compileOnlyConfigurationName,
            "runtimeOnly" to sourceSet.runtimeOnlyConfigurationName,
        )

        val usages = mutableListOf<NpmDependencyInGradleScopeUsage>()
        for ((dependencyScope, configurationName) in dependencyScopes) {
            project.configurations
                .findByName(configurationName)
                ?.dependencies
                ?.configureEach { dependency ->
                    if (dependency is NpmDependencyDeprecated) {
                        val arguments = renderArguments(dependency)
                        usages += NpmDependencyInGradleScopeUsage(
                            sourceSetName = sourceSet.name,
                            dependencyScope = dependencyScope,
                            deprecatedDeclaration = "${dependency.deprecatedFunction()}($arguments)",
                            replacement = "${dependency.replacementFunction()}($arguments)",
                        )
                    }
                }
        }
        return usages
    }

    private fun NpmDependencyDeprecated.deprecatedFunction(): String = when (scope) {
        NpmDependencyScopeDeprecated.NORMAL -> "npm"
        NpmDependencyScopeDeprecated.DEV -> "devNpm"
        NpmDependencyScopeDeprecated.OPTIONAL -> "optionalNpm"
        NpmDependencyScopeDeprecated.PEER -> "peerNpm"
    }

    private fun NpmDependencyDeprecated.replacementFunction(): String = when (scope) {
        NpmDependencyScopeDeprecated.NORMAL -> "npm"
        NpmDependencyScopeDeprecated.DEV -> "npmDev"
        NpmDependencyScopeDeprecated.OPTIONAL -> "npmOptional"
        NpmDependencyScopeDeprecated.PEER -> "npmPeer"
    }

    private fun KotlinGradleProjectCheckerContext.renderArguments(dependency: NpmDependencyDeprecated): String {
        val version = dependency.getVersion()
        if (!version.startsWith(NPM_DEP_FILE_VERSION_PREFIX)) {
            return """"${dependency.getName()}", "$version""""
        }

        val directory = File(version.removePrefix(NPM_DEP_FILE_VERSION_PREFIX))
        val relativeToProject = directory.relativeToOrNull(project.projectDir.normalizedAbsoluteFile())
        val path = relativeToProject?.invariantSeparatorsPath?.takeIf { !it.startsWith("..") }
            ?: directory.invariantSeparatorsPath

        return """"${dependency.getName()}", project.file("$path")"""
    }
}

/** Should be used only in [NpmDependencyInGradleScopeUsage] and [KotlinToolingDiagnostics.NpmDependencyInGradleScope] */
internal data class NpmDependencyInGradleScopeUsage(
    val sourceSetName: String,
    val dependencyScope: String,
    val deprecatedDeclaration: String,
    val replacement: String,
)
