package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.artifacts.dsl.Dependencies
import org.gradle.api.artifacts.dsl.DependencyCollector
import org.gradle.api.plugins.jvm.PlatformDependencyModifiers
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

/**
 * Dependency container for different scopes that Kotlin projects can have.
 *
 * @since 2.2.20
 */

@ExperimentalKotlinGradlePluginApi
interface KotlinDependencies : Dependencies, PlatformDependencyModifiers {
    /**
     * Dependencies for the implementation scope.
     */
    val implementation: DependencyCollector

    /**
     * Dependencies for the api scope.
     */
    val api: DependencyCollector

    /**
     * Dependencies for the compileOnly scope.
     */
    val compileOnly: DependencyCollector

    /**
     * Dependencies for the runtimeOnly scope.
     */
    val runtimeOnly: DependencyCollector

    /**
     * Test dependencies for the implementation scope.
     */
    val testImplementation: DependencyCollector

    /**
     * Test dependencies for the compileOnly scope.
     */
    val testCompileOnly: DependencyCollector

    /**
     * Test dependencies for the runtimeOnly scope.
     */
    val testRuntimeOnly: DependencyCollector
}