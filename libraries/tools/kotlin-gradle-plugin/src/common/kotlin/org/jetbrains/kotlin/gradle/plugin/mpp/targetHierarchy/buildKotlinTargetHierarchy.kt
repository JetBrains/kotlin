/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.gradle.api.InvalidUserCodeException
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.tooling.core.closure

internal fun KotlinTargetHierarchyDescriptor.buildKotlinTargetHierarchy(compilation: KotlinCompilation<*>): KotlinTargetHierarchy {
    val context = KotlinTargetHierarchyBuilderImplContext(compilation)
    this(context.getOrCreateBuilder(KotlinTargetHierarchy.Node.Root))
    return context.build(KotlinTargetHierarchy.Node.Root)
}

private class KotlinTargetHierarchyBuilderImplContext(val compilation: KotlinCompilation<*>) {
    private val builders = hashMapOf<KotlinTargetHierarchy.Node, KotlinTargetHierarchyBuilderImpl>()
    private val builtValues = hashMapOf<KotlinTargetHierarchy.Node, KotlinTargetHierarchy>()

    fun getOrCreateBuilder(node: KotlinTargetHierarchy.Node): KotlinTargetHierarchyBuilderImpl = builders.getOrPut(node) {
        KotlinTargetHierarchyBuilderImpl(this, node)
    }

    fun build(node: KotlinTargetHierarchy.Node): KotlinTargetHierarchy {
        return builtValues.getOrPut(node) {
            val builder = getOrCreateBuilder(node)

            /*
            Keep the hierarchy 'deduplicated'.
            e.g.
            if we have two child hierarchies:

            1) a -> b -> c
            2) b -> c

            then we can remove the duplicated 'sub hierarchy' 2) and only return
            a -> b -> c

             */
            val childrenNodes = builder.children.map { it.node } -
                    builder.children.flatMap { child -> child.childrenClosure }.map { it.node }.toSet()

            KotlinTargetHierarchy(node, childrenNodes.map(this::build).toSet())
        }
    }
}

private class KotlinTargetHierarchyBuilderImpl(
    val context: KotlinTargetHierarchyBuilderImplContext,
    val node: KotlinTargetHierarchy.Node,
) : KotlinTargetHierarchyBuilder {

    override val compilation: KotlinCompilation<*> get() = context.compilation
    override val target: KotlinTarget get() = compilation.target

    val children = mutableSetOf<KotlinTargetHierarchyBuilderImpl>()
    val childrenClosure: Set<KotlinTargetHierarchyBuilderImpl> get() = closure { it.children }

    override fun group(name: String, build: KotlinTargetHierarchyBuilder.() -> Unit) {
        val node = KotlinTargetHierarchy.Node.Group(name)
        val child = context.getOrCreateBuilder(node)
        children.add(child)
        child.build()
        checkCyclicHierarchy()
    }

    override val isNative: Boolean get() = target is KotlinNativeTarget
    override val isApple: Boolean get() = target.let { it is KotlinNativeTarget && it.konanTarget.family.isAppleFamily }
    override val isIos: Boolean get() = target.let { it is KotlinNativeTarget && it.konanTarget.family == Family.IOS }
    override val isWatchos: Boolean get() = target.let { it is KotlinNativeTarget && it.konanTarget.family == Family.WATCHOS }
    override val isMacos: Boolean get() = target.let { it is KotlinNativeTarget && it.konanTarget.family == Family.OSX }
    override val isTvos: Boolean get() = target.let { it is KotlinNativeTarget && it.konanTarget.family == Family.TVOS }
    override val isWindows: Boolean get() = target.let { it is KotlinNativeTarget && it.konanTarget.family == Family.MINGW }
    override val isLinux: Boolean get() = target.let { it is KotlinNativeTarget && it.konanTarget.family == Family.LINUX }
    override val isAndroidNative: Boolean get() = target.let { it is KotlinNativeTarget && it.konanTarget.family == Family.ANDROID }
    override val isJvm: Boolean get() = target is KotlinJvmTarget
    override val isAndroidJvm: Boolean get() = target is KotlinAndroidTarget
    override val isJsLegacy: Boolean get() = target is KotlinJsTarget
    override val isJsIr: Boolean get() = target is KotlinJsIrTarget
    override val isJs: Boolean get() = isJsIr || isJsLegacy

    override fun toString(): String {
        return "KotlinTargetHierarchyBuilder($node)"
    }
}


/* Cycle Detection: Provide feedback for users when a KotlinTargetHierarchy cycle is declared */

private fun KotlinTargetHierarchyBuilderImpl.checkCyclicHierarchy(): Nothing? {
    val stack = mutableListOf(node)
    val visited = hashSetOf<KotlinTargetHierarchyBuilderImpl>()

    fun checkChild(child: KotlinTargetHierarchyBuilderImpl) {
        if (!visited.add(child)) return
        stack += child.node
        if (this == child) throw CyclicKotlinTargetHierarchyException(stack)
        child.children.forEach { next -> checkChild(next) }
        stack -= child.node
    }

    children.forEach { child -> checkChild(child) }
    return null
}

internal class CyclicKotlinTargetHierarchyException(val cycle: List<KotlinTargetHierarchy.Node>) : InvalidUserCodeException(
    "KotlinTargetHierarchy cycle detected: ${cycle.joinToString(" -> ")}"
)
