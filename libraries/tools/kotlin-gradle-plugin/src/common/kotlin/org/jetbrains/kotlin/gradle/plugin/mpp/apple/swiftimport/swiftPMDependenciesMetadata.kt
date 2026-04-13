/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftimport

import org.gradle.api.Project
import org.gradle.api.artifacts.ArtifactView
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.categoryByName
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinUsages.KOTLIN_METADATA
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.plugin.mpp.isMain
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.plugin.usageByName
import org.jetbrains.kotlin.gradle.utils.createConsumable
import org.jetbrains.kotlin.gradle.utils.getAttributeSafely
import org.jetbrains.kotlin.gradle.utils.maybeCreateDependencyScope
import org.jetbrains.kotlin.gradle.utils.maybeCreateResolvable

private const val SWIFTPM_DEPENDENCIES_METADATA_USAGE = "swiftPMDependenciesMetadata"
internal const val SWIFTPM_DEPENDENCIES_METADATA_FOR_LOCK_FILES_USAGE = "swiftPMDependenciesMetadataForLockFiles"

@Suppress("UNCHECKED_CAST")
private fun swiftPMDependencies(swiftPMDependenciesMetadataClasspath: ArtifactView): Provider<TransitiveSwiftPMDependencies> {
    return swiftPMDependenciesMetadataClasspath
        .artifacts.resolvedArtifacts
        .map { artifacts ->
            val metadataByDependencyIdentifier = artifacts
                .filter {
                    // Filter out variants that didn't specify Usage and resolved by accident: KT-85517
                    it.variant.attributes.getAttributeSafely(Usage.USAGE_ATTRIBUTE) == SWIFTPM_DEPENDENCIES_METADATA_USAGE
                }
                .associate { resolvedArtifact ->
                    val (swiftPMPackageIdentifier, isModular) = when (val componentId = resolvedArtifact.id.componentIdentifier) {
                        is ProjectComponentIdentifier -> componentId.projectPath to false
                        is ModuleComponentIdentifier -> "${componentId.group}_${componentId.module}_${componentId.version}" to true
                        else -> error("Unexpected componentId: $componentId")
                    }
                    SwiftPMDependencyIdentifier(
                        swiftPMPackageIdentifier.replace(Regex("[^a-zA-Z0-9]"), "_"),
                        isModular = isModular,
                    ) to resolvedArtifact.file.inputStream().use {
                        deserializeSwiftPMImportMetadata(it)
                    }
                }
            TransitiveSwiftPMDependencies(metadataByDependencyIdentifier)
        }
}

internal fun Project.swiftPMDependenciesForLockFilesScopeConfiguration(): Configuration {
    return project.configurations.maybeCreateDependencyScope("swiftPMDependenciesForLockFilesMetadataClasspathDependencies")
}

internal fun Project.swiftPMDependenciesForLockFilesResolvableMetadataConfiguration(): Configuration {
    return project.configurations.maybeCreateResolvable("swiftPMDependenciesForLockFilesMetadataClasspath") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(SWIFTPM_DEPENDENCIES_METADATA_FOR_LOCK_FILES_USAGE))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)

        extendsFrom(project.swiftPMDependenciesForLockFilesScopeConfiguration())
    }
}

private fun Project.swiftPMDependenciesResolvableMetadataConfiguration(): Configuration {
    return project.configurations.maybeCreateResolvable("swiftPMDependenciesMetadataClasspath") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KOTLIN_METADATA))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
    }
}

internal fun Project.inheritSwiftPMDependenciesFromAppleCompilationDependencies() {
    project.launch {
        KotlinPluginLifecycle.Stage.AfterFinaliseCompilations.await()
        val sourceSets = multiplatformExtension.awaitSourceSets()
        val appleCompilations = sourceSets.mapNotNull {
            val compilation = it.internal.compilations.singleOrNull() ?: return@mapNotNull null
            if (compilation.isMain() && compilation is KotlinNativeCompilation && compilation.konanTarget.family.isAppleFamily) {
                return@mapNotNull compilation
            }
            null
        }
        val swiftPMDependenciesMetadata = swiftPMDependenciesResolvableMetadataConfiguration()
        appleCompilations.map {
            configurations.getByName(
                it.compilation.internal.compileDependencyConfigurationName
            )
        }.forEach {
            swiftPMDependenciesMetadata.extendsFrom(it)
        }
    }
}

internal fun Project.transitiveSwiftPMDependenciesProvider(): Provider<TransitiveSwiftPMDependencies> = swiftPMDependencies(
    // 1. Select metadataApiElements component graph
    swiftPMDependenciesResolvableMetadataConfiguration().incoming.artifactView {
        // 2. Reselect SwiftPM metadata variant
        it.withVariantReselection()
        it.attributes {
            it.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(SWIFTPM_DEPENDENCIES_METADATA_USAGE))
            it.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        }
        // SwiftPM metadata is optional, so select it only if it exists
        it.lenient(true)
    }
)

internal fun Project.registerSwiftPMDependenciesMetadataApiElements(swiftPMDependenciesMetadata: TaskProvider<SerializeSwiftPMDependenciesMetadata>): Configuration {
    return project.configurations.createConsumable("swiftPMDependenciesMetadataElements") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(SWIFTPM_DEPENDENCIES_METADATA_USAGE))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        outgoing.artifact(swiftPMDependenciesMetadata) {
            it.classifier = "swiftpm-metadata"
            it.extension = "json"
        }
    }.get()
}

internal fun Project.registerSwiftPMDependenciesMetadataForLockFilesApiElements(swiftPMDependenciesMetadata: TaskProvider<SerializeSwiftPMDependenciesMetadataForLockFiles>): Configuration {
    return project.configurations.createConsumable("swiftPMDependenciesMetadataElementsForLockFiles") {
        attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(SWIFTPM_DEPENDENCIES_METADATA_FOR_LOCK_FILES_USAGE))
        attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))
        outgoing.artifact(swiftPMDependenciesMetadata) {
            it.classifier = "swiftpm-metadata"
        }
    }.get()
}
