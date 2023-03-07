/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmAndroidCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.android.AndroidVariantType
import org.jetbrains.kotlin.gradle.plugin.sources.android.type
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName
import org.jetbrains.kotlin.tooling.core.withClosure

internal data class KotlinTargetHierarchy(
    val node: Node, val children: Set<KotlinTargetHierarchy> = emptySet()
) {

    val childrenClosure: Set<KotlinTargetHierarchy> =
        children.withClosure<KotlinTargetHierarchy> { it.children }

    sealed class Node {
        abstract fun sharedSourceSetName(compilation: KotlinCompilation<*>): String?

        object Root : Node() {
            override fun sharedSourceSetName(compilation: KotlinCompilation<*>): String? = null
        }

        data class Group(val name: String) : Node() {
            override fun sharedSourceSetName(compilation: KotlinCompilation<*>): String? {
                val moduleName = Module.orNull(compilation)?.name ?: return null
                return lowerCamelCaseName(name, moduleName)
            }
        }

        final override fun toString(): String = when (this) {
            is Group -> name
            is Root -> "<root>"
        }
    }

    data class Module(val name: String) {
        override fun toString(): String = name

        companion object {
            val main = Module("main")
            val test = Module("test")
            private val instrumentedTest = Module("instrumentedTest")

            fun orNull(compilation: KotlinCompilation<*>): Module? = when (compilation) {
                is KotlinJvmAndroidCompilation -> when (compilation.androidVariant.type) {
                    AndroidVariantType.Main -> main
                    AndroidVariantType.UnitTest -> test
                    AndroidVariantType.InstrumentedTest -> instrumentedTest
                    AndroidVariantType.Unknown -> null
                }
                else -> when (compilation.name) {
                    "main" -> main
                    "test" -> test
                    else -> Module(compilation.name)
                }
            }
        }
    }

    override fun toString(): String {
        if (children.isEmpty()) return node.toString()
        return node.toString() + "\n" + children.joinToString("\n").prependIndent("----")
    }
}
