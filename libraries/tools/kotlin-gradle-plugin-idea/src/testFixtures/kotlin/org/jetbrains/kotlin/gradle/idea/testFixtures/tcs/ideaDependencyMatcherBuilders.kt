/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.tcs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency

fun buildIdeaKotlinDependencyMatchers(notation: Any?): List<IdeaKotlinDependencyMatcher> {
    return when (notation) {
        null -> return emptyList()
        is IdeaKotlinDependency -> listOf(IdeaKotlinDependencyInstanceMatcher(notation))
        is IdeaKotlinBinaryCoordinates -> listOf(IdeaBinaryCoordinatesInstanceMatcher(notation))
        is IdeaKotlinDependencyMatcher -> listOf(notation)
        is Iterable<*> -> notation.flatMap { buildIdeaKotlinDependencyMatchers(it) }
        else -> error("Can't build ${IdeaKotlinSourceDependencyMatcher::class.java.simpleName} from $notation")
    }
}

fun ideSourceDependency(type: IdeaKotlinSourceDependency.Type, project: Project, sourceSetName: String): IdeaKotlinDependencyMatcher {
    return IdeaKotlinSourceDependencyMatcher(type, project.path, sourceSetName)
}

fun ideSourceDependency(type: IdeaKotlinSourceDependency.Type, path: String): IdeaKotlinDependencyMatcher {
    val segments = path.split("/")
    val projectPath = segments.dropLast(1).joinToString("/")
    val sourceSetName = segments.last()
    return IdeaKotlinSourceDependencyMatcher(type, projectPath, sourceSetName)
}

fun regularSourceDependency(path: String) = ideSourceDependency(IdeaKotlinSourceDependency.Type.Regular, path)

fun friendSourceDependency(path: String) = ideSourceDependency(IdeaKotlinSourceDependency.Type.Friend, path)

fun dependsOnDependency(project: Project, sourceSetName: String) =
    ideSourceDependency(IdeaKotlinSourceDependency.Type.DependsOn, project, sourceSetName)

fun dependsOnDependency(path: String) = ideSourceDependency(IdeaKotlinSourceDependency.Type.DependsOn, path)

fun projectArtifactDependency(
    type: IdeaKotlinSourceDependency.Type = IdeaKotlinSourceDependency.Type.Regular, projectPath: String, artifactFilePath: FilePathRegex
): IdeaKotlinDependencyMatcher = IdeaKotlinProjectArtifactDependencyMatcher(
    type = type,
    projectPath = projectPath,
    artifactFilePath = artifactFilePath
)

fun binaryCoordinates(regex: Regex): IdeaKotlinDependencyMatcher {
    return IdeaBinaryCoordinatesMatcher(regex)
}

fun binaryCoordinates(literal: String): IdeaKotlinDependencyMatcher {
    return binaryCoordinates(Regex.fromLiteral(literal))
}

fun anyDependency(): IdeaKotlinDependencyMatcher = object : IdeaKotlinDependencyMatcher {
    override val description: String get() = "any"
    override fun matches(dependency: IdeaKotlinDependency): Boolean = true
}