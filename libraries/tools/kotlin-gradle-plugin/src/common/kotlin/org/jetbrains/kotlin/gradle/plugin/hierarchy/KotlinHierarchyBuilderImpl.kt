/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.hierarchy

import org.gradle.api.InvalidUserCodeException
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.DEPRECATED_TARGET_MESSAGE
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.core.closure


internal suspend fun KotlinHierarchyTemplate.buildHierarchy(compilation: KotlinCompilation<*>): KotlinHierarchy? {
    val context = KotlinHierarchyBuilderImplContext(compilation)
    context.root.applyHierarchyTemplate(this)
    return context.build(KotlinHierarchy.Node.Root)
}

private class KotlinHierarchyBuilderImplContext(private val compilation: KotlinCompilation<*>) {
    val root by lazy { KotlinHierarchyBuilderRootImpl(getOrCreateBuilder(KotlinHierarchy.Node.Root)) }

    private val builders = hashMapOf<KotlinHierarchy.Node, KotlinHierarchyBuilderImpl>()
    private val builtValues = hashMapOf<KotlinHierarchy.Node, KotlinHierarchy?>()

    fun getOrCreateBuilder(node: KotlinHierarchy.Node): KotlinHierarchyBuilderImpl = builders.getOrPut(node) {
        KotlinHierarchyBuilderImpl(this, node)
    }

    suspend fun build(node: KotlinHierarchy.Node): KotlinHierarchy? {
        return builtValues.getOrPut(node) {
            val builder = getOrCreateBuilder(node)
            if (!builder.contains(compilation)) return@getOrPut null

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
            KotlinHierarchy(node, directChildren)
        }
    }
}

private class KotlinHierarchyBuilderRootImpl(
    private val builder: KotlinHierarchyBuilderImpl,
) : KotlinHierarchyBuilder.Root, KotlinHierarchyBuilder by builder {


    override fun sourceSetTrees(vararg tree: KotlinSourceSetTree) {
        builder.sourceSetTrees = tree.toHashSet()
    }

    override fun withSourceSetTree(vararg tree: KotlinSourceSetTree) {
        builder.sourceSetTrees = builder.sourceSetTrees.orEmpty().plus(tree)
    }

    override fun excludeSourceSetTree(vararg tree:KotlinSourceSetTree) {
        val modules = tree.toHashSet()
        if (modules.isEmpty()) return
        builder.sourceSetTrees = builder.sourceSetTrees.orEmpty() - modules
    }
}


private class KotlinHierarchyBuilderImpl(
    val context: KotlinHierarchyBuilderImplContext,
    val node: KotlinHierarchy.Node,
) : KotlinHierarchyBuilder {

    val children = mutableSetOf<KotlinHierarchyBuilderImpl>()
    val childrenClosure get() = closure { it.children }

    var sourceSetTrees: Set<KotlinSourceSetTree>? = null
    private var includePredicate: ((KotlinCompilation<*>) -> Boolean) = { false }
    private var excludePredicate: ((KotlinCompilation<*>) -> Boolean) = { false }


    override fun withCompilations(predicate: (KotlinCompilation<*>) -> Boolean) {
        val previousIncludePredicate = this.includePredicate
        val previousExcludePredicate = this.excludePredicate
        this.includePredicate = { previousIncludePredicate(it) || predicate(it) }
        this.excludePredicate = { previousExcludePredicate(it) && !predicate(it) }
    }

    override fun excludeCompilations(predicate: (KotlinCompilation<*>) -> Boolean) {
        val previousIncludePredicate = this.includePredicate
        val previousExcludePredicate = this.excludePredicate
        this.includePredicate = { previousIncludePredicate(it) && !predicate(it) }
        this.excludePredicate = { previousExcludePredicate(it) || predicate(it) }
    }

    suspend fun contains(compilation: KotlinCompilation<*>): Boolean {
        sourceSetTrees?.let { sourceSetTrees ->
            val sourceSetTree = KotlinSourceSetTree.orNull(compilation) ?: return false
            if (sourceSetTree !in sourceSetTrees) return false
        }

        /* Return eagerly, when compilation is explicitly excluded */
        if (excludePredicate(compilation)) return false

        /* Return eagerly, when compilation is explicitly included */
        if (includePredicate(compilation)) return true

        /* Find any child that includes this compilation */
        return childrenClosure.any { child -> child.contains(compilation) }
    }

    private inline fun withTargets(crossinline predicate: (KotlinTarget) -> Boolean) = withCompilations { predicate(it.target) }

    override fun group(name: String, build: KotlinHierarchyBuilder.() -> Unit) {
        val node = KotlinHierarchy.Node.Group(name)
        val child = context.getOrCreateBuilder(node).also(build)
        children.add(child)
        checkCyclicHierarchy()
    }

    override fun withNative() = withTargets { it is KotlinNativeTarget }

    override fun withApple() = withTargets { it is KotlinNativeTarget && it.konanTarget.family.isAppleFamily }

    override fun withIos() = withTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.IOS }

    override fun withWatchos() = withTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.WATCHOS }

    override fun withMacos() = withTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.OSX }

    override fun withTvos() = withTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.TVOS }

    override fun withMingw() = withTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.MINGW }

    override fun withLinux() = withTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.LINUX }

    override fun withAndroidNative() = withTargets { it is KotlinNativeTarget && it.konanTarget.family == Family.ANDROID }

    // Don't check for instance of [KotlinJsTargetDsl] or [KotlinWasmTargetDsl] because they are implemented by single target [KotlinJsIrTarget]
    override fun withJs() = withTargets { it.platformType == KotlinPlatformType.js }

    @Deprecated("Renamed to 'withWasmJs''", replaceWith = ReplaceWith("withWasmJs()"))
    override fun withWasm() = withWasmJs()

    override fun withWasmJs() = withTargets {
        it.platformType == KotlinPlatformType.wasm && it is KotlinJsIrTarget && it.wasmTargetType == KotlinWasmTargetType.JS
    }

    override fun withWasmWasi() = withTargets {
        it.platformType == KotlinPlatformType.wasm && it is KotlinJsIrTarget && it.wasmTargetType == KotlinWasmTargetType.WASI
    }

    override fun withJvm() = withTargets {
        it is KotlinJvmTarget ||
                /*
                Handle older KotlinWithJavaTarget correctly:
                KotlinWithJavaTarget is also registered as the target in Kotlin2JsProjectExtension
                using KotlinPlatformType.js instead of jvm.
                 */
                (it is KotlinWithJavaTarget<*, *> && it.platformType == KotlinPlatformType.jvm)
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun withAndroid() = withAndroidTarget()

    override fun withAndroidTarget() = withTargets { it is KotlinAndroidTarget }

    override fun withAndroidNativeX64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.ANDROID_X64
    }

    override fun withAndroidNativeX86() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.ANDROID_X86
    }

    override fun withAndroidNativeArm32() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.ANDROID_X86
    }

    override fun withAndroidNativeArm64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.ANDROID_ARM64
    }

    override fun withIosArm32() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.IOS_ARM32
    }

    override fun withIosArm64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.IOS_ARM64
    }

    override fun withIosX64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.IOS_X64
    }

    override fun withIosSimulatorArm64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.IOS_SIMULATOR_ARM64
    }

    override fun withWatchosArm32() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_ARM32
    }

    override fun withWatchosArm64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_ARM64
    }

    override fun withWatchosX64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_X64
    }

    override fun withWatchosSimulatorArm64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_SIMULATOR_ARM64
    }

    override fun withWatchosDeviceArm64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_DEVICE_ARM64
    }

    override fun withTvosArm64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.TVOS_ARM64
    }

    override fun withTvosX64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.TVOS_X64
    }

    override fun withTvosSimulatorArm64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.TVOS_SIMULATOR_ARM64
    }

    override fun withLinuxX64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_X64
    }

    override fun withMingwX64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MINGW_X64
    }

    override fun withMacosX64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MACOS_X64
    }

    override fun withMacosArm64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MACOS_ARM64
    }

    override fun withLinuxArm64() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_ARM64
    }

    @Deprecated(DEPRECATED_TARGET_MESSAGE, level = DeprecationLevel.ERROR)
    override fun withWatchosX86() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_X86
    }

    @Deprecated(DEPRECATED_TARGET_MESSAGE, level = DeprecationLevel.ERROR)
    override fun withMingwX86() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MINGW_X86
    }

    @Deprecated(DEPRECATED_TARGET_MESSAGE, level = DeprecationLevel.ERROR)
    override fun withLinuxArm32Hfp() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_ARM32_HFP
    }

    @Deprecated(DEPRECATED_TARGET_MESSAGE, level = DeprecationLevel.ERROR)
    override fun withLinuxMips32() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_MIPS32
    }

    @Deprecated(DEPRECATED_TARGET_MESSAGE, level = DeprecationLevel.ERROR)
    override fun withLinuxMipsel32() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_MIPSEL32
    }

    @Deprecated(DEPRECATED_TARGET_MESSAGE, level = DeprecationLevel.ERROR)
    override fun withWasm32() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WASM32
    }

    override fun toString(): String {
        return "KotlinHierarchyBuilder($node)"
    }
}

/* Cycle Detection: Provide feedback for users when a KotlinHierarchy cycle is declared */

private fun KotlinHierarchyBuilderImpl.checkCyclicHierarchy(): Nothing? {
    val stack = mutableListOf(node)
    val visited = hashSetOf<KotlinHierarchyBuilderImpl>()

    fun checkChild(child: KotlinHierarchyBuilderImpl) {
        if (!visited.add(child)) return
        stack += child.node
        if (this == child) throw CyclicKotlinHierarchyException(stack)
        child.children.forEach { next -> checkChild(next) }
        stack -= child.node
    }

    children.forEach { child -> checkChild(child) }
    return null
}

internal class CyclicKotlinHierarchyException(val cycle: List<KotlinHierarchy.Node>) : InvalidUserCodeException(
    "KotlinHierarchy cycle detected: ${cycle.joinToString(" -> ")}"
)
