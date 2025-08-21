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
     * Used to add dependency to [commonMain][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME]
     * [implementation][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.implementationConfigurationName] configuration
     *
     * [`implementation`](https://kotlinlang.org/docs/gradle-configure-project.html#dependency-types) scoped dependencies are used during
     * compilation and at runtime. They are not exported to compilations of library consumers, but are exported for their runtime.
     */
    val implementation: DependencyCollector

    /**
     * Used to add dependency to [commonMain][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME]
     * [api][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.apiConfigurationName] configuration
     *
     * [`api`](https://kotlinlang.org/docs/gradle-configure-project.html#dependency-types) scoped dependencies are used both during
     * compilation and at runtime. They are exported to compilations and runtime of library consumers.
     */
    val api: DependencyCollector

    /**
     * Used to add dependency to [commonMain][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME]
     * [compileOnly][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.compileOnlyConfigurationName] configuration
     *
     * [`compileOnly`](https://kotlinlang.org/docs/gradle-configure-project.html#dependency-types) scoped dependencies are used during
     * compilation and are not visible at runtime. They are not exported to compilations or runtime of library consumers.
     */
    val compileOnly: DependencyCollector

    /**
     * Used to add dependency to [commonMain][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME]
     * [runtimeOnly][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.runtimeOnlyConfigurationName] configuration
     *
     * [`runtimeOnly`](https://kotlinlang.org/docs/gradle-configure-project.html#dependency-types) scoped dependencies are used only at
     * runtime and not visible during compilation. They are exported to runtime of library consumers.
     */
    val runtimeOnly: DependencyCollector

    /**
     * Used to add dependency to [commonTest][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME]
     * [implementation][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.implementationConfigurationName] configuration
     *
     * @see implementation
     */
    val testImplementation: DependencyCollector

    /**
     * Used to add dependency to [commonTest][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME]
     * [compileOnly][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.compileOnlyConfigurationName] configuration
     *
     * @see compileOnly
     */
    val testCompileOnly: DependencyCollector

    /**
     * Used to add dependency to [commonTest][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.COMMON_TEST_SOURCE_SET_NAME]
     * [runtimeOnly][org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.runtimeOnlyConfigurationName] configuration
     *
     * @see runtimeOnly
     */
    val testRuntimeOnly: DependencyCollector

    /**
     * Convenience for generating "org.jetbrains.kotlin:kotlin-${module}" dependency
     */
    fun kotlin(module: String): Dependency

    /**
     * Convenience for generating "org.jetbrains.kotlin:kotlin-${module}:${version}" dependency
     */
    fun kotlin(module: String, version: String?): Dependency
}