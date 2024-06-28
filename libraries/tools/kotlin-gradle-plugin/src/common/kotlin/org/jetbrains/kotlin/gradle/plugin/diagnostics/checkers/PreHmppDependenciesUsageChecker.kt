/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.diagnostics.checkers

import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectChecker
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinGradleProjectCheckerContext
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnosticsCollector
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinMetadataTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages
import org.jetbrains.kotlin.gradle.plugin.mpp.resolvableMetadataConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.isKotlinGranularMetadataEnabled
import java.util.*
import java.util.concurrent.ConcurrentHashMap

internal object PreHmppDependenciesUsageChecker : KotlinGradleProjectChecker {
    override suspend fun KotlinGradleProjectCheckerContext.runChecks(collector: KotlinToolingDiagnosticsCollector) {
        KotlinPluginLifecycle.Stage.ReadyForExecution.await()

        if (!project.isKotlinGranularMetadataEnabled || project.kotlinPropertiesProvider.allowLegacyMppDependencies) return

        val metadataTarget = project.multiplatformExtensionOrNull?.awaitTargets()
            ?.matching { it is KotlinMetadataTarget }
            ?.singleOrNull()
            ?: return

        // Note that we do not inspect dependencies of compilations, because adding dependencies into compilation is esoteric enough on its own,
        // and x2 esoteric for KotlinMetadataTarget
        val sourceSetsInMetadataCompilations = metadataTarget.compilations.flatMapTo(mutableSetOf()) { it.allKotlinSourceSets }
        val configurationsToInspect = sourceSetsInMetadataCompilations.map { it.internal.resolvableMetadataConfiguration }

        // Resolution of configurations can happen concurrently, so need to use thread-safe primitives
        val reportedDependencies: MutableSet<ComponentIdentifier> = Collections.newSetFromMap(ConcurrentHashMap())
        val processedDependencies: MutableSet<ComponentIdentifier> = Collections.newSetFromMap(ConcurrentHashMap())

        for (configuration in configurationsToInspect) {
            configuration.incoming.afterResolve { resolvableDependencies ->
                val resolvedDependencies = resolvableDependencies.resolutionResult.root.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    // We don't want to report deprecation on transitive dependencies. Gradle will add them into list of 'dependencies',
                    // but will mark them as 'isConstraint'
                    .filter { it.selected.id is ModuleComponentIdentifier && !it.isConstraint }

                for (dependency in resolvedDependencies) {
                    val dependencyId = dependency.selected.id as ModuleComponentIdentifier
                    if (!processedDependencies.add(dependencyId)) continue

                    if (isPreHmppDependency(dependency) && reportedDependencies.add(dependencyId)) {
                        collector.reportOncePerGradleBuild(
                            project,
                            KotlinToolingDiagnostics.PreHmppDependenciesUsedInBuild(dependencyId.displayName),
                            key = dependencyId.displayName
                        )
                    }
                }
            }
        }
    }

    private fun isPreHmppDependency(dependency: ResolvedDependencyResult): Boolean {
        if (isAllowedDependency(dependency.selected.id)) return false
        val attributes = dependency.resolvedVariant.attributes
        val kotlinPlatformAttribute = attributes.getAttribute(Attribute.of(KotlinPlatformType.attribute.name, String::class.java))
            ?: return false
        val usageAttribute = attributes.getAttribute(Attribute.of(Usage.USAGE_ATTRIBUTE.name, String::class.java)) ?: return false

        return kotlinPlatformAttribute == KotlinPlatformType.common.name && usageAttribute != KotlinUsages.KOTLIN_METADATA
    }

    private fun isAllowedDependency(identifier: ComponentIdentifier): Boolean {
        return when {
            identifier !is ModuleComponentIdentifier -> false
            identifier.group != "org.jetbrains.kotlin" -> false
            identifier.module.contains("kotlin-stdlib") || identifier.module.contains("kotlin-test") -> true
            else -> false
        }
    }
}
