package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinSourceDependency

fun buildIdeaKotlinSourceDependencyMatchers(notation: Any?): List<IdeaKotlinSourceDependencyMatcher> {
    return when (notation) {
        null -> emptyList()
        is Iterable<*> -> notation.flatMap { buildIdeaKotlinSourceDependencyMatchers(it) }
        is String -> listOf(IdeaKotlinSourceDependencyMatcher.FragmentPath(notation))
        is Regex -> listOf(IdeaKotlinSourceDependencyMatcher.FragmentPathRegex(notation))
        else -> error("Can't build ${IdeaKotlinSourceDependencyMatcher::class.simpleName} from $notation")
    }
}

interface IdeaKotlinSourceDependencyMatcher : IdeaKotlinDependencyMatcher<IdeaKotlinSourceDependency> {
    class FragmentPath(private val dependency: String) : IdeaKotlinSourceDependencyMatcher {
        override val description: String = dependency

        override fun matches(dependency: IdeaKotlinSourceDependency): Boolean {
            return this.dependency == dependency.toString()
        }
    }

    class FragmentPathRegex(private val dependencyRegex: Regex) : IdeaKotlinSourceDependencyMatcher {
        override val description: String = dependencyRegex.pattern

        override fun matches(dependency: IdeaKotlinSourceDependency): Boolean {
            return dependencyRegex.matches(dependency.toString())
        }
    }
}
