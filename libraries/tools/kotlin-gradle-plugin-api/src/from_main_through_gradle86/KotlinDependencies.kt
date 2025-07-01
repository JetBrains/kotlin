@file:Suppress("DEPRECATION")

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.Dependencies

/**
 * Keep in sync with [org.jetbrains.kotlin.gradle.plugin.mpp.MinSupportedGradleVersionWithDependencyCollectorsConst]
 */
internal const val MinSupportedGradleVersionWithDependencyCollectorsConst = "8.8"
private const val TopLevelDependenciesBackwardsDeprecation = "Kotlin top-level dependencies is not available in your Gradle version. " +
        "Minimum supported version is Gradle $MinSupportedGradleVersionWithDependencyCollectorsConst.\n" +
        "Please upgrade your Gradle version or keep using source set dependencies block: https://kotl.in/kmp-top-level-dependencies"

/**
 * You will see this type if you are compiling against Kotlin Gradle Plugin
 * less than [MinSupportedGradleVersionWithDependencyCollectorsConst]
 */
@Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
interface KotlinBackwardsDeploymentDependencyCollector {
    /**
     * DependencyCollector backwards compatibility
     */
    operator fun invoke(p: Any)
    /**
     * DependencyCollector backwards compatibility
     */
    operator fun invoke(p: Any, a: org.gradle.api.Action<in org.gradle.api.artifacts.ExternalModuleDependency>)
}

/**
 * You will see this type if you are compiling against Kotlin Gradle Plugin
 * less than [MinSupportedGradleVersionWithDependencyCollectorsConst]
 *
 * @suppress Dokka sees duplicate type from common
 */
@Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
interface KotlinDependencies : Dependencies {
    /**
     * Dependencies for the implementation scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
    val implementation: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Dependencies for the api scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
    val api: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Dependencies for the compileOnly scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
    val compileOnly: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Dependencies for the runtimeOnly scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
    val runtimeOnly: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Test dependencies for the implementation scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
    val testImplementation: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Test dependencies for the compileOnly scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
    val testCompileOnly: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Test dependencies for the runtimeOnly scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
    val testRuntimeOnly: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Platform function backwards compatibility
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
    fun platform(p: Any)

    /**
     * Enforced platform function backwards compatibility
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
    fun enforcedPlatform(p: Any)

    /**
     * Kotlin function backwards compatibility
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
    fun kotlin(module: String): Dependency

    /**
     * Kotlin function backwards compatibility
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.WARNING)
    fun kotlin(module: String, version: String?): Dependency
}
