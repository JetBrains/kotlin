package org.jetbrains.kotlin.gradle.dependencyResolutionTests.tcs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.ide.kotlinIdeMultiplatformImport

fun Project.resolveDependencies(sourceSetName: String): Iterable<IdeaKotlinDependency> {
    return kotlinIdeMultiplatformImport
        .resolveDependencies(multiplatformExtension.sourceSets.getByName(sourceSetName))
        .filter { it !is IdeaKotlinBinaryDependency }
}
