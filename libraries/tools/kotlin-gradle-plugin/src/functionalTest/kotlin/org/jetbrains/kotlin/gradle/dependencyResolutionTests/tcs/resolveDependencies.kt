package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport

fun Project.resolveDependencies(sourceSetName: String): Iterable<IdeaKotlinDependency> {
    return kotlinIdeMultiplatformImport
        .resolveDependencies(multiplatformExtension.sourceSets.getByName(sourceSetName))
}

/**
 * Only dependsOn and project-2-project dependencies
 */
fun Project.resolveProjectDependencies(sourceSetName: String): Iterable<IdeaKotlinDependency> =
    resolveDependencies(sourceSetName).filter { it !is IdeaKotlinBinaryDependency }
