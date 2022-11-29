/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.ide

import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinDependency
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.getValue
import org.jetbrains.kotlin.tooling.core.setValue

fun interface IdeDependencyResolver {
    fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency>

    object Empty : IdeDependencyResolver {
        override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> = emptySet()
    }

    companion object {
        var IdeaKotlinDependency.resolvedBy: IdeDependencyResolver? by extrasKeyOf("resolvedBy")
        var IdeaKotlinDependency.gradleArtifact: ResolvedArtifactResult? by extrasKeyOf("gradleArtifact")
    }
}

fun IdeDependencyResolver(
    resolvers: Iterable<IdeDependencyResolver?>
): IdeDependencyResolver {
    val resolversList = resolvers.filterNotNull()
    if (resolversList.isEmpty()) return IdeDependencyResolver.Empty
    return IdeCompositeDependencyResolver(resolversList)
}

fun IdeDependencyResolver(
    vararg resolvers: IdeDependencyResolver?
): IdeDependencyResolver = IdeDependencyResolver(resolvers.toList())


operator fun IdeDependencyResolver.plus(other: IdeDependencyResolver): IdeDependencyResolver {
    if (this is IdeCompositeDependencyResolver && other is IdeCompositeDependencyResolver)
        return IdeCompositeDependencyResolver(this.children + other.children)

    if (this is IdeCompositeDependencyResolver) {
        return IdeCompositeDependencyResolver(this.children + other)
    }

    if (other is IdeCompositeDependencyResolver) {
        return IdeCompositeDependencyResolver(listOf(this) + other.children)
    }
    return IdeCompositeDependencyResolver(listOf(this, other))
}

private class IdeCompositeDependencyResolver(
    val children: List<IdeDependencyResolver>
) : IdeDependencyResolver {
    override fun resolve(sourceSet: KotlinSourceSet): Set<IdeaKotlinDependency> {
        return children.flatMap { child -> child.resolve(sourceSet) }.toSet()
    }
}
