package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKpmFragmentDependency

fun buildIdeaKpmFragmentDependencyMatchers(notation: Any?): List<TestIdeaKpmFragmentDependencyMatcher> {
    return when (notation) {
        null -> emptyList()
        is Iterable<*> -> notation.flatMap { buildIdeaKpmFragmentDependencyMatchers(it) }
        is String -> listOf(TestIdeaKpmFragmentDependencyMatcher.DependencyLiteral(notation))
        is Regex -> listOf(TestIdeaKpmFragmentDependencyMatcher.DependencyRegex(notation))
        else -> error("Can't build ${TestIdeaKpmFragmentDependencyMatcher::class.simpleName} from $notation")
    }
}

interface TestIdeaKpmFragmentDependencyMatcher : TestIdeaKpmDependencyMatcher<IdeaKpmFragmentDependency> {
    class DependencyLiteral(private val dependencyLiteral: String) : TestIdeaKpmFragmentDependencyMatcher {
        override val description: String = dependencyLiteral

        override fun matches(dependency: IdeaKpmFragmentDependency): Boolean {
            return this.dependencyLiteral == dependency.toString()
        }
    }

    class DependencyRegex(private val dependencyRegex: Regex) : TestIdeaKpmFragmentDependencyMatcher {
        override val description: String = dependencyRegex.pattern

        override fun matches(dependency: IdeaKpmFragmentDependency): Boolean {
            return dependencyRegex.matches(dependency.coordinates.toString())
        }
    }
}
