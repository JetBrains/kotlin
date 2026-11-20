/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.kotlin.dsl.withType

internal fun Project.configureTestSharding() {
    tasks.withType<Test>().configureEach {
        val testShardingArguments = project.testShardingArguments
        testShardingArguments.shardTask.set(path)
        jvmArgumentProviders.add(testShardingArguments)
        // A shard may get none of the tests of a task, e.g. every shard but one of a task with a single test class.
        // The sharding filter runs in the test JVM, so Gradle would fail such a shard as a task that discovers no tests.
        // Gradle skips that check if any JUnit tags are set, as they are when the nightly tests are excluded
        // (see test-federation-convention), so without this only nightly builds and local runs would fail.
        failOnNoDiscoveredTests.convention(testShardingArguments.shardIndex.map { false }.orElse(true))
        val shardTimingsPath = project.providers.gradleProperty(TEST_SHARD_TIMINGS_KEY).map { "$TEST_SHARD_TIMINGS_DIRECTORY/$it.tsv" }

        doFirst {
            if (testShardingArguments.shardIndex.isPresent) {
                if (testFramework.options !is JUnitPlatformOptions) {
                    error("Test sharding is only supported on 'Junit5'")
                }

                val timingsPath = shardTimingsPath.orNull
                val shardTimings = when {
                    timingsPath == null -> "by hash"
                    testShardingArguments.shardTimings.isPresent -> "by the timings of '$timingsPath'"
                    else -> "by hash, missing '$timingsPath'"
                }
                logger.quiet(
                    "Running tests in shard index ${testShardingArguments.shardIndex.get()} (shard count: ${testShardingArguments.shardCount.get()}), $shardTimings"
                )
            }
        }
    }
}

/**
 * [enabled] = `true` spreads the test methods of all test classes across shards, as if every class was annotated with `@ShardByMethod`.
 * [enabled] = `false` keeps the default (all tests of a class run on the same shard); classes annotated with `@ShardByMethod` are still spread.
 */
fun Test.shardTestsByMethod(enabled: Boolean) {
    systemProperty("tests.shardByMethod", enabled.toString())
}
