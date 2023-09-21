/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.LocalScope
import org.jetbrains.kotlin.formver.embeddings.ScopedKotlinName
import org.jetbrains.kotlin.formver.embeddings.SimpleKotlinName
import org.jetbrains.kotlin.name.Name

/**
 * Resolver for names of local properties.
 *
 * This is a stacked resolver: the resolver for the innermost scope contains a reference
 * to the resolver for the outer scopes, and automatically searches them.
 */
sealed class BasePropertyNameResolver {
    abstract val parent: BasePropertyNameResolver?
    abstract fun tryResolveLocalPropertyName(name: Name): ScopedKotlinName?
    abstract fun registerLocalPropertyName(name: Name)

    fun innerScope(scopeDepth: Int) = ChildPropertyNameResolver(scopeDepth, this)
}

data object RootPropertyNameResolver : BasePropertyNameResolver() {
    override val parent: BasePropertyNameResolver? = null

    override fun tryResolveLocalPropertyName(name: Name): ScopedKotlinName? = null
    override fun registerLocalPropertyName(name: Name) {}
}

class ChildPropertyNameResolver(private val scopeDepth: Int, override val parent: BasePropertyNameResolver) : BasePropertyNameResolver() {
    private val names: MutableSet<Name> = mutableSetOf()

    override fun tryResolveLocalPropertyName(name: Name): ScopedKotlinName? =
        if (names.contains(name)) {
            ScopedKotlinName(LocalScope(scopeDepth), SimpleKotlinName(name))
        } else {
            parent.tryResolveLocalPropertyName(name)
        }

    override fun registerLocalPropertyName(name: Name) {
        names.add(name)
    }
}