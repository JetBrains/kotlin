package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmFragmentDependency

fun buildIdeaKpmFragmentDependencyMatchers(notation: Any?): List<IdeaKpmFragmentDependencyMatcher> {
    return when (notation) {
        null -> emptyList()
        is Iterable<*> -> notation.flatMap { buildIdeaKpmFragmentDependencyMatchers(it) }
        is String -> listOf(IdeaKpmFragmentDependencyMatcher.DependencyLiteral(notation))
        is Regex -> listOf(IdeaKpmFragmentDependencyMatcher.DependencyRegex(notation))
        else -> error("Can't build ${IdeaKpmFragmentDependencyMatcher::class.simpleName} from $notation")
    }
}

interface IdeaKpmFragmentDependencyMatcher : IdeaKpmDependencyMatcher<IdeaKpmFragmentDependency> {
    class DependencyLiteral(private val dependencyLiteral: String) : IdeaKpmFragmentDependencyMatcher {
        override val description: String = dependencyLiteral

        override fun matches(dependency: IdeaKpmFragmentDependency): Boolean {
            return this.dependencyLiteral == dependency.toString()
        }
    }

    class DependencyRegex(private val dependencyRegex: Regex) : IdeaKpmFragmentDependencyMatcher {
        override val description: String = dependencyRegex.pattern

        override fun matches(dependency: IdeaKpmFragmentDependency): Boolean {
            return dependencyRegex.matches(dependency.coordinates.toString())
        }
    }
}
