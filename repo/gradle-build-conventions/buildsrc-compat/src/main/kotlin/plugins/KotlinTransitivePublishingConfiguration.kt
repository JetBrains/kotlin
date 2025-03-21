/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package plugins

import gradle.GradlePluginVariant
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Task
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.sources.AbstractKotlinSourceSet

/**
 * Configures the `install` task to depend on the `install` task of all project dependencies declared in non-test source sets.
 * Using this, we can ensure that all transitive dependencies required at runtime of a published project are also published.
 * The goal of this function is to configure the `install` task so that granular publication is possible without the need for publishing the entire repository.
 *
 * Dependencies required at runtime are declared in the `api`, `implementation`, and `runtimeOnly` scopes.
 *
 * Additionally, this verifies that we do not declare runtime dependencies in published projects on other projects that are not published.
 * If that is the case, the dependency on :<project-path>:install will cause a build failure indicating this problem.
 *
 * An exception to autoconfiguration is implicit dependencies that could not be inferred automatically from the build configuration, such as KGP's dependency on kotlin-stdlib
 * which KGP adds at Gradle runtime and expects it to be available in the repository.
 * Such dependencies should be added explicitly, like
 * ```kotlin
 * tasks.named("install") {
 *   dependsOn(":kotlin-stdlib:install")
 * }
 * ```
 */
internal fun Task.configureInstallTaskToBeTransitive() {
    // There's no reliable way to configure this from a custom project's extension without a big refactoring of our publishing-related logic
    // because the logic that configures the `install` task is called from multiple different places without specific ordering guarantees.
    // I've decided to hardcode the source set/compilation names here to make it easier to refactor it together with the publishing logic
    // instead of adding more complex and fragile logic here.
    // Particularly, `knownNonTestSourceSets` serves for verification that `install` does not cause irrelevant projects to be published.
    val knownNonTestSourceSets = buildSet {
        add("common")
        add("commonMain")
        add("jsMain")
        add("jvmMain")
        add("wasmCommonMain")
        add("wasmJsMain")
        add("wasmWasiMain")
        add("main")
        for (gradlePluginVariant in GradlePluginVariant.values()) {
            add(gradlePluginVariant.sourceSetName)
        }
        if (project.path == ":kotlin-stdlib") {
            add("jvmJava9")
            add("jvmMainJdk7")
            add("jvmMainJdk8")
            add("nativeWasmMain")
            add("jvmCompileOnlyDeclarations")
        }
        if (project.path in setOf(":kotlin-stdlib", ":kotlin-reflect", ":kotlin-stdlib-jdk7", ":kotlin-stdlib-jdk8")) {
            add("java9")
        }
        if (project.path == ":kotlin-test") {
            add("annotationsCommonMain")
            add("assertionsCommonMain")
            add("jvmJUnit")
            add("jvmJUnit5")
            add("jvmTestNG")
        }
    }
    val knownTestCompilationNames = buildSet {
        add(KotlinCompilation.TEST_COMPILATION_NAME)
        add(org.gradle.internal.component.external.model.TestFixturesSupport.TEST_FIXTURE_SOURCESET_NAME)
        add("functionalTest")
        if (project.path == ":kotlin-stdlib-jdk8") {
            add("moduleTest")
        }
        if (project.path == ":kotlin-stdlib") {
            add("longRunningTest")
            add("recursiveDeletionTest")
        }
        if (project.path == ":kotlin-test") {
            add("JUnit5Test")
            add("TestNGTest")
            add("JUnitTest")
        }
    }

    project.pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
        val kotlinExtension = project.extensions.getByType<org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension>()
        configureDependsOnKotlinSourceSetsRuntimeDependencies(kotlinExtension.sourceSets, knownNonTestSourceSets, knownTestCompilationNames)
    }

    project.pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        val kotlinExtension = project.extensions.getByType<KotlinJvmProjectExtension>()
        configureDependsOnKotlinSourceSetsRuntimeDependencies(kotlinExtension.sourceSets, knownNonTestSourceSets, knownTestCompilationNames)
    }

    project.pluginManager.withPlugin("java") {
        project.extensions.getByType<JavaPluginExtension>().sourceSets.configureEach {
            val sourceSet = this
            if (!SourceSet.isMain(sourceSet)) return@configureEach
            configureDependsOnRuntimeDependenciesInstall(
                setOf(
                    sourceSet.implementationConfigurationName,
                    sourceSet.runtimeOnlyConfigurationName,
                )
            )
        }
    }

    project.pluginManager.withPlugin("java-library") {
        project.extensions.getByType<JavaPluginExtension>().sourceSets.configureEach {
            val sourceSet = this
            if (!SourceSet.isMain(sourceSet)) return@configureEach
            configureDependsOnRuntimeDependenciesInstall(
                setOf(
                    sourceSet.implementationConfigurationName,
                    sourceSet.apiConfigurationName,
                    sourceSet.runtimeOnlyConfigurationName,
                )
            )
        }
    }
}

/**
 * Traverses all the dependencies of all non-test Kotlin source sets
 * and adds a dependency on the `install` task of each project that we depend on at runtime.
 *
 * Dependencies required at runtime are declared in the `api`, `implementation`, and `runtimeOnly` scopes.
 */
private fun Task.configureDependsOnKotlinSourceSetsRuntimeDependencies(
    sourceSets: NamedDomainObjectContainer<KotlinSourceSet>,
    knownNonTestSourceSets: Set<String>,
    testCompilationNames: Set<String>,
) {
    sourceSets.configureEach sourceSet@{
        val sourceSet = this as AbstractKotlinSourceSet
        val kotlinCompilations = sourceSet.compilations
        // If there's any non-test compilation that includes this source set, we consider dependencies of this source set as required at published runtime.
        // This comes from a generic case of declared dependency on intermediate source sets that could declare dependencies, and we cannot decide
        // if the source set is a part of the published API.
        val isTestOnly = kotlinCompilations.all { it.name in testCompilationNames }
        if (isTestOnly) {
            return@sourceSet
        }
        require(knownNonTestSourceSets.contains(sourceSet.name)) {
            val applicableCompilations = kotlinCompilations.filter { !testCompilationNames.contains(it.name) }
            """
            Source set ${sourceSet.name} isn't explicitly known as non-test. Please either add it to `knownNonTestSourceSets` or mark irrelevant compilations as test via `knownTestCompilationNames`.
            The following compilations are detected as non-test and related to this source set: ${applicableCompilations.map { it.name }}
            """.trimIndent()
        }
        val configurationsAddingRuntimeDeps =
            setOf(implementationConfigurationName, apiConfigurationName, runtimeOnlyConfigurationName)
        configureDependsOnRuntimeDependenciesInstall(configurationsAddingRuntimeDeps)
    }
}

private fun Task.configureDependsOnRuntimeDependenciesInstall(configurationsAddingRuntimeDeps: Set<String>) {
    val installTask = this
    configurationsAddingRuntimeDeps.forEach { configurationName ->
        project.configurations.named(configurationName) {
            require(isCanBeDeclared) {
                "Configuration $name is expected to be declarable."
            }
            dependencies.withType<ProjectDependency> {
                // It would be nice to fail with a sound message here if there's no such task, but it's not possible because'
                // 1. It would violate Gradle Isolated Project limitations in the future.
                // 2. We don't know if the dependency project was already configured to have the `install` task, or it is added later.
                installTask.dependsOn("${path}:install")
            }
        }
    }
}