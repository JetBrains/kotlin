/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DuplicatedCode")

package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.*
import kotlin.test.fail

fun IdeaKpmProject.assertIsNotEmpty(): IdeaKpmProject = apply {
    if (this.modules.isEmpty()) fail("Expected at least one module in model")
}

fun IdeaKpmProject.assertContainsModule(name: String): IdeaKpmModule {
    return modules.find { it.name == name }
        ?: fail("Missing module with name '$name'. Found: ${modules.map { it.name }}")
}

fun IdeaKpmModule.assertContainsFragment(name: String): IdeaKpmFragment {
    return fragments.find { it.name == name }
        ?: fail("Missing fragment with name '$name'. Found: ${fragments.map { it.name }}")
}

fun IdeaKpmFragment.assertResolvedBinaryDependencies(
    binaryType: String,
    matchers: Set<TestIdeaKpmBinaryDependencyMatcher>
): Set<IdeaKpmResolvedBinaryDependency> {
    val resolvedBinaryDependencies = dependencies
        .mapNotNull { dependency ->
            when (dependency) {
                is IdeaKpmResolvedBinaryDependencyImpl -> dependency
                is IdeaKpmUnresolvedBinaryDependencyImpl -> fail("Unexpected unresolved dependency: $dependency")
                is IdeaKpmFragmentDependencyImpl -> null
            }
        }
        .filter { it.binaryType == binaryType }
        .toSet()

    val unexpectedResolvedBinaryDependencies = resolvedBinaryDependencies
        .filter { dependency -> matchers.none { matcher -> matcher.matches(dependency) } }

    val missingDependencies = matchers.filter { matcher ->
        resolvedBinaryDependencies.none { dependency -> matcher.matches(dependency) }
    }

    if (unexpectedResolvedBinaryDependencies.isEmpty() && missingDependencies.isEmpty()) {
        return resolvedBinaryDependencies
    }

    fail(
        buildString {
            if (unexpectedResolvedBinaryDependencies.isNotEmpty()) {
                appendLine("${name}: Unexpected dependencies found:")
                unexpectedResolvedBinaryDependencies.forEach { unexpectedDependency ->
                    appendLine(unexpectedDependency)
                }

                appendLine()
                appendLine("${name}: Unexpected dependency coordinates:")
                unexpectedResolvedBinaryDependencies.forEach { unexpectedDependency ->
                    appendLine("\"${unexpectedDependency.coordinates}\",")
                }
            }

            if (missingDependencies.isNotEmpty()) {
                appendLine()
                appendLine("${name}: Missing dependencies:")
                missingDependencies.forEach { missingDependency ->
                    appendLine(missingDependency.description)
                }
            }

            appendLine()
            appendLine("${name}: Resolved Dependency Coordinates:")
            resolvedBinaryDependencies.mapNotNull { it.coordinates }.forEach { coordinates ->
                appendLine("\"$coordinates\",")
            }
        }
    )
}

@JvmName("assertResolvedBinaryDependenciesByAnyMatcher")
fun IdeaKpmFragment.assertResolvedBinaryDependencies(
    binaryType: String, matchers: Set<Any?>,
) = assertResolvedBinaryDependencies(binaryType, matchers.flatMap { buildIdeaKpmBinaryDependencyMatchers(it) }.toSet())

fun IdeaKpmFragment.assertResolvedBinaryDependencies(
    binaryType: String, vararg matchers: Any?
) = assertResolvedBinaryDependencies(binaryType, matchers.toSet())

fun IdeaKpmFragment.assertFragmentDependencies(matchers: Set<TestIdeaKpmFragmentDependencyMatcher>): Set<IdeaKpmFragmentDependency> {
    val sourceDependencies = dependencies.filterIsInstance<IdeaKpmFragmentDependency>().toSet()

    val unexpectedDependencies = sourceDependencies
        .filter { dependency -> matchers.none { matcher -> matcher.matches(dependency) } }

    val missingDependencies = matchers.filter { matcher ->
        sourceDependencies.none { dependency -> matcher.matches(dependency) }
    }

    if (unexpectedDependencies.isEmpty() && missingDependencies.isEmpty()) {
        return sourceDependencies
    }

    fail(
        buildString {
            if (unexpectedDependencies.isNotEmpty()) {
                appendLine()
                appendLine("${coordinates.path}: Unexpected source dependency found:")
                unexpectedDependencies.forEach { unexpectedDependency ->
                    appendLine("\"${unexpectedDependency}\",")
                }
            }

            if (missingDependencies.isNotEmpty()) {
                appendLine()
                appendLine("${coordinates.path}: Missing fragment dependencies:")
                missingDependencies.forEach { missingDependency ->
                    appendLine(missingDependency.description)
                }
            }

            appendLine()
            appendLine("${coordinates.path}: Resolved source dependency paths:")
            sourceDependencies.forEach { dependency ->
                appendLine("\"${dependency}\",")
            }
        }
    )
}

@JvmName("assertSourceDependenciesByAnyMatcher")
fun IdeaKpmFragment.assertFragmentDependencies(matchers: Set<Any?>): Set<IdeaKpmFragmentDependency> =
    assertFragmentDependencies(matchers.flatMap { buildIdeaKpmFragmentDependencyMatchers(it) }.toSet())

fun IdeaKpmFragment.assertFragmentDependencies(vararg matchers: Any?) =
    assertFragmentDependencies(matchers.toSet())
