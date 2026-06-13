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
