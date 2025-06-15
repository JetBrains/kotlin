@file:Suppress("DEPRECATION_ERROR")

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.artifacts.dsl.Dependencies
import org.jetbrains.kotlin.gradle.plugin.mpp.MinSupportedGradleVersionWithDependencyCollectorsString


private const val TopLevelDependenciesBackwardsDeprecation = "Kotlin top-level dependencies is not available in your Gradle version. " +
        "Minimum supported version is $MinSupportedGradleVersionWithDependencyCollectorsString.\n" +
        "Please upgrade your Gradle version or keep using source-set level dependencies block: https://kotl.in/kmp-top-level-dependencies"

/**
 * You will see this type if you are compiling against Kotlin Gradle Plugin
 * less than [MinSupportedGradleVersionWithDependencyCollectorsString]
 */
@Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.ERROR)
interface KotlinBackwardsDeploymentDependencyCollector {
    operator fun invoke(p: Any) {
        throw NotImplementedError()
    }
}

/**
 * You will see this type if you are compiling against Kotlin Gradle Plugin
 * less than [MinSupportedGradleVersionWithDependencyCollectorsString]
 */
@Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.ERROR)
interface KotlinDependencies : Dependencies {
    /**
     * Dependencies for the implementation scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.ERROR)
    val implementation: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Dependencies for the api scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.ERROR)
    val api: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Dependencies for the compileOnly scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.ERROR)
    val compileOnly: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Dependencies for the runtimeOnly scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.ERROR)
    val runtimeOnly: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Test dependencies for the implementation scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.ERROR)
    val testImplementation: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Test dependencies for the compileOnly scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.ERROR)
    val testCompileOnly: KotlinBackwardsDeploymentDependencyCollector

    /**
     * Test dependencies for the runtimeOnly scope.
     */
    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.ERROR)
    val testRuntimeOnly: KotlinBackwardsDeploymentDependencyCollector

    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.ERROR)
    fun platform(p: Any) {
        throw NotImplementedError()
    }

    @Deprecated(TopLevelDependenciesBackwardsDeprecation, level = DeprecationLevel.ERROR)
    fun enforcedPlatform(p: Any) {
        throw NotImplementedError()
    }
}