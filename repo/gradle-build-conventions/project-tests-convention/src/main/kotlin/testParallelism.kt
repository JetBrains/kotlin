import org.gradle.api.tasks.testing.Test

/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

/**
 * See: https://docs.junit.org/6.0.3/writing-tests/parallel-execution.html
 * Will choose a fixed parallelism strategy with the specified number of threads.
 */
fun Test.withJunit5ParallelExecution(parallelism: Int) {
    systemProperty("junit.jupiter.execution.parallel.enabled", "true")
    systemProperty("junit.jupiter.execution.parallel.mode.classes.default", "concurrent")
    systemProperty("junit.jupiter.execution.parallel.config.strategy", "fixed")
    systemProperty("junit.jupiter.execution.parallel.config.fixed.parallelism", "$parallelism")
    systemProperty("junit.jupiter.execution.parallel.config.fixed.max-pool-size", "$parallelism")
}

/**
 * Limits the parallelism of the grouping test engine (`CompilerTestGroupingTestEngine`) for this test task:
 * at most [batchSize] tests are compiled and executed together as a single batch,
 * and at most [simultaneousBatches] batches are processed at the same time.
 * Together, they bound the memory consumption of a run, so set these parameters responsibly:
 * - not very high to keep RAM consumption well under RAM limit for a test task and prevent GC thrashing,
 * - not very low to avoid CPU idling and unnecessary increase of test runners duration.
 * Pick reasonable values which deliver the quickest test task execution.
 */
fun Test.groupingEngineSettings(batchSize: Int, simultaneousBatches: Int) {
    systemProperty("kotlin.test.grouping.engine.simultaneous.methods.group.size", batchSize)
    systemProperty("kotlin.test.grouping.engine.simultaneous.methods.groups", simultaneousBatches)
}
