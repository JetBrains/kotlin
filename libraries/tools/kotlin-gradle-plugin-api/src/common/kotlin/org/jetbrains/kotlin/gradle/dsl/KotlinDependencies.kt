package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.plugins.jvm.PlatformDependencyModifiers
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

/**
 * Dependency container for different scopes that Kotlin projects can have.
 *
 * @since 2.2.20
 */

@KotlinGradlePluginDsl
@ExperimentalKotlinGradlePluginApi
interface KotlinDependencies : Dependencies, PlatformDependencyModifiers {
    /**
     * Add a dependency to the [commonMain][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME] source set's
     * [implementation][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.implementationConfigurationName] configuration.
     *
     * [`implementation`](https://kotlinlang.org/docs/gradle-configure-project.html#dependency-types)-scoped dependencies apply during
     * compilation and runtime. Gradle doesn't export them to the compilations of library consumers, but it does include their runtime components.
     */
    val implementation: DependencyCollector

    /**
     * Add a dependency to the [commonMain][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME] source set's
     * [api][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.apiConfigurationName] configuration
     *
     * [`api`](https://kotlinlang.org/docs/gradle-configure-project.html#dependency-types)-scoped dependencies apply during
     * compilation and runtime. Gradle exports them to both the compilation and runtime of library consumers.
     */
    val api: DependencyCollector

    /**
     * Add a dependency to the [commonMain][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME] source set's
     * [compileOnly][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.compileOnlyConfigurationName] configuration.
     *
     * [`compileOnly`](https://kotlinlang.org/docs/gradle-configure-project.html#dependency-types)-scoped dependencies apply only during
     * compilation. Gradle doesn't include them at runtime or export them to the compilations or runtime of library consumers.
     */
    val compileOnly: DependencyCollector

    /**
     * Add a dependency to the [commonMain][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME] source set's
     * [runtimeOnly][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.runtimeOnlyConfigurationName] configuration.
     *
     * [`runtimeOnly`](https://kotlinlang.org/docs/gradle-configure-project.html#dependency-types)-scoped dependencies apply only at
     * runtime and aren't visible during compilation. Gradle includes them in the runtime of library consumers but doesn't export them for compilation.
     */
    val runtimeOnly: DependencyCollector

    /**
     * Add a dependency to the [commonTest][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME] source set's
     * [implementation][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.implementationConfigurationName] configuration.
     *
     * @see implementation
     */
    val testImplementation: DependencyCollector

    /**
     * Add a dependency to the [commonTest][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME] source sets
     * [compileOnly][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.compileOnlyConfigurationName] configuration.
     *
     * @see compileOnly
     */
    val testCompileOnly: DependencyCollector

    /**
     * Add a dependency to the [commonTest][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME] source set's
     * [runtimeOnly][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.runtimeOnlyConfigurationName] configuration.
     *
     * @see runtimeOnly
     */
    val testRuntimeOnly: DependencyCollector

    /**
     * Generates a "org.jetbrains.kotlin:kotlin-${module}" dependency.
     */
    fun kotlin(module: String): Dependency

    /**
     * Generates a "org.jetbrains.kotlin:kotlin-${module}:${version}" dependency.
     */
    fun kotlin(module: String, version: String?): Dependency
}
