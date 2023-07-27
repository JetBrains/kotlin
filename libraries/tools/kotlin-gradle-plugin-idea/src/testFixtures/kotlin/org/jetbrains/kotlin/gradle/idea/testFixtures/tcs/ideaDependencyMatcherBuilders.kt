/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.tcs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCoordinates
import org.gradle.api.artifacts.component.BuildIdentifier
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.build.BuildState
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
    return IdeaKotlinSourceDependencyMatcher(type, project.currentBuildId().buildPath, project.path, sourceSetName)
}

fun ideSourceDependency(type: IdeaKotlinSourceDependency.Type, path: String): IdeaKotlinDependencyMatcher {
    val segments = path.split("/")
    val buildAndProjectPath = segments.dropLast(1).joinToString(":").split("::", limit = 2)
    val buildId = if (buildAndProjectPath.size == 2) buildAndProjectPath.first() else ":"
    val projectPath = if (buildAndProjectPath.size == 2) ":" + buildAndProjectPath.last() else buildAndProjectPath.last()
    val sourceSetName = segments.last()
    return IdeaKotlinSourceDependencyMatcher(type, buildId, projectPath, sourceSetName)
}

fun regularSourceDependency(path: String) = ideSourceDependency(IdeaKotlinSourceDependency.Type.Regular, path)

fun friendSourceDependency(path: String) = ideSourceDependency(IdeaKotlinSourceDependency.Type.Friend, path)

fun dependsOnDependency(project: Project, sourceSetName: String) =
    ideSourceDependency(IdeaKotlinSourceDependency.Type.DependsOn, project, sourceSetName)

fun dependsOnDependency(path: String) = ideSourceDependency(IdeaKotlinSourceDependency.Type.DependsOn, path)

fun projectArtifactDependency(
    type: IdeaKotlinSourceDependency.Type = IdeaKotlinSourceDependency.Type.Regular,
    buildIdAndProjectPath: String, artifactFilePath: FilePathRegex
): IdeaKotlinDependencyMatcher {
    val slicedProjectPath = buildIdAndProjectPath.split("::", limit = 2)
    val buildPath = if (slicedProjectPath.size == 2) slicedProjectPath.first() else ":"
    val projectPath = if (slicedProjectPath.size == 2) ":" + slicedProjectPath.last() else slicedProjectPath.last()

    return IdeaKotlinProjectArtifactDependencyMatcher(
        type = type,
        buildPath = buildPath,
        projectPath = projectPath,
        artifactFilePath = artifactFilePath
    )
}

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

/* Duplicated: Aks Gradle for public API? */
private fun Project.currentBuildId(): BuildIdentifier =
    (project as ProjectInternal).services.get(BuildState::class.java).buildIdentifier