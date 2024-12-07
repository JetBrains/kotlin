package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.util.GradleVersion
import org.gradle.work.NormalizeLineEndings
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider.Companion.kotlinPropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.internal.kotlinSecondaryVariantsDataSharing
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.projectStructureMetadataResolvedConfiguration
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.utils.currentBuild
import org.jetbrains.kotlin.gradle.utils.filesProvider
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

internal class MetadataDependencyTransformationTaskInputs(
    project: Project,
    kotlinSourceSet: KotlinSourceSet,
    private val keepProjectDependencies: Boolean = true,
) {
    private val currentBuild = project.currentBuild

    // GMT algorithm uses the project-structure-metadata.json files from the other subprojects.
    // Resolving `projectStructureMetadataResolvableConfiguration` triggers other subprojects' tasks
    // to generate project-structure-metadata.json.
    // Thus, this should be a Gradle input to trigger the whole process.
    @Suppress("unused") // Gradle input
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    val projectStructureMetadataFileCollection = kotlinSourceSet
        .internal
        .projectStructureMetadataResolvedConfiguration()
        .files

    @Suppress("unused") // Gradle input
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    val metadataLocationsOfProjectDependencies: FileCollection =
        // This configuration is resolvable only for P2P dependencies, for IDE import we should not invoke sourceSet metadata compilations
        project.kotlinSecondaryVariantsDataSharing
            .consumeCommonSourceSetMetadataLocations(kotlinSourceSet.internal.resolvableMetadataConfiguration, keepProjectDependencies)
            .files

    @Suppress("unused") // Gradle input
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    val configurationToResolve: FileCollection = kotlinSourceSet
        .internal
        .resolvableMetadataConfiguration
        .applyIf(!keepProjectDependencies) { withoutProjectDependencies() }


    @Suppress("unused") // Gradle input
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:IgnoreEmptyDirectories
    @get:NormalizeLineEndings
    val hostSpecificMetadataConfigurationsToResolve: FileCollection = project.filesProvider {
        kotlinSourceSet.internal.compilations
            .filter { compilation ->
                if (compilation is KotlinNativeCompilation) {
                    compilation.konanTarget.enabledOnCurrentHostForKlibCompilation(project.kotlinPropertiesProvider)
                } else {
                    true
                }
            }.mapNotNull { compilation ->
                compilation
                    .internal
                    .configurations
                    .hostSpecificMetadataConfiguration
                    ?.applyIf(!keepProjectDependencies) { withoutProjectDependencies() }
            }
    }

    @Transient // Only needed for configuring task inputs;
    private val participatingSourceSets: Set<KotlinSourceSet> = kotlinSourceSet.internal.withDependsOnClosure

    @Suppress("unused") // Gradle input
    @get:Input
    val inputSourceSetsAndCompilations: Map<String, Iterable<String>> by lazy {
        participatingSourceSets.associate { sourceSet ->
            sourceSet.name to sourceSet.internal.compilations.map { it.name }.sorted()
        }
    }

    @Suppress("unused") // Gradle input
    @get:Input
    val inputCompilationDependencies: Map<String, Set<String>> by lazy {
        participatingSourceSets.flatMap { it.internal.compilations }.associate {
            it.name to project.configurations.getByName(it.compileDependencyConfigurationName)
                .allDependencies
                .map { dependency ->
                    if (dependency is ProjectDependency && keepProjectDependencies) {
                        if (GradleVersion.current() < GradleVersion.version("8.11")) {
                            @Suppress("DEPRECATION")
                            dependency.dependencyProject.path
                        } else {
                            dependency.path
                        }
                    } else {
                        "${dependency.name}:${dependency.group}:${dependency.version}"
                    }
                }
                .toSet()
        }
    }

    private fun Configuration.withoutProjectDependencies(): FileCollection {
        return incoming.artifactView { view ->
            view.componentFilter { componentIdentifier -> componentIdentifier !in currentBuild }
        }.files
    }

}
