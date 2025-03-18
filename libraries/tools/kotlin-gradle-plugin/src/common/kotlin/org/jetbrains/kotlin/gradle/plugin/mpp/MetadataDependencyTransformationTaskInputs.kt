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
import java.io.IOException
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.*

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
    val projectStructureMetadataFileCollection: FileCollection = kotlinSourceSet
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
    @get:Internal
    @get:Deprecated("TODO add deprecation msg. Scheduled for removal in Kotlin 2.4.")
    val inputSourceSetsAndCompilations: Map<String, Iterable<String>> = emptyMap()

    @get:Input
    @Suppress("unused") // Gradle input
    val inputSourceSetsAndCompilationsChecksum: String by lazy {
        Hasher().use { hasher ->
            participatingSourceSets.forEach { sourceSet ->
                hasher.write(sourceSet.name)

                hasher.write("[")
                sourceSet.internal.compilations
                    .mapTo(sortedSetOf()) { it.name }
                    .forEach { name ->
                        hasher.write(name)
                        hasher.write(",")
                    }

                hasher.write("]")
            }

            hasher.get()
        }
    }

    @Suppress("unused") // Gradle input
    @get:Internal
    @get:Deprecated("TODO add deprecation msg. Scheduled for removal in Kotlin 2.4.")
    val inputCompilationDependencies: Map<String, Set<String>> = emptyMap()

    @get:Input
    @Suppress("unused") // Gradle input
    val inputCompilationDependenciesChecksum: String by lazy {
        Hasher().use { hasher ->
            participatingSourceSets
                .flatMap { it.internal.compilations }
                .forEach { compilation ->
                    hasher.write(compilation.name)
                    hasher.write("[")
                    project.configurations.getByName(compilation.compileDependencyConfigurationName)
                        .allDependencies
                        .forEach { dependency ->
                            if (dependency is ProjectDependency && keepProjectDependencies) {
                                if (GradleVersion.current() < GradleVersion.version("8.11")) {
                                    @Suppress("DEPRECATION")
                                    hasher.write(dependency.dependencyProject.path)
                                } else {
                                    hasher.write(dependency.path)
                                }
                            } else {
                                hasher.write(dependency.name)
                                hasher.write(":")
                                hasher.write(dependency.group ?: "")
                                hasher.write(":")
                                hasher.write(dependency.version ?: "")
                            }
                            hasher.write(",")
                        }
                    hasher.write("]")
                }
            hasher.get()
        }
    }

    private fun Configuration.withoutProjectDependencies(): FileCollection {
        return incoming.artifactView { view ->
            view.componentFilter { componentIdentifier -> componentIdentifier !in currentBuild }
        }.files
    }
}


private class Hasher : AutoCloseable {
    private val messageDigester = MessageDigest.getInstance("MD5")
    private val writer: OutputStreamWriter = DigestOutputStream(nullOutputStream(), messageDigester).writer()

    fun write(value: String) {
        writer.write(value)
    }

    fun get(): String {
        return Base64.getEncoder().encodeToString(messageDigester.digest())
    }

    override fun close() {
        writer.close()
    }
}

/** Replace with [OutputStream.nullOutputStream] when the minimum JDK is 11+. */
private fun nullOutputStream(): OutputStream =
    object : OutputStream() {
        @Volatile
        private var closed = false

        override fun write(b: Int) {
            if (closed) {
                throw IOException("Stream closed")
            }
        }

        override fun close() {
            closed = true
        }
    }
