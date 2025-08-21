package org.jetbrains.kotlin.gradle.idea.testFixtures.utils

import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinUnresolvedBinaryDependency
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.IdeaKotlinDependencyMatcher

fun unresolvedDependenciesDiagnosticMatcher(dependencyName: String) = IdeaKotlinDependencyMatcher("Unresolved dependency: ${dependencyName}") { dependency ->
    dependency is IdeaKotlinUnresolvedBinaryDependency && Regex("Couldn't resolve dependency '${dependencyName}' in '.+?' for all target platforms").containsMatchIn(dependency.cause.orEmpty())
}