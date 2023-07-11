/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.gradle.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.dsl.NativeTargetShortcutTrace.Companion.nativeTargetShortcutTrace
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_TEST_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.tooling.core.extrasReadWriteProperty


private const val SHORTCUTS_DEPRECATION_MESSAGE = "Use applyDefaultHierarchyTemplate() instead. " +
        "Deprecated since 1.9.20, scheduled for removal in 2.0"

@KotlinGradlePluginDsl
@Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
interface KotlinTargetContainerWithNativeShortcuts : KotlinTargetContainerWithPresetFunctions, KotlinSourceSetContainer {

    private data class DefaultSourceSets(val main: KotlinSourceSet, val test: KotlinSourceSet)

    private fun KotlinNativeTarget.defaultSourceSets(): DefaultSourceSets =
        DefaultSourceSets(
            compilations.getByName(MAIN_COMPILATION_NAME).defaultSourceSet,
            compilations.getByName(TEST_COMPILATION_NAME).defaultSourceSet
        )

    private fun mostCommonSourceSets() = DefaultSourceSets(
        sourceSets.getByName(COMMON_MAIN_SOURCE_SET_NAME),
        sourceSets.getByName(COMMON_TEST_SOURCE_SET_NAME)
    )

    private fun List<KotlinNativeTarget>.defaultSourceSets(): List<DefaultSourceSets> = map { it.defaultSourceSets() }

    private fun createIntermediateSourceSet(
        name: String,
        children: List<KotlinSourceSet>,
        parent: KotlinSourceSet? = null,
    ): KotlinSourceSet =
        sourceSets.maybeCreate(name).apply {
            parent?.let { dependsOn(parent) }
            children.forEach {
                it.dependsOn(this)
            }
        }

    private fun createIntermediateSourceSets(
        trace: NativeTargetShortcutTrace,
        namePrefix: String,
        children: List<DefaultSourceSets>,
        parent: DefaultSourceSets? = null,
    ): DefaultSourceSets {
        val main = createIntermediateSourceSet("${namePrefix}Main", children.map { it.main }, parent?.main)
        val test = createIntermediateSourceSet("${namePrefix}Test", children.map { it.test }, parent?.test)

        main.nativeTargetShortcutTrace = trace
        test.nativeTargetShortcutTrace = trace

        children.forEach { child ->
            child.main.nativeTargetShortcutTrace = trace
            child.test.nativeTargetShortcutTrace = trace
        }

        return DefaultSourceSets(main, test)
    }

    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun ios(
        namePrefix: String = "ios",
        configure: KotlinNativeTarget.() -> Unit = {},
    ) {
        val targets = listOf(
            iosArm64("${namePrefix}Arm64"),
            iosX64("${namePrefix}X64")
        )
        createIntermediateSourceSets(
            NativeTargetShortcutTrace("ios"), namePrefix, targets.defaultSourceSets(), mostCommonSourceSets()
        )
        targets.forEach { it.configure() }
    }

    /**
     * Deprecated:
     * Declare targets explicitly like
     * ```kotlin
     * kotlin {
     *     applyDefaultHierarchyTemplate() /* <- optional; is applied by default, when compatible */
     *
     *     iosX64()
     *     iosArm64()
     *     iosSimulatorArm64() // <- Note: This target was previously not registered by the ios() shortcut!
     *
     *     /* ... more targets! */
     * }
     * ```
     */
    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun ios() = ios("ios") { }

    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun ios(namePrefix: String) = ios(namePrefix) { }

    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun ios(namePrefix: String, configure: Action<KotlinNativeTarget>) = ios(namePrefix) { configure.execute(this) }

    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun ios(configure: Action<KotlinNativeTarget>) = ios { configure.execute(this) }

    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun tvos(
        namePrefix: String = "tvos",
        configure: KotlinNativeTarget.() -> Unit,
    ) {
        val targets = listOf(
            tvosArm64("${namePrefix}Arm64"),
            tvosX64("${namePrefix}X64")
        )
        createIntermediateSourceSets(
            NativeTargetShortcutTrace("tvos"),
            namePrefix, targets.defaultSourceSets(), mostCommonSourceSets()
        )
        targets.forEach { it.configure() }
    }

    /**
     * Deprecated:
     * Declare targets explicitly like
     * ```kotlin
     * kotlin {
     *     applyDefaultHierarchyTemplate() /* <- optional; is applied by default, when compatible */
     *
     *     tvosArm64()
     *     tvosX64()
     *     tvosSimulatorArm64() // <- Note: This target was previously not registered by the tvos() shortcut!
     *
     *     /* ... more targets! */
     * }
     * ```
     */
    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun tvos() = tvos("tvos") { }

    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun tvos(namePrefix: String) = tvos(namePrefix) { }

    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun tvos(namePrefix: String, configure: Action<KotlinNativeTarget>) = tvos(namePrefix) { configure.execute(this) }

    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun tvos(configure: Action<KotlinNativeTarget>) = tvos { configure.execute(this) }

    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun watchos(
        namePrefix: String = "watchos",
        configure: KotlinNativeTarget.() -> Unit = {},
    ) {
        val device32 = watchosArm32("${namePrefix}Arm32")
        val device64 = watchosArm64("${namePrefix}Arm64")
        val simulatorX64 = watchosX64("${namePrefix}X64")
        val deviceTargets = listOf(device32, device64)

        val trace = NativeTargetShortcutTrace("watchos")

        val deviceSourceSets = createIntermediateSourceSets(
            trace,
            "${namePrefix}Device",
            deviceTargets.defaultSourceSets()
        )

        createIntermediateSourceSets(
            trace,
            namePrefix,
            listOf(deviceSourceSets, simulatorX64.defaultSourceSets()),
            mostCommonSourceSets()
        )

        listOf(device32, device64, simulatorX64).forEach { it.configure() }
    }

    /**
     * Deprecated:
     * Declare targets explicitly like
     * ```kotlin
     * kotlin {
     *     applyDefaultHierarchyTemplate() /* <- optional; is applied by default, when compatible */
     *
     *     watchosArm64()
     *     watchosX64()
     *     watchosSimulatorArm64() // <- Note: This target was previously not registered by the watchos() shortcut!
     *     watchosArm32() //<- Note: This target was previously applied, but is likely not needed anymore
     *
     *
     *     /* ... more targets! */
     * }
     * ```
     */
    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun watchos() = watchos("watchos") { }

    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun watchos(namePrefix: String) = watchos(namePrefix) { }

    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun watchos(namePrefix: String, configure: Action<KotlinNativeTarget>) = watchos(namePrefix) { configure.execute(this) }

    @Deprecated(SHORTCUTS_DEPRECATION_MESSAGE)
    fun watchos(configure: Action<KotlinNativeTarget>) = watchos { configure.execute(this) }
}

internal class NativeTargetShortcutTrace(val shortcut: String) : Throwable() {
    companion object {
        var KotlinSourceSet.nativeTargetShortcutTrace by extrasReadWriteProperty<NativeTargetShortcutTrace>()
    }
}
