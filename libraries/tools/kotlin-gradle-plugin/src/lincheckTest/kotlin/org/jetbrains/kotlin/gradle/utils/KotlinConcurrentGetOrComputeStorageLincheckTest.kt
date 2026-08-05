/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.jetbrains.kotlin.gradle.cache.KotlinConcurrentGetOrComputeStorage
import org.jetbrains.kotlinx.lincheck.annotations.Operation
import org.jetbrains.kotlinx.lincheck.check
import org.jetbrains.kotlinx.lincheck.strategy.stress.StressOptions
import kotlin.test.Test

class KotlinConcurrentGetOrComputeStorageLincheckTest {
    private val storage = KotlinConcurrentGetOrComputeStorage()

    @Operation
    fun getOrPut(key: String, value: String): Any = storage.getOrCompute(key) { value }

    @Test
    fun stressTest() = StressOptions().check(this::class)
}
