/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.artifacts.result.ResolvedVariantResult
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.usageByName
import java.io.File

internal class ResolvedMppVariantsProvider private constructor(private val project: Project) {
    companion object {

        fun get(project: Project): ResolvedMppVariantsProvider = with(project.extensions.extraProperties) {
            val propertyName = "kotlin.mpp.internal.resolvedModuleVariantsProvider"
            if (!has(propertyName)) {
                set(propertyName, ResolvedMppVariantsProvider(project))
            }
            @Suppress("UNCHECKED_CAST")
            get(propertyName) as ResolvedMppVariantsProvider
        }
    }

    /** Gets the name of the variant that the module specified by the [moduleIdentifier] resolved to in the given [configuration].
     * The [moduleIdentifier] may be either the root module or a platform-specific module, the result is the same for the two cases. */
    fun getResolvedVariantName(moduleIdentifier: ModuleDependencyIdentifier, configuration: Configuration): String? =
        getEntryForModule(moduleIdentifier).run {
            if (configuration !in resolvedVariantsByConfiguration) {
                resolveConfigurationAndSaveVariants(configuration, artifactResolutionMode = ArtifactResolutionMode.NONE)
            }
            val variants = resolvedVariantsByConfiguration.getOrPut(configuration) { null }
            variants?.singleOrNull()?.displayName
        }

    /** Gets the artifact that contains the common code metadata for the given [rootModuleIdentifier], which can only denote the root
     * module of a multiplatform project, not one of its platform-specific modules, as seen in the given [configuration].
     * If the [configuration] requests platform artifacts and not the common code metadata, then this function will resolve its
     * dependencies to metadata separately. */
    fun getMetadataArtifactByRootModule(rootModuleIdentifier: ModuleDependencyIdentifier, configuration: Configuration): File? {
        val rootModuleEntry = getEntryForModule(rootModuleIdentifier)

        val platformModuleEntry = rootModuleEntry.run {
            if (configuration !in chosenPlatformModuleByConfiguration) {
                resolveConfigurationAndSaveVariants(configuration, artifactResolutionMode = ArtifactResolutionMode.METADATA)
            }
            chosenPlatformModuleByConfiguration.getOrPut(configuration) { null }
        }

        return platformModuleEntry?.run {
            // The condition might be true if the configuration has only been resolved with resolution mode NORMAL
            if (configuration !in resolvedMetadataArtifactByConfiguration) {
                resolveConfigurationAndSaveVariants(configuration, artifactResolutionMode = ArtifactResolutionMode.METADATA)
            }
            resolvedMetadataArtifactByConfiguration.getOrPut(configuration) { null }
        }
    }

    /** Gets the artifact of a particular MPP platform-specific [moduleIdentifier] as resolved in the [configuration]. */
    fun getPlatformArtifactByPlatformModule(moduleIdentifier: ModuleDependencyIdentifier, configuration: Configuration): File? =
        getEntryForModule(moduleIdentifier).run {
            if (configuration !in resolvedArtifactByConfiguration) {
                resolveConfigurationAndSaveVariants(configuration, artifactResolutionMode = ArtifactResolutionMode.NORMAL)
            }
            resolvedArtifactByConfiguration.getOrPut(configuration) { null }
        }

    private fun getEntryForModule(moduleIdentifier: ModuleDependencyIdentifier) =
        entriesCache.getOrPut(moduleIdentifier, { ModuleEntry(moduleIdentifier) })

    private val entriesCache: MutableMap<ModuleDependencyIdentifier, ModuleEntry> = mutableMapOf()

    private val mppComponentIdsByConfiguration: MutableMap<Configuration, Set<ComponentIdentifier>> = mutableMapOf()

    private enum class ArtifactResolutionMode {
        NONE, NORMAL, METADATA
    }

    private fun resolveConfigurationAndSaveVariants(
        configuration: Configuration,
        artifactResolutionMode: ArtifactResolutionMode
    ) {
        val mppComponentIds: Set<ComponentIdentifier> = mppComponentIdsByConfiguration.getOrPut(configuration) {
            resolveMppComponents(configuration)
        }

        if (artifactResolutionMode != ArtifactResolutionMode.NONE) {
            val artifacts = resolveArtifacts(artifactResolutionMode, configuration, mppComponentIds)
            matchMppComponentsWithResolvedArtifacts(mppComponentIds, artifacts, configuration, artifactResolutionMode)
        }
    }

    private fun resolveMppComponents(configuration: Configuration): MutableSet<ComponentIdentifier> {
        val result = mutableListOf<ResolvedComponentResult>()

        configuration.incoming.resolutionResult.allComponents { component ->
            val moduleId = ModuleIds.fromComponent(project, component)
            val variants = component.variants

            val isMpp = variants.any { variant -> variant.attributes.keySet().any { it.name == KotlinPlatformType.attribute.name } }
            if (isMpp) {
                result.add(component)
                val moduleEntry = getEntryForModule(moduleId)
                moduleEntry.resolvedVariantsByConfiguration[configuration] = variants

                moduleEntry.dependenciesByConfiguration[configuration] = component.dependencies
                    .filterIsInstance<ResolvedDependencyResult>()
                    .map { dependency -> ModuleIds.fromComponent(project, dependency.selected) }

                if (component.id is ProjectComponentIdentifier) {
                    // Then the platform variant chosen for this module is definitely inside the module itself:
                    moduleEntry.chosenPlatformModuleByConfiguration[configuration] = moduleEntry
                }
            }
        }

        return result.mapTo(mutableSetOf()) { it.id }
    }

    private fun resolveArtifacts(
        artifactResolutionMode: ArtifactResolutionMode,
        configuration: Configuration,
        mppComponentIds: Set<ComponentIdentifier>
    ): Map<ComponentIdentifier, ResolvedArtifactResult> {
        val artifactsConfiguration =
            if (
                artifactResolutionMode == ArtifactResolutionMode.NORMAL ||
                configuration.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name == KotlinUsages.KOTLIN_METADATA
            ) {
                configuration
            } else {
                configuration.copyRecursive().apply {
                    attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
                }
            }

        return artifactsConfiguration.incoming.artifactView { view ->
            view.componentFilter { it in mppComponentIds }
            view.attributes { attrs -> attrs.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA)) }
            view.lenient(true)
        }.artifacts.associateBy { it.id.componentIdentifier }
    }

    private fun matchMppComponentsWithResolvedArtifacts(
        mppComponentIds: Set<ComponentIdentifier>,
        artifacts: Map<ComponentIdentifier, ResolvedArtifactResult>,
        configuration: Configuration,
        artifactResolutionMode: ArtifactResolutionMode
    ) {
        val mppModuleIds = mppComponentIds.mapTo(mutableSetOf()) { ModuleIds.fromComponentId(project, it) }

        mppComponentIds.forEach { componentId ->
            val moduleEntry = getEntryForModule(ModuleIds.fromComponentId(project, componentId))
            val artifact = artifacts[componentId]
            when {
                // With project dependencies, we don't need the host-specific metadata artifacts, as we have the compilation outputs:
                componentId is ProjectComponentIdentifier -> {
                    moduleEntry.resolvedMetadataArtifactByConfiguration[configuration] = null
                }

                // We found the host-specific artifact for a platform variant of some MPP:
                artifact != null -> {
                    val resolvedArtifactMap = when (artifactResolutionMode) {
                        ArtifactResolutionMode.NORMAL -> moduleEntry.resolvedArtifactByConfiguration
                        ArtifactResolutionMode.METADATA -> moduleEntry.resolvedMetadataArtifactByConfiguration
                        else -> error("unexpected $artifactResolutionMode")
                    }
                    resolvedArtifactMap[configuration] = artifact.file
                }

                // Otherwise, this may be a root module of some MPP that resolved to a variant in another module. Take a note of that.
                else -> {
                    // TODO: there's an assumption that resolving a root MPP module to a host-specific metadata artifact and to a platform
                    //  artifact will choose variants that are published within the same Maven module; change this code if that's not
                    //  true anymore.
                    val singleDependencyId = moduleEntry.dependenciesByConfiguration.getValue(configuration).singleOrNull()
                    if (singleDependencyId != null && singleDependencyId in mppModuleIds) {
                        moduleEntry.chosenPlatformModuleByConfiguration[configuration] =
                            getEntryForModule(singleDependencyId)
                    }
                }
            }
        }
    }

    /**
     * Stores resolution results of the module denoted by [moduleIdentifier] in different configurations of the project.
     * The [moduleIdentifier] may point to a root module of a multiplatform project (then it has meaningful
     * [chosenPlatformModuleByConfiguration]) or to a platform module.
     */
    private class ModuleEntry(
        @Suppress("unused") // simplify debugging
        val moduleIdentifier: ModuleDependencyIdentifier
    ) {
        val dependenciesByConfiguration: MutableMap<Configuration, List<ModuleDependencyIdentifier>> = HashMap()
        val resolvedVariantsByConfiguration: MutableMap<Configuration, List<ResolvedVariantResult?>?> = HashMap()
        val resolvedArtifactByConfiguration: MutableMap<Configuration, File?> = HashMap()
        val resolvedMetadataArtifactByConfiguration: MutableMap<Configuration, File?> = HashMap()
        val chosenPlatformModuleByConfiguration: MutableMap<Configuration, ModuleEntry?> = HashMap()
    }
}