/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleFragment

fun interface IdeaKotlinDependencyResolver {
    fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinDependency>

    object Empty : IdeaKotlinDependencyResolver {
        override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinDependency> = emptySet()
    }
}

fun IdeaKotlinDependencyResolver(
    resolvers: Iterable<IdeaKotlinDependencyResolver>
): IdeaKotlinDependencyResolver {
    val resolversList = resolvers.toList()
    if (resolversList.isEmpty()) return IdeaKotlinDependencyResolver.Empty
    return CompositeIdeaKotlinDependencyResolver(resolversList)
}

fun IdeaKotlinDependencyResolver(
    vararg resolvers: IdeaKotlinDependencyResolver
): IdeaKotlinDependencyResolver = IdeaKotlinDependencyResolver(resolvers.toList())

operator fun IdeaKotlinDependencyResolver.plus(
    other: IdeaKotlinDependencyResolver
): IdeaKotlinDependencyResolver {
    if (this is CompositeIdeaKotlinDependencyResolver && other is CompositeIdeaKotlinDependencyResolver) {
        return CompositeIdeaKotlinDependencyResolver(this.children + other.children)
    }

    if (this is CompositeIdeaKotlinDependencyResolver) {
        return CompositeIdeaKotlinDependencyResolver(this.children + other)
    }

    if (other is CompositeIdeaKotlinDependencyResolver) {
        return CompositeIdeaKotlinDependencyResolver(listOf(this) + other.children)
    }

    return CompositeIdeaKotlinDependencyResolver(listOf(this, other))
}

private class CompositeIdeaKotlinDependencyResolver(
    val children: List<IdeaKotlinDependencyResolver>
) : IdeaKotlinDependencyResolver {
    override fun resolve(fragment: KotlinGradleFragment): Set<IdeaKotlinDependency> {
        return children.flatMap { child -> child.resolve(fragment) }.toSet()
    }
}
