/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.gradle.api.InvalidUserCodeException
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchyBuilder
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchyDescriptor
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.core.closure


internal fun KotlinTargetHierarchyDescriptor.buildKotlinTargetHierarchy(compilation: KotlinCompilation<*>): KotlinTargetHierarchy {
    val context = KotlinTargetHierarchyBuilderImplContext(compilation)
    describe(context.getOrCreateBuilder(KotlinTargetHierarchy.Node.Root))
    return context.build(KotlinTargetHierarchy.Node.Root)
}

private class KotlinTargetHierarchyBuilderImplContext(private val compilation: KotlinCompilation<*>) {
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

            /*
            Respect include & exclude predicates!
             */
            val applicableChildrenNodes = childrenNodes.filter { node ->
                val childrenBuilder = getOrCreateBuilder(node)
                childrenBuilder.predicate?.invoke(compilation) ?: true
            }

            KotlinTargetHierarchy(node, applicableChildrenNodes.map(this::build).toSet())
        }
    }
}

private class KotlinTargetHierarchyBuilderImpl(
    val context: KotlinTargetHierarchyBuilderImplContext,
    val node: KotlinTargetHierarchy.Node,
) : KotlinTargetHierarchyBuilder {

    val children = mutableSetOf<KotlinTargetHierarchyBuilderImpl>()
    val childrenClosure get() = closure { it.children }

    var predicate: ((KotlinCompilation<*>) -> Boolean)? = null

    override fun includeCompilation(predicate: (KotlinCompilation<*>) -> Boolean) {
        val previousPredicate = this.predicate
        if (previousPredicate == null) {
            this.predicate = predicate
            return
        }

        this.predicate = { previousPredicate(it) || predicate(it) }
    }

    override fun excludeCompilation(predicate: (KotlinCompilation<*>) -> Boolean) {
        val previousPredicate = this.predicate
        if (previousPredicate == null) {
            this.predicate = { !predicate(it) }
            return
        }
        this.predicate = { previousPredicate(it) && !predicate(it) }
    }

    private inline fun includeTarget(crossinline predicate: (KotlinTarget) -> Boolean) = includeCompilation { predicate(it.target) }

    override fun anyNative() = includeTarget { it is KotlinNativeTarget }

    override fun anyApple() = includeTarget { it is KotlinNativeTarget && it.konanTarget.family.isAppleFamily }

    override fun anyIos() = includeTarget { it is KotlinNativeTarget && it.konanTarget.family == Family.IOS }

    override fun anyWatchos() = includeTarget { it is KotlinNativeTarget && it.konanTarget.family == Family.WATCHOS }

    override fun anyMacos() = includeTarget { it is KotlinNativeTarget && it.konanTarget.family == Family.OSX }

    override fun anyTvos() = includeTarget { it is KotlinNativeTarget && it.konanTarget.family == Family.TVOS }

    override fun anyMingw() = includeTarget { it is KotlinNativeTarget && it.konanTarget.family == Family.MINGW }

    override fun anyLinux() = includeTarget { it is KotlinNativeTarget && it.konanTarget.family == Family.LINUX }

    override fun anyAndroidNative() = includeTarget { it is KotlinNativeTarget && it.konanTarget.family == Family.ANDROID }

    override fun anyJs() = includeTarget { it is KotlinJsTargetDsl }

    override fun jvm() = includeTarget { it is KotlinJvmTarget }

    override fun android() = includeTarget { it is KotlinAndroidTarget }

    override fun androidNativeX64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.ANDROID_X64
    }

    override fun androidNativeX86() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.ANDROID_X86
    }

    override fun androidNativeArm32() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.ANDROID_X86
    }

    override fun androidNativeArm64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.ANDROID_ARM64
    }

    override fun iosArm32() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.IOS_ARM32
    }

    override fun iosArm64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.IOS_ARM64
    }

    override fun iosX64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.IOS_X64
    }

    override fun iosSimulatorArm64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.IOS_SIMULATOR_ARM64
    }

    override fun watchosArm32() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_ARM32
    }

    override fun watchosArm64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_ARM64
    }

    override fun watchosX86() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_X86
    }

    override fun watchosX64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_X64
    }

    override fun watchosSimulatorArm64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_SIMULATOR_ARM64
    }

    override fun watchosDeviceArm64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_DEVICE_ARM64
    }

    override fun tvosArm64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.TVOS_ARM64
    }

    override fun tvosX64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.TVOS_X64
    }

    override fun tvosSimulatorArm64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.TVOS_SIMULATOR_ARM64
    }

    override fun linuxX64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_X64
    }

    override fun mingwX86() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MINGW_X86
    }

    override fun mingwX64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MINGW_X64
    }

    override fun macosX64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MACOS_X64
    }

    override fun macosArm64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MACOS_ARM64
    }

    override fun linuxArm64() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_ARM64
    }

    override fun linuxArm32Hfp() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_ARM32_HFP
    }

    override fun linuxMips32() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_MIPS32
    }

    override fun linuxMipsel32() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_MIPSEL32
    }

    override fun wasm32() = includeTarget {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WASM32
    }

    override fun group(name: String, build: KotlinTargetHierarchyBuilder.() -> Unit) {
        val node = KotlinTargetHierarchy.Node.Group(name)
        val child = context.getOrCreateBuilder(node)
        children.add(child)
        child.build()
        checkCyclicHierarchy()
    }

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
