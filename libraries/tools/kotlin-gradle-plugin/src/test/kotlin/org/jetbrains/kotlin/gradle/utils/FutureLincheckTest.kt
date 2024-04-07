/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import org.junit.Test

class FutureLincheckTest {
    private val future = CompletableFuture<Int>()

    @Operation
    fun complete(e: Int) = future.complete(e)

    @Operation
    fun isCompleted() = future.isCompleted

    @Operation
    fun value() = runCatching { future.getOrThrow() }.getOrNull()

    @Test
    fun stressTest() = StressOptions().check(this::class)
}

class MappedFutureLincheckTest {
    private val future = CompletableFuture<Int>()
    private val mapped = future.map { "$it" }

    @Operation
    fun complete(e: Int) = future.complete(e)

    @Operation
    fun value() = runCatching { mapped.getOrThrow() }.getOrNull()

    @Test
    fun stressTest() = StressOptions().check(this::class)
}