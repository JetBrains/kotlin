/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.GradleKpmFragment

fun interface IdeaKpmDependencyResolver {
    fun resolve(fragment: GradleKpmFragment): Set<IdeaKpmDependency>

    object Empty : IdeaKpmDependencyResolver {
        override fun resolve(fragment: GradleKpmFragment): Set<IdeaKpmDependency> = emptySet()
    }
}

fun IdeaKpmDependencyResolver(
    resolvers: Iterable<IdeaKpmDependencyResolver>
): IdeaKpmDependencyResolver {
    val resolversList = resolvers.toList()
    if (resolversList.isEmpty()) return IdeaKpmDependencyResolver.Empty
    return IdeaKpmCompositeDependencyResolver(resolversList)
}

fun IdeaKpmDependencyResolver(
    vararg resolvers: IdeaKpmDependencyResolver
): IdeaKpmDependencyResolver = IdeaKpmDependencyResolver(resolvers.toList())

operator fun IdeaKpmDependencyResolver.plus(
    other: IdeaKpmDependencyResolver
): IdeaKpmDependencyResolver {
    if (this is IdeaKpmCompositeDependencyResolver && other is IdeaKpmCompositeDependencyResolver) {
        return IdeaKpmCompositeDependencyResolver(this.children + other.children)
    }

    if (this is IdeaKpmCompositeDependencyResolver) {
        return IdeaKpmCompositeDependencyResolver(this.children + other)
    }

    if (other is IdeaKpmCompositeDependencyResolver) {
        return IdeaKpmCompositeDependencyResolver(listOf(this) + other.children)
    }

    return IdeaKpmCompositeDependencyResolver(listOf(this, other))
}

private class IdeaKpmCompositeDependencyResolver(
    val children: List<IdeaKpmDependencyResolver>
) : IdeaKpmDependencyResolver {
    override fun resolve(fragment: GradleKpmFragment): Set<IdeaKpmDependency> {
        return children.flatMap { child -> child.resolve(fragment) }.toSet()
    }
}
