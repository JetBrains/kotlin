/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.internal

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.MockKotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.MockKotlinSourceSet
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class KotlinCompilationsModuleGroupsTest {
    private lateinit var instance: KotlinCompilationsModuleGroups

    @Before
    fun before() {
        instance = KotlinCompilationsModuleGroups()
    }

    private fun compilation(name: String) = MockKotlinCompilation(name, MockKotlinSourceSet(name))

    @Test
    fun testIdentityAsModuleLeaderForNewCompilation() {
        val a = compilation("a")
        assertEquals(a, instance.getModuleLeader(a))
    }

    private fun assertLeader(leader: KotlinCompilation<*>, vararg ofCompilations: KotlinCompilation<*>) {
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