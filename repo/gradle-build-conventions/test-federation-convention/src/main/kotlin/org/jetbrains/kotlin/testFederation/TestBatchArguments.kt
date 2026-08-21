/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.gradle.process.CommandLineArgumentProvider

internal const val CURRENT_TEST_BATCH_KEY = "tests.currentBatch"
internal const val TOTAL_TEST_BATCHES_KEY = "tests.totalBatches"
internal const val TEST_BATCH_SEED_KEY = "tests.batchSeed"

internal val Project.testBatchArguments: TestBatchArguments
    get() {
        val arguments = objects.newInstance(TestBatchArguments::class.java)
        arguments.currentBatch.set(project.providers.gradleProperty(CURRENT_TEST_BATCH_KEY).map { it.toInt() })
        arguments.totalBatches.set(project.providers.gradleProperty(TOTAL_TEST_BATCHES_KEY).map { it.toInt() })
        arguments.batchSeed.set(project.providers.gradleProperty(TEST_BATCH_SEED_KEY).map { it.toInt() })
        return arguments
    }

abstract class TestBatchArguments : CommandLineArgumentProvider {

    @get:Input
    @get:Optional
    abstract val currentBatch: Property<Int>

    @get:Input
    @get:Optional
    abstract val totalBatches: Property<Int>

    @get:Input
    @get:Optional
    abstract val batchSeed: Property<Int>

    override fun asArguments(): Iterable<String> {
        if (!currentBatch.isPresent || !totalBatches.isPresent) {
            return emptyList()
        }

        return listOfNotNull(
            "-D$CURRENT_TEST_BATCH_KEY=${currentBatch.get()}",
            "-D$TOTAL_TEST_BATCHES_KEY=${totalBatches.get()}",
            if (batchSeed.isPresent) "-D$TEST_BATCH_SEED_KEY=${batchSeed.get()}" else null,
        )
    }
}
