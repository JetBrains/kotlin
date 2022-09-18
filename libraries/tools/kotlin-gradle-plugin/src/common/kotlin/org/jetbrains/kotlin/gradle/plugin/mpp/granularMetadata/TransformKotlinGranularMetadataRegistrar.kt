/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.granularMetadata

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtensionOrNull
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.metadata.getMetadataCompilationForSourceSet
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.*
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.gradle.utils.withType
import java.util.concurrent.ConcurrentHashMap

/**
 * Entrypoint-class for registering and configuring [TransformKotlinGranularMetadata] task
 */
internal class TransformKotlinGranularMetadataRegistrar
private constructor(
    private val project: Project
) {
    fun taskName(sourceSet: KotlinSourceSet) = lowerCamelCaseName("transform", sourceSet.name, "MetadataDependencies")

    fun metadataDependenciesConfigurationName(sourceSet: KotlinSourceSet) = lowerCamelCaseName(sourceSet.name, "MetadataDependencies")

    fun registerForMetadataCompilation(compilation: AbstractKotlinCompilation<*>) {
        val sourceSet = compilation.defaultSourceSet
        val metadataDependenciesConfiguration = registerMetadataDependenciesConfiguration(sourceSet)
        val task = registerTask(sourceSet, metadataDependenciesConfiguration)

        val artifacts = metadataDependenciesConfiguration.incoming.artifacts

        // Add dependsOn closure as dependencies
        compilation.compileDependencyFiles += sourceSet.dependsOnClassesDirs

        // Inlcude non-multiplatform artifacts that cannot be transformed but still were requested for compilation
        compilation.compileDependencyFiles += project.files({ artifacts.filterNot { it.isMpp }.map { it.file } })

        // And finally include tranformed metadata artifacts;
        compilation.compileDependencyFiles += project.files({ task.map { it.transformedLibraries } }).builtBy(task)
    }

    private val ResolvedArtifactResult.isMpp: Boolean get() = variant.attributes.doesContainMultiplatformAttributes

    private val KotlinSourceSet.dependsOnClassesDirs get(): FileCollection {
            return project.files({
                  internal.dependsOnClosure.mapNotNull { hierarchySourceSet ->
                      val compilation =
                          project.getMetadataCompilationForSourceSet(
                              hierarchySourceSet
                          ) ?: return@mapNotNull null
                      compilation.output.classesDirs
                  }
              })
        }

    /**
     * Creates Compile Metadata Dependencies configurations
     */
    private fun registerMetadataDependenciesConfiguration(sourceSet: KotlinSourceSet): Configuration {
        val name = metadataDependenciesConfigurationName(sourceSet)

        val configurationFound = project.configurations.findByName(name)
        if (configurationFound != null) return configurationFound

        // Compile-scope configurations of a SourceSet
        val configurations = listOf(
            sourceSet.apiConfigurationName,
            sourceSet.compileOnlyConfigurationName,
            sourceSet.implementationConfigurationName
        ).map { project.configurations.getByName(it) }.toTypedArray()

        val dependsOnConfigurations = sourceSet.dependsOn.map { registerMetadataDependenciesConfiguration(it) }.toTypedArray()

        return project.configurations.create(name) { configuration ->
            configuration.isCanBeResolved = true
            configuration.isCanBeConsumed = false

            configuration.description = "Compile Metadata Dependencies for $sourceSet; created by ${this.javaClass}"

            configuration.attributes.attribute(KotlinPlatformType.attribute, KotlinPlatformType.common)
            configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
            configuration.attributes.attribute(Category.CATEGORY_ATTRIBUTE, project.categoryByName(Category.LIBRARY))

            configuration.extendsFrom(*configurations)
            configuration.extendsFrom(*dependsOnConfigurations)
        }
    }

    private fun registerTask(
        kotlinSourceSet: KotlinSourceSet,
        metadataDependenciesConfiguration: Configuration
    ): TaskProvider<TransformKotlinGranularMetadata> {

        /**
         * Transformation is possible only in Main Module, which is publishable
         * Any associated compilations such as test are not publishable thus their
         * intermediate sourceSets don't need to be compiled into a separate klib
         * and having granular metadata transformations
         * TODO: This will not be true when [TransformKotlinGranularMetadata] will be used in IDE Import
         */
        val platformMainCompilations = kotlinSourceSet
            .internal
            .compilations
            .filter { it !is KotlinCommonCompilation }
            .filter { it.isMain() }

        val platformVariantsDependenciesConfigurations = platformMainCompilations
            .map { project.configurations.getByName(it.compileDependencyConfigurationName) }
        val hostSpecificVariantsDependenciesConfigurations = hostSpecificDependencies(platformMainCompilations)

        val settings = TransformKotlinGranularMetadata.Settings(
            sourceSetName = kotlinSourceSet.name,

            resolvedSourceSetMetadataDependencies = ResolvedDependencyGraph(metadataDependenciesConfiguration),
            resolvedVariantDependencies = platformVariantsDependenciesConfigurations.map(::ResolvedDependencyGraph),

            projectsData = projectsData,
            resolvedHostSpecificDependencies = hostSpecificVariantsDependenciesConfigurations
                ?.map { project.provider { ResolvedDependencyGraph(it) } }
        )

        return project.locateOrRegisterTask(
            name = taskName(kotlinSourceSet),
            args = listOf(settings)
        ) {
            inputs.files(metadataDependenciesConfiguration)
        }
    }

    private val projectsData by lazy { collectProjectsData() }

    private fun collectProjectsData(): Map<String, TransformKotlinGranularMetadata.ProjectData> {
        return project.rootProject.allprojects.associateBy { it.path }.mapValues { (path, subProject) ->
            TransformKotlinGranularMetadata.ProjectData(
                path = path,
                sourceSetMetadataOutputs = project.provider { subProject.multiplatformExtensionOrNull?.sourceSetsMetadataOutputs() },
                projectStructureMetadata = project.provider { subProject.multiplatformExtensionOrNull?.kotlinProjectStructureMetadata },
                moduleId = project.provider { ModuleDependencyIdentifier(subProject.group.toString(), subProject.name) }
            )
        }
    }

    /**
     * For each sharedNative or platform compilation register and return configuration
     * that should resolve into Metadata artifacts of platform dependencies
     */
    private fun hostSpecificDependencies(compilations: List<KotlinCompilation<*>>): List<Configuration>? {
        val isSharedNative = compilations.isNotEmpty() && compilations.all {
            it.platformType == KotlinPlatformType.common || it.platformType == KotlinPlatformType.native
        }

        if (!isSharedNative) return null

        return compilations.map { compilation ->
            compilation as AbstractKotlinNativeCompilation

            val platformCompileDependencies = project.configurations.getByName(compilation.compileDependencyConfigurationName)

            val configurationName = lowerCamelCaseName("hostSpecificMetadataDependenciesOf", compilation.compileDependencyConfigurationName)

            project.configurations.getOrCreate(configurationName, invokeWhenCreated = { configuration ->
                configuration.isCanBeResolved = true
                configuration.isCanBeConsumed = false
                configuration.isVisible = false

                configuration.description = "Host specific metadata of compilation: $compilation"

                configuration.extendsFrom(*platformCompileDependencies.extendsFrom.toTypedArray())

                copyAttributes(platformCompileDependencies.attributes, configuration.attributes)
                configuration.attributes.attribute(Usage.USAGE_ATTRIBUTE, project.usageByName(KotlinUsages.KOTLIN_METADATA))
            })
        }
    }

    private fun KotlinMultiplatformExtension.sourceSetsMetadataOutputs(): Map<String, FileCollection> {
        val commonTarget = targets.withType<KotlinMetadataTarget>().singleOrNull() ?: return emptyMap()

        val compilations = commonTarget.compilations

        return sourceSets.mapNotNull { sourceSet ->
            val compilation = compilations.findByName(sourceSet.name)
                ?: return@mapNotNull null // given source set is not shared

            val destination = when (compilation) {
                is KotlinCommonCompilation -> compilation.output.classesDirs
                is KotlinSharedNativeCompilation -> compilation.output.classesDirs
                else -> error("Unexpected compilation type: $compilation")
            }

            Pair(sourceSet.name, destination)
        }.toMap()
    }

    companion object {
        private val instances = ConcurrentHashMap<Project, TransformKotlinGranularMetadataRegistrar>()
        fun create(project: Project): TransformKotlinGranularMetadataRegistrar {
            return instances.getOrPut(project) { TransformKotlinGranularMetadataRegistrar(project) }
        }
    }
}