/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.ide.testFixtures

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import kotlin.test.fail

fun buildIdeDependencyMatchers(notation: Any?): List<IdeDependencyMatcher> {
    return when (notation) {
        null -> return emptyList()
        is IdeDependencyMatcher -> listOf(notation)
        is Iterable<*> -> notation.flatMap { buildIdeDependencyMatchers(it) }
        else -> error("Can't build ${IdeSourceDependencyMatcher::class.java.simpleName} from $notation")
    }
}

fun ideSourceDependency(type: IdeaKotlinSourceDependency.Type, project: Project, sourceSetName: String): IdeDependencyMatcher {
    return IdeSourceDependencyMatcher(type, project.path, sourceSetName)
}

fun dependsOnDependency(project: Project, sourceSetName: String) =
    ideSourceDependency(IdeaKotlinSourceDependency.Type.DependsOn, project, sourceSetName)

interface IdeDependencyMatcher {
    val description: String
    fun matches(dependency: IdeaKotlinDependency): Boolean
}

private class IdeSourceDependencyMatcher(
    val type: IdeaKotlinSourceDependency.Type,
    val projectPath: String,
    val sourceSetName: String
) : IdeDependencyMatcher {
    override val description: String =
        "source($type)::$projectPath/$sourceSetName"

    override fun matches(dependency: IdeaKotlinDependency): Boolean {
        if (dependency !is IdeaKotlinSourceDependency) return false
        return dependency.type == type &&
                dependency.coordinates.projectPath == projectPath &&
                dependency.coordinates.sourceSetName == sourceSetName
    }
}

fun Iterable<IdeaKotlinDependency>.assertMatches(vararg notation: Any?) {
    val thisList = toList()
    val matchers = notation.flatMap { buildIdeDependencyMatchers(it) }

    val unexpectedDependencies = thisList.filter { dependency -> matchers.none { matcher -> matcher.matches(dependency) } }
    val missingDependencies = matchers.filter { matcher -> thisList.none { dependency -> matcher.matches(dependency) } }

    if (unexpectedDependencies.isEmpty() && missingDependencies.isEmpty()) {
        return
    }

    fail(
        buildString {
            if (unexpectedDependencies.isNotEmpty()) {
                appendLine()
                appendLine("Unexpected dependency found:")
                unexpectedDependencies.forEach { unexpectedDependency ->
                    appendLine("\"${unexpectedDependency}\",")
                }
            }

            if (missingDependencies.isNotEmpty()) {
                appendLine()
                appendLine("Missing dependencies:")
                missingDependencies.forEach { missingDependency ->
                    appendLine(missingDependency.description)
                }
            }

            appendLine()
            appendLine("Dependencies:")
            thisList.forEach { dependency ->
                appendLine("\"${dependency}\",")
            }
        }
    )
}
