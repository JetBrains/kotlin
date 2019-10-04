/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.dsl

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.MAIN_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation.Companion.TEST_COMPILATION_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_MAIN_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet.Companion.COMMON_TEST_SOURCE_SET_NAME
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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
        parent: KotlinSourceSet? = null
    ) : KotlinSourceSet =
        sourceSets.maybeCreate(name).apply {
            parent?.let { dependsOn(parent) }
            children.forEach {
                it.dependsOn(this)
            }
        }

    private fun createIntermediateSourceSets(
        namePrefix: String,
        children: List<DefaultSourceSets>,
        parent: DefaultSourceSets? = null
    ): DefaultSourceSets {
        val main = createIntermediateSourceSet("${namePrefix}Main", children.map { it.main }, parent?.main)
        val test = createIntermediateSourceSet("${namePrefix}Test", children.map { it.test }, parent?.test)
        return DefaultSourceSets(main, test)
    }

    fun ios(
        namePrefix: String = "ios",
        configure: KotlinNativeTarget.() -> Unit = {}
    ) {
        val targets = listOf(
            iosArm64("${namePrefix}Arm64"),
            iosX64("${namePrefix}X64")
        )
        createIntermediateSourceSets(namePrefix, targets.defaultSourceSets(), mostCommonSourceSets())
        targets.forEach { it.configure() }
    }

    fun ios() = ios("ios") { }
    fun ios(namePrefix: String) = ios(namePrefix) { }
    fun ios(namePrefix: String, configure: Closure<*>) = ios(namePrefix) { ConfigureUtil.configure(configure, this) }
    fun ios(configure: Closure<*>) = ios { ConfigureUtil.configure(configure, this) }

    fun tvos(
        namePrefix: String = "tvos",
        configure: KotlinNativeTarget.() -> Unit
    ) {
        val targets = listOf(
            tvosArm64("${namePrefix}Arm64"),
            tvosX64("${namePrefix}X64")
        )
        createIntermediateSourceSets(namePrefix, targets.defaultSourceSets(), mostCommonSourceSets())
        targets.forEach { it.configure() }
    }

    fun tvos() = tvos("tvos") { }
    fun tvos(namePrefix: String) = tvos(namePrefix) { }
    fun tvos(namePrefix: String, configure: Closure<*>) = tvos(namePrefix) { ConfigureUtil.configure(configure, this) }
    fun tvos(configure: Closure<*>) = tvos { ConfigureUtil.configure(configure, this) }

    fun watchos(
        namePrefix: String = "watchos",
        configure: KotlinNativeTarget.() -> Unit = {}
    ) {
        val device32 = watchosArm32("${namePrefix}Arm32")
        val device64 = watchosArm64("${namePrefix}Arm64")
        val simulator = watchosX86("${namePrefix}X86")
        val deviceTargets = listOf(device32, device64)

        val deviceSourceSets = createIntermediateSourceSets(
            "${namePrefix}Device",
            deviceTargets.defaultSourceSets()
        )

        createIntermediateSourceSets(
            namePrefix,
            listOf(deviceSourceSets, simulator.defaultSourceSets()),
            mostCommonSourceSets()
        )

        listOf(device32, device64, simulator).forEach { it.configure() }
    }

    fun watchos() = watchos("watchos") { }
    fun watchos(namePrefix: String) = watchos(namePrefix) { }
    fun watchos(namePrefix: String, configure: Closure<*>) = watchos(namePrefix) { ConfigureUtil.configure(configure, this) }
    fun watchos(configure: Closure<*>) = watchos { ConfigureUtil.configure(configure, this) }
}
