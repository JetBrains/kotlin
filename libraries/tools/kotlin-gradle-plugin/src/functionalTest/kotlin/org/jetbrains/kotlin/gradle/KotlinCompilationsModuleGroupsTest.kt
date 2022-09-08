/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.internal.KotlinCompilationsModuleGroups
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinCompilationData
import org.junit.Test
import kotlin.test.assertEquals

class KotlinCompilationsModuleGroupsTest {
    private val project = buildProjectWithMPP()
    private val kotlin = project.multiplatformExtension
    private val instance: KotlinCompilationsModuleGroups = KotlinCompilationsModuleGroups()

    private fun compilation(name: String): AbstractKotlinCompilation<*> =
        kotlin.jvm().compilations.maybeCreate(name)

    @Test
    fun testIdentityAsModuleLeaderForNewCompilation() {
        val a = compilation("a")
        assertEquals(a, instance.getModuleLeader(a))
    }

    private fun assertLeader(leader: KotlinCompilationData<*>, vararg ofCompilations: KotlinCompilationData<*>) {
        val leadersForModules = ofCompilations.map { instance.getModuleLeader(it) }
        assertEquals(leadersForModules.map { leader }, leadersForModules)
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
