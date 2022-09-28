/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.InternalKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.KotlinCompilationModuleManager
import org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl.kotlinCompilationModuleManager
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.junit.Test
import kotlin.test.assertEquals

class KotlinCompilationsModuleGroupsTest {
    private val project = buildProjectWithMPP()
    private val kotlin = project.multiplatformExtension
    private val instance: KotlinCompilationModuleManager = project.kotlinCompilationModuleManager

    private fun compilation(name: String): InternalKotlinCompilation<*> =
        kotlin.jvm().compilations.maybeCreate(name)

    private fun KotlinCompilationModuleManager.unionModules(first: InternalKotlinCompilation<*>, second: InternalKotlinCompilation<*>) {
        unionModules(first.compilationModule, second.compilationModule)
    }

    @Test
    fun testIdentityAsModuleLeaderForNewCompilation() {
        val a = compilation("a")
        assertEquals(a.internal.compilationModule, instance.getModuleLeader(a.compilationModule))
    }

    private fun assertLeader(leader: InternalKotlinCompilation<*>, vararg ofCompilations: InternalKotlinCompilation<*>) {
        val leadersForModules = ofCompilations.map { instance.getModuleLeader(it.compilationModule) }
        assertEquals(leadersForModules.map { leader.compilationModule }, leadersForModules)
    }

    @Test
    fun testModulesMerged() {
        val (a, b, c, d) = listOf("a", "b", "c", "d").map(this::compilation)

        // Union all but one:
        instance.unionModules(b, c)
        instance.unionModules(a, b)
        assertLeader(a, a, b, c)
        assertLeader(d, d)
    }

    @Test
    fun testMainPreferredAsLeader() {
        val (aMain, aTest) = listOf("aMain", "aTest").map(this::compilation)
        val (zMain, yTest) = listOf("zMain", "yTest").map(this::compilation)
        val (xMain, xBenchmark, xTest) = listOf("xMain", "xBenchmark", "xTest").map(this::compilation)

        instance.unionModules(aMain, aTest)
        assertLeader(aMain, aMain, aTest)

        instance.unionModules(yTest, zMain)
        assertLeader(zMain, zMain, yTest)

        instance.unionModules(xBenchmark, xTest)
        instance.unionModules(xBenchmark, xMain)
        assertLeader(xMain, xMain, xBenchmark, xTest)
    }

    @Test
    fun testNonTestPreferredAsLeader() {
        val (aBenchmark, aPerformanceTest, aTest) = listOf("aBenchmark", "aPerformanceTest", "aTest").map(this::compilation)
        instance.unionModules(aPerformanceTest, aTest)
        instance.unionModules(aTest, aBenchmark)
        assertLeader(aBenchmark, aBenchmark, aPerformanceTest, aTest)
    }

    @Test
    fun unionModuleWithSelf() {
        val compilation = compilation("compilation")
        instance.unionModules(compilation, compilation)
        assertLeader(compilation, compilation)
    }

    @Test
    fun testUnionModulesTwice() {
        val (a, b) = listOf("a", "b").map(this::compilation)
        instance.unionModules(a, b)
        instance.unionModules(a, b)
        assertLeader(a, a, b)
    }
}
