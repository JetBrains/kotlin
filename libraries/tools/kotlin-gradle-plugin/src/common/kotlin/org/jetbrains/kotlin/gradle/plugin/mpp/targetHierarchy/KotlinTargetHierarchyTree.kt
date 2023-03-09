/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.tooling.core.withClosure

internal data class KotlinTargetHierarchyTree(
    val node: Node, val children: Set<KotlinTargetHierarchyTree> = emptySet()
) {

    val childrenClosure: Set<KotlinTargetHierarchyTree> =
        children.withClosure<KotlinTargetHierarchyTree> { it.children }

    sealed class Node {
        abstract suspend fun sharedSourceSetName(compilation: KotlinCompilation<*>): String?

        object Root : Node() {
            override suspend fun sharedSourceSetName(compilation: KotlinCompilation<*>): String? = null
        }

        data class Group(val name: String) : Node() {
            override suspend fun sharedSourceSetName(compilation: KotlinCompilation<*>): String? {
                val moduleName = KotlinTargetHierarchy.ModuleName.orNull(compilation)?.name ?: return null
                return lowerCamelCaseName(name, moduleName)
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
