/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.sharding

import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import kotlin.test.assertEquals
import kotlin.test.fail

/**
 * Tags test classes which are only meant to be executed by other tests (using their own launcher).
 * Such classes may fail deliberately and are therefore excluded from the 'test' task.
 */
internal const val testFixtureTag = "tests.fixture"

/**
 * Executes [testClasses] on the shard described by [configuration], expecting no failures, and returns the succeeded tests.
 * The sharding filter is registered explicitly with the [configuration], instead of the one of this JVM (see 'tests.currentShard').
 */
internal fun executeTests(configuration: TestShardingConfiguration, vararg testClasses: Class<*>): List<TestIdentifier> {
    val succeeded = mutableListOf<TestIdentifier>()
    val failed = mutableListOf<String>()

    val launcher = LauncherFactory.create(LauncherConfig.builder().enablePostDiscoveryFilterAutoRegistration(false).build())
    val request = LauncherDiscoveryRequestBuilder.request()
        .selectors(testClasses.map { selectClass(it) })
        .configurationParameters(configuration.toConfigurationParameters())
        .filters(TestShardingPostDiscoveryFilter(configuration))
        .build()

    launcher.execute(request, object : TestExecutionListener {
        override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
            when {
                testExecutionResult.status != TestExecutionResult.Status.SUCCESSFUL -> failed.add("${testIdentifier.uniqueId}: $testExecutionResult")
                testIdentifier.isTest -> succeeded.add(testIdentifier)
            }
        }
    })

    if (failed.isNotEmpty()) fail("Unexpected failures on $configuration:\n${failed.joinToString("\n")}")
    return succeeded
}

/** The name of the test class containing this test (e.g., also for dynamic tests or invocations of a test template) */
internal val TestIdentifier.className: String
    get() = uniqueIdObject.segments.firstOrNull { it.type == "class" }?.value ?: error("Missing class of '$uniqueId'")

/**
 * Checks that the [shards] are not empty and together execute [all] tests, each test exactly once
 */
internal fun checkShardDistribution(all: List<TestIdentifier>, vararg shards: List<TestIdentifier>) {
    /* Test is suspicious if a shard has no tests */
    shards.forEachIndexed { index, shard ->
        if (shard.isEmpty()) fail("Shard ${index + 1} does not contain any tests")
    }

    /* Test shards should not have overlapping tests */
    for (a in shards.indices) {
        for (b in a + 1 until shards.size) {
            val intersection = shards[a].map { it.uniqueId }.intersect(shards[b].map { it.uniqueId }.toSet())
            if (intersection.isNotEmpty()) {
                fail("Shard ${a + 1} and ${b + 1} have ${intersection.size} tests in common: ${intersection.joinToString(", ")}")
            }
        }
    }

    /* Test shards should execute all tests */
    assertEquals(
        all.map { it.uniqueId }.sorted(), shards.toList().flatten().map { it.uniqueId }.sorted(),
        "Expected all tests to be distributed across shards without duplicates"
    )
}

/**
 * Checks that all tests of a class (of [all]) are executed on exactly one of the [shards]
 */
internal fun checkClassesAreNotSplit(all: List<TestIdentifier>, vararg shards: List<TestIdentifier>) {
    all.groupBy { it.className }.forEach { (className, tests) ->
        val shardsContainingClass = shards.filter { shard -> shard.any { it.className == className } }
        assertEquals(1, shardsContainingClass.size, "Expected $className to run on exactly one shard")
        assertEquals(
            tests.map { it.uniqueId }.toSet(),
            shardsContainingClass.single().filter { it.className == className }.map { it.uniqueId }.toSet()
        )
    }
}
