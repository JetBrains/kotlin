/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.provider.Property
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.io.File
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
        val includedTestFixtures = LinkedHashSet<String>()

        val builder = object : ArtifactContentBuilder {
            override fun project(path: String, isTransitive: Boolean) {
                includedProjects.compute(path) { _, oldValue -> oldValue == true || isTransitive }
            }

            override fun testFixtures(path: String) {
                includedTestFixtures.add(path)
            }
        }

        block(builder)

        dependencies {
            val artifactContent = configurations.getByName("artifactContent")

            for ((projectPath, shouldBeTransitive) in includedProjects) {
                artifactContent(project(projectPath)) { isTransitive = shouldBeTransitive }
            }

            for (projectPath in includedTestFixtures) {
                artifactContent(testFixtures(project(projectPath)) as ModuleDependency) { isTransitive = false }
            }
        }

        sourcesJar {
            val artifactContentElements = configurations.getByName("artifactContentElements")

            // Build the included projects' artifacts so their code-generation tasks run and
            // the generated sources exist on disk for the published project source JARs.
            dependsOn(artifactContentElements)

            addEmbeddedProjectSourcesJars(artifactContentElements.name)
            addEmbeddedLibrarySources(artifactContentElements)
        }

        javadocJar()
    }

    /**
     * Registers the checks that the artifact is self-sufficient at runtime: ProGuard verifies that its JAR together with its
     * runtime classpath references no missing class ('validateClasspath', configured by [proguardConfiguration]), and no
     * entry is shipped by two JARs of that classpath ('validateNoDuplicateClasses', with the exceptions listed in
     * [permittedDuplicatesFile]). Both run as part of 'check'.
     */
    fun validateClasspath(proguardConfiguration: File, permittedDuplicatesFile: File): Unit = with(project) {
        val artifactClasspath = files(tasks.named(JavaPlugin.JAR_TASK_NAME), configurations.named(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME))

        val mergedClasspathJar = tasks.register<Jar>("mergedClasspathJar") {
            description = "Merges the artifact and its runtime classpath JARs into a single JAR for ProGuard validation"
            destinationDirectory.set(layout.buildDirectory.dir("proguard"))
            archiveFileName.set("merged-classpath.jar")
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE

            dependsOn(artifactClasspath)
            from({ artifactClasspath.files.map(::zipTree) })
        }

        val validateClasspath = tasks.register<CacheableProguardTask>("validateClasspath") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Validates that the artifact and its runtime classpath have all required classes"

            javaLauncher.set(getToolchainLauncherFor(JdkMajorVersion.JDK_11_0))

            configuration(proguardConfiguration)

            injars(files(mergedClasspathJar))
            outjars(layout.buildDirectory.file("proguard/output.jar"))

            libraryjars(files(javaLauncher.map { it.metadata.installationPath.asFile.resolve("jmods") }))
        }

        val validateNoDuplicateClasses = tasks.register<VerifyNoDuplicateClasspathEntriesTask>("validateNoDuplicateClasses") {
            classpath.from(artifactClasspath)
            this.permittedDuplicatesFile.set(permittedDuplicatesFile)
        }

        tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
            dependsOn(validateClasspath, validateNoDuplicateClasses)
        }
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

    /**
     * Include test fixtures of the project with the given [path] into the artifact.
     * Neither the production code of the project nor dependencies of its test fixtures are included.
     */
    fun testFixtures(path: String)

    /**
     * Include test fixtures of all projects with the given [paths] into the artifact.
     * Neither the production code of the projects nor dependencies of their test fixtures are included.
     */
    fun testFixtures(paths: Iterable<String>) {
        paths.forEach { testFixtures(it) }
    }
}
