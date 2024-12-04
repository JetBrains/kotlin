/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.hierarchy

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.tooling.core.withClosure

internal data class KotlinHierarchy(
    val node: Node, val children: Set<KotlinHierarchy> = emptySet(),
) {

    val childrenClosure: Set<KotlinHierarchy> =
        children.withClosure<KotlinHierarchy> { it.children }

    sealed class Node {
        abstract suspend fun sharedSourceSetName(compilation: KotlinCompilation<*>): String?

        object Root : Node() {
            override suspend fun sharedSourceSetName(compilation: KotlinCompilation<*>): String? = null
        }

        data class Group(val name: String) : Node() {
            override suspend fun sharedSourceSetName(compilation: KotlinCompilation<*>): String? {
                val sourceSetTree = KotlinSourceSetTree.orNull(compilation)?.name ?: return null
                return lowerCamelCaseName(name, sourceSetTree)
            }
        }

        final override fun toString(): String = when (this) {
            is Group -> name
            is Root -> "<root>"
        }
    }

    override fun toString(): String {
        if (children.isEmpty()) return node.toString()
        return node.toString() + "\n" + children.joinToString("\n").prependIndent("----")
    }
}
