/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.conversion

import org.jetbrains.kotlin.formver.embeddings.LocalScope
import org.jetbrains.kotlin.formver.embeddings.ScopedKotlinName
import org.jetbrains.kotlin.formver.embeddings.SimpleKotlinName
import org.jetbrains.kotlin.name.Name

data class LoopIdentifier(val targetName: String, val index: Int)

/**
 * Resolver for names of local properties.
 *
 * This is a stacked resolver: the resolver for the innermost scope contains a reference
 * to the resolver for the outer scopes, and automatically searches them.
 */
class PropertyNameResolver(
    private val scopeDepth: Int,
    val parent: PropertyNameResolver? = null,
    private val loopName: LoopIdentifier? = null
) {
    private val names: MutableSet<Name> = mutableSetOf()

    fun tryResolveLocalPropertyName(name: Name): ScopedKotlinName? =
        if (names.contains(name)) {
            ScopedKotlinName(LocalScope(scopeDepth), SimpleKotlinName(name))
        } else {
            parent?.tryResolveLocalPropertyName(name)
        }

    fun registerLocalPropertyName(name: Name) {
        names.add(name)
    }

    fun innerScope(scopeDepth: Int) = PropertyNameResolver(scopeDepth, this)

    fun addLoopIdentifier(labelName: String, index: Int) = PropertyNameResolver(scopeDepth, parent, LoopIdentifier(labelName, index))

    fun tryResolveLoopName(name: String): Int? {
        return if (loopName?.targetName == name) {
            loopName.index
        } else {
            parent?.tryResolveLoopName(name)
        }
    }
}