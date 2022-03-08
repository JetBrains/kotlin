package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.IdeaKotlinSourceDependency
import org.jetbrains.kotlin.gradle.kpm.idea.path

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
    class FragmentPath(private val path: String) : IdeaKotlinSourceDependencyMatcher {
        override val description: String = path

        override fun matches(dependency: IdeaKotlinSourceDependency): Boolean {
            return path == dependency.path
        }
    }

    class FragmentPathRegex(private val pathRegex: Regex) : IdeaKotlinSourceDependencyMatcher {
        override val description: String = pathRegex.pattern

        override fun matches(dependency: IdeaKotlinSourceDependency): Boolean {
            return pathRegex.matches(dependency.path)
        }
    }
}
