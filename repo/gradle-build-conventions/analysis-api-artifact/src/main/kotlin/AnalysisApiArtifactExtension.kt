/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.provider.Property
import org.gradle.jvm.JvmLibrary
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.dependencies
import org.gradle.language.base.artifact.SourcesArtifact
import javax.inject.Inject

abstract class AnalysisApiArtifactExtension @Inject constructor(private val project: Project) {
    private var isPublishedProjectsConfigured = false

    /**
     * Whether the binaries ('.jar') artifact is expected to contain content. `true` by default.
     *
     * Set to `false` for umbrella artifacts that ship no binaries of their own (for instance, ones that
     * re-export their dependencies without declaring [content]).
     */
    abstract val expectNonEmptyBinaries: Property<Boolean>

    /**
     * Whether the sources ('-sources.jar') artifact is expected to contain content. `true` by default.
     *
     * Set to `false` for artifacts that intentionally ship no sources.
     */
    abstract val expectNonEmptySources: Property<Boolean>

    init {
        expectNonEmptyBinaries.convention(true)
        expectNonEmptySources.convention(true)
    }

    /**
     * Configures the artifact content.
     * Unlike the `embedded` configuration, [content] also adds sources of included projects into the `-sources.jar`.
     */
    fun content(block: ArtifactContentBuilder.() -> Unit): Unit = with(project) {
        require(!isPublishedProjectsConfigured) { ::content.name + " can be called only once" }
        isPublishedProjectsConfigured = true

        val includedProjects = LinkedHashMap<String, Boolean>()

        val builder = object : ArtifactContentBuilder {
            override fun project(path: String, isTransitive: Boolean) {
                includedProjects.compute(path) { _, oldValue -> oldValue == true || isTransitive }
            }
        }

        block(builder)

        dependencies {
            val artifactContent = configurations.getByName("artifactContent")

            for ((projectPath, shouldBeTransitive) in includedProjects) {
                artifactContent(project(projectPath)) { isTransitive = shouldBeTransitive }
            }
        }

        sourcesJar {
            val artifactContentElements = configurations.getByName("artifactContentElements")

            // Build the included projects' artifacts so their code-generation tasks run and
            // the generated sources exist on disk for 'addEmbeddedSources'.
            dependsOn(artifactContentElements)

            addEmbeddedSources(artifactContentElements.name)
            addEmbeddedLibrarySources(artifactContentElements)
        }

        javadocJar()
    }

    private fun Jar.addEmbeddedLibrarySources(configuration: Configuration) = with(project) {
        val allLibrarySources by lazy {
            val moduleComponentIds = configuration.incoming.resolutionResult.allComponents.map { it.id }

            dependencies.createArtifactResolutionQuery()
                .forComponents(moduleComponentIds)
                .withArtifacts(JvmLibrary::class.java, SourcesArtifact::class.java)
                .execute()
                .resolvedComponents
                .flatMap { it.getArtifacts(SourcesArtifact::class.java) }
                .filterIsInstance<ResolvedArtifactResult>()
                .map { zipTree(it.file) }
        }

        from({ allLibrarySources })
    }
}

interface ArtifactContentBuilder {
    /**
     * Include the project with the given [path] into the artifact.
     * If [isTransitive] is `true`, the project's dependencies are also included.
     */
    fun project(path: String, isTransitive: Boolean = false)

    /**
     * Include all projects with the given [paths] into the artifact.
     * If [isTransitive] is `true`, the project's dependencies are also included.
     */
    fun projects(paths: Iterable<String>, isTransitive: Boolean = false) {
        paths.forEach { project(it, isTransitive) }
    }

    /**
     * Include all projects with the given [paths] into the artifact.
     * If [isTransitive] is `true`, the project's dependencies are also included.
     */
    fun projects(paths: Array<String>, isTransitive: Boolean = false) {
        projects(paths.asList(), isTransitive)
    }
}
