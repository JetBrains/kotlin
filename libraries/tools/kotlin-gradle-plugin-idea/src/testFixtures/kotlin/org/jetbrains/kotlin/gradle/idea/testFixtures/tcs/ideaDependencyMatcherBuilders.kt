/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.tcs

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency

fun buildIdeaKotlinDependencyMatchers(notation: Any?): List<IdeaKotlinDependencyMatcher> {
    return when (notation) {
        null -> return emptyList()
        is IdeaKotlinDependencyMatcher -> listOf(notation)
        is Iterable<*> -> notation.flatMap { buildIdeaKotlinDependencyMatchers(it) }
        else -> error("Can't build ${IdeaKotlinSourceDependencyMatcher::class.java.simpleName} from $notation")
    }
}

fun ideSourceDependency(type: IdeaKotlinSourceDependency.Type, project: Project, sourceSetName: String): IdeaKotlinDependencyMatcher {
    return IdeaKotlinSourceDependencyMatcher(type, project.path, sourceSetName)
}

fun dependsOnDependency(project: Project, sourceSetName: String) =
    ideSourceDependency(IdeaKotlinSourceDependency.Type.DependsOn, project, sourceSetName)

fun binaryCoordinates(regex: Regex): IdeaKotlinDependencyMatcher {
    return IdeaBinaryCoordinatesMatcher(regex)
}

fun binaryCoordinates(literal: String): IdeaKotlinDependencyMatcher {
    return binaryCoordinates(Regex.fromLiteral(literal))
}