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
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.konan.target.DEPRECATED_TARGET_MESSAGE
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.tooling.core.closure

internal suspend fun KotlinTargetHierarchyDescriptor.buildKotlinTargetHierarchy(compilation: KotlinCompilation<*>): KotlinTargetHierarchyTree? {
    val context = KotlinTargetHierarchyBuilderImplContext(compilation)
    describe(context.root)
    return context.build(KotlinTargetHierarchyTree.Node.Root)
}

private class KotlinTargetHierarchyBuilderImplContext(private val compilation: KotlinCompilation<*>) {
    val root by lazy { KotlinTargetHierarchyBuilderRootImpl(getOrCreateBuilder(KotlinTargetHierarchyTree.Node.Root)) }

    private val builders = hashMapOf<KotlinTargetHierarchyTree.Node, KotlinTargetHierarchyBuilderImpl>()
    private val builtValues = hashMapOf<KotlinTargetHierarchyTree.Node, KotlinTargetHierarchyTree?>()

    fun getOrCreateBuilder(node: KotlinTargetHierarchyTree.Node): KotlinTargetHierarchyBuilderImpl = builders.getOrPut(node) {
        KotlinTargetHierarchyBuilderImpl(this, node)
    }

    suspend fun build(node: KotlinTargetHierarchyTree.Node): KotlinTargetHierarchyTree? {
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
            KotlinTargetHierarchyTree(node, directChildren)
        }
    }
}

private class KotlinTargetHierarchyBuilderRootImpl(
    private val builder: KotlinTargetHierarchyBuilderImpl
) : KotlinTargetHierarchyBuilder.Root, KotlinTargetHierarchyBuilder by builder {


    override fun modules(vararg module: KotlinTargetHierarchy.ModuleName) {
        builder.modules = module.toHashSet()
    }

    override fun withModule(vararg module: KotlinTargetHierarchy.ModuleName) {
        builder.modules = builder.modules.orEmpty().plus(module)
    }

    override fun excludeModule(vararg module: KotlinTargetHierarchy.ModuleName) {
        val modules = module.toHashSet()
        if (modules.isEmpty()) return
        builder.modules = builder.modules.orEmpty() - modules
    }
}


private class KotlinTargetHierarchyBuilderImpl(
    val context: KotlinTargetHierarchyBuilderImplContext,
    val node: KotlinTargetHierarchyTree.Node,
) : KotlinTargetHierarchyBuilder {

    val children = mutableSetOf<KotlinTargetHierarchyBuilderImpl>()
    val childrenClosure get() = closure { it.children }

    var modules: Set<KotlinTargetHierarchy.ModuleName>? = null
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
        modules?.let { modules ->
            val module = KotlinTargetHierarchy.ModuleName.orNull(compilation) ?: return false
            if (module !in modules) return false
        }

        /* Return eagerly, when compilation is explicitly excluded */
        if (excludePredicate(compilation)) return false

        /* Return eagerly, when compilation is explicitly included */
        if (includePredicate(compilation)) return true

        /* Find any child that includes this compilation */
        return childrenClosure.any { child -> child.contains(compilation) }
    }

    private inline fun withTargets(crossinline predicate: (KotlinTarget) -> Boolean) = withCompilations { predicate(it.target) }

    override fun group(name: String, build: KotlinTargetHierarchyBuilder.() -> Unit) {
        val node = KotlinTargetHierarchyTree.Node.Group(name)
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

    override fun withWasm() = withTargets { it.platformType == KotlinPlatformType.wasm }

    override fun withJvm() = withTargets {
        it is KotlinJvmTarget ||
                /*
                Handle older KotlinWithJavaTarget correctly:
                KotlinWithJavaTarget is also registered as the target in Kotlin2JsProjectExtension
                using KotlinPlatformType.js instead of jvm.
                 */
                (it is KotlinWithJavaTarget<*, *> && it.platformType == KotlinPlatformType.jvm)
    }

    override fun withAndroid() = withTargets { it is KotlinAndroidTarget }

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

    @Deprecated(DEPRECATED_TARGET_MESSAGE)
    override fun withWatchosX86() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.WATCHOS_X86
    }

    @Deprecated(DEPRECATED_TARGET_MESSAGE)
    override fun withMingwX86() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.MINGW_X86
    }

    @Deprecated(DEPRECATED_TARGET_MESSAGE)
    override fun withLinuxArm32Hfp() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_ARM32_HFP
    }

    @Deprecated(DEPRECATED_TARGET_MESSAGE)
    override fun withLinuxMips32() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_MIPS32
    }

    @Deprecated(DEPRECATED_TARGET_MESSAGE)
    override fun withLinuxMipsel32() = withTargets {
        it is KotlinNativeTarget && it.konanTarget == KonanTarget.LINUX_MIPSEL32
    }

    @Deprecated(DEPRECATED_TARGET_MESSAGE)
    override fun withWasm32() = withTargets {
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

internal class CyclicKotlinTargetHierarchyException(val cycle: List<KotlinTargetHierarchyTree.Node>) : InvalidUserCodeException(
    "KotlinTargetHierarchy cycle detected: ${cycle.joinToString(" -> ")}"
)
