/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.gradle.api.Project
import org.gradle.api.internal.tasks.testing.junitplatform.JUnitPlatformTestFramework
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.withType

internal fun Project.configureTestSharding() {
    tasks.withType<Test>().configureEach {
        val testShardingArguments = project.testShardingArguments
        jvmArgumentProviders.add(testShardingArguments)

        doFirst {
            if (testShardingArguments.currentShard.isPresent) {
                if (testFramework !is JUnitPlatformTestFramework) {
                    error("Test sharding is only supported on 'Junit5'")
                }

                logger.quiet("Running tests in shard ${testShardingArguments.currentShard.get()}/${testShardingArguments.totalShards.get()}")
            }
        }
    }
}
