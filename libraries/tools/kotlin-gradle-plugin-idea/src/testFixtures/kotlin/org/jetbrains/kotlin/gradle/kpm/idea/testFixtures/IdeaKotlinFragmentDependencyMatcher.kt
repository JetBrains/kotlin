package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinFragmentDependency

fun buildIdeaKotlinFragmentDependencyMatchers(notation: Any?): List<IdeaKotlinFragmentDependencyMatcher> {
    return when (notation) {
        null -> emptyList()
        is Iterable<*> -> notation.flatMap { buildIdeaKotlinFragmentDependencyMatchers(it) }
        is String -> listOf(IdeaKotlinFragmentDependencyMatcher.DependencyLiteral(notation))
        is Regex -> listOf(IdeaKotlinFragmentDependencyMatcher.DependencyRegex(notation))
        else -> error("Can't build ${IdeaKotlinFragmentDependencyMatcher::class.simpleName} from $notation")
    }
}

interface IdeaKotlinFragmentDependencyMatcher : IdeaKotlinDependencyMatcher<IdeaKotlinFragmentDependency> {
    class DependencyLiteral(private val dependencyLiteral: String) : IdeaKotlinFragmentDependencyMatcher {
        override val description: String = dependencyLiteral

        override fun matches(dependency: IdeaKotlinFragmentDependency): Boolean {
            return this.dependencyLiteral == dependency.toString()
        }
    }

    class DependencyRegex(private val dependencyRegex: Regex) : IdeaKotlinFragmentDependencyMatcher {
        override val description: String = dependencyRegex.pattern

        override fun matches(dependency: IdeaKotlinFragmentDependency): Boolean {
            return dependencyRegex.matches(dependency.coordinates.toString())
        }
    }
}
