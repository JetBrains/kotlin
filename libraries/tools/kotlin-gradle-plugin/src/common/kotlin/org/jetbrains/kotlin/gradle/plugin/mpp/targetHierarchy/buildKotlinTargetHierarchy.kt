/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.targetHierarchy

import org.gradle.api.InvalidUserCodeException
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinWithJavaTarget
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.core.closure

internal fun KotlinTargetHierarchyDescriptor.buildKotlinTargetHierarchy(compilation: KotlinCompilation<*>): KotlinTargetHierarchy? {
    val context = KotlinTargetHierarchyBuilderImplContext(compilation)
    describe(context.getOrCreateBuilder(KotlinTargetHierarchy.Node.Root))
    return context.build(KotlinTargetHierarchy.Node.Root)
}

private class KotlinTargetHierarchyBuilderImplContext(private val compilation: KotlinCompilation<*>) {
    private val builders = hashMapOf<KotlinTargetHierarchy.Node, KotlinTargetHierarchyBuilderImpl>()
    private val builtValues = hashMapOf<KotlinTargetHierarchy.Node, KotlinTargetHierarchy?>()

    fun getOrCreateBuilder(node: KotlinTargetHierarchy.Node): KotlinTargetHierarchyBuilderImpl = builders.getOrPut(node) {
        KotlinTargetHierarchyBuilderImpl(this, node)
    }

    fun build(node: KotlinTargetHierarchy.Node): KotlinTargetHierarchy? {
        return builtValues.getOrPut(node) {
            val builder = getOrCreateBuilder(node)
            if (compilation !in builder) return@getOrPut null

            /*
            Keep the hierarchy 'deduplicated'.
            e.g.
            if we have two child hierarchies:

            1) a -> b -> c
            2) b -> c

            then we can remove the duplicated 'sub hierarchy' 2) and only return 1) a -> b -> c
            (Remove children that are also reachable by more specific paths)
            */
            val children = builder.children.mapNotNull { child -> build(child.node) }
            val directChildren = children.toSet() - children.flatMap { child -> child.childrenClosure }.toSet()
            KotlinTargetHierarchy(node, directChildren)
        }
    }
}

private class KotlinTargetHierarchyBuilderImpl(
    val context: KotlinTargetHierarchyBuilderImplContext,
    val node: KotlinTargetHierarchy.Node,
) : KotlinTargetHierarchyBuilder {

    val children = mutableSetOf<KotlinTargetHierarchyBuilderImpl>()
    val childrenClosure get() = closure { it.children }

    private var includePredicate: ((KotlinCompilation<*>) -> Boolean) = { false }
    private var excludePredicate: ((KotlinCompilation<*>) -> Boolean) = { false }

    override fun addCompilations(predicate: (KotlinCompilation<*>) -> Boolean) {
        val previousIncludePredicate = this.includePredicate
        val previousExcludePredicate = this.excludePredicate
        this.includePredicate = { previousIncludePredicate(it) || predicate(it) }
        this.excludePredicate = { previousExcludePredicate(it) && !predicate(it) }
    }

    override fun removeCompilations(predicate: (KotlinCompilation<*>) -> Boolean) {
        val previousIncludePredicate = this.includePredicate
        val previousExcludePredicate = this.excludePredicate
        this.includePredicate = { previousIncludePredicate(it) && !predicate(it) }
        this.excludePredicate = { previousExcludePredicate(it) || predicate(it) }
    }

    operator fun contains(compilation: KotlinCompilation<*>): Boolean {
        /* Return eagerly, when compilation is explicitly excluded */
        if (excludePredicate(compilation)) return false

        /* Return eagerly, when compilation is explicitly included */
        if (includePredicate(compilation)) return true

        /* Find any child that includes this compilation */
        return childrenClosure.any { child -> compilation in child }
    }

    private inline fun addTargets(crossinline predicate: (KotlinTarget) -> Boolean) = addCompilations { predicate(it.target) }

    override fun group(name: String, build: KotlinTargetHierarchyBuilder.() -> Unit) {
        val node = KotlinTargetHierarchy.Node.Group(name)
        val child = context.getOrCreateBuilder(node).also(build)
        children.add(child)
        checkCyclicHierarchy()
    }

    override fun anyNative() = addTargets { it is KotlinNativeTarget }

    override fun anyApple() = addTargets { it is KotlinNativeTarget && it.konanTarget.family.isAppleFamily }

    override fun anyIos() = addTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.IOS }

    override fun anyWatchos() = addTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.WATCHOS }

    override fun anyMacos() = addTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.OSX }

    override fun anyTvos() = addTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.TVOS }

    override fun anyMingw() = addTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.MINGW }

    override fun anyLinux() = addTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.LINUX }

    override fun anyAndroidNative() = addTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.ANDROID }

    override fun anyJs() = addTargets { it is KotlinJsTargetDsl }

    override fun jvm() = addTargets {
        it is KotlinJvmTarget ||
                /*
                Handle older KotlinWithJavaTarget correctly:
                KotlinWithJavaTarget is also registered as the target in Kotlin2JsProjectExtension
                using KotlinPlatformType.js instead of jvm.
                 */
                (it is KotlinWithJavaTarget<*, *> && it.platformType == KotlinPlatformType.jvm)
    }

    override fun android() = addTargets { it is KotlinAndroidTarget }

    override fun androidNativeX64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.ANDROID_X64
    }

    override fun androidNativeX86() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.ANDROID_X86
    }

    override fun androidNativeArm32() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.ANDROID_X86
    }

    override fun androidNativeArm64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.ANDROID_ARM64
    }

    override fun iosArm32() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.IOS_ARM32
    }

    override fun iosArm64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.IOS_ARM64
    }

    override fun iosX64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.IOS_X64
    }

    override fun iosSimulatorArm64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.IOS_SIMULATOR_ARM64
    }

    override fun watchosArm32() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_ARM32
    }

    override fun watchosArm64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_ARM64
    }

    override fun watchosX86() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_X86
    }

    override fun watchosX64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_X64
    }

    override fun watchosSimulatorArm64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_SIMULATOR_ARM64
    }

    override fun watchosDeviceArm64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_DEVICE_ARM64
    }

    override fun tvosArm64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.TVOS_ARM64
    }

    override fun tvosX64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.TVOS_X64
    }

    override fun tvosSimulatorArm64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.TVOS_SIMULATOR_ARM64
    }

    override fun linuxX64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_X64
    }

    override fun mingwX86() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MINGW_X86
    }

    override fun mingwX64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MINGW_X64
    }

    override fun macosX64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MACOS_X64
    }

    override fun macosArm64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MACOS_ARM64
    }

    override fun linuxArm64() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_ARM64
    }

    override fun linuxArm32Hfp() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_ARM32_HFP
    }

    override fun linuxMips32() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_MIPS32
    }

    override fun linuxMipsel32() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_MIPSEL32
    }

    override fun wasm32() = addTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WASM32
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
