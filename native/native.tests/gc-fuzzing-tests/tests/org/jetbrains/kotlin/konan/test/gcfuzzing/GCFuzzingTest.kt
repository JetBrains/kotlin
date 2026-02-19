/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing

import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.gcfuzzing.fuzzer.*
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.util.stream.Stream
import kotlin.time.TimeSource
import kotlin.streams.asStream
import kotlin.test.fail
import kotlin.time.Duration

class GCFuzzingTest : AbstractNativeSimpleTest() {

    @TestFactory
    fun simple(): Stream<DynamicTest> {
        val executeSingleId = System.getProperty("gcfuzzing.single.id")
        if (executeSingleId != null) {
            val id = ProgramId.fromString(executeSingleId) ?: fail("Invalid program ID \"$executeSingleId\"")
            return listOf(DynamicTest.dynamicTest("$id") {
                execute(id)
            }).stream()
        }


        val timelimit = Duration.parse(System.getProperty("gcfuzzing.timelimit")!!)
        val start = TimeSource.Monotonic.markNow()

        val seed = System.getProperty("gcfuzzing.seed")!!.toInt()
        val fuzzer = SimpleFuzzer(seed)
        var stepCounter = 0
        return generateSequence {
            if (start.elapsedNow() > timelimit) return@generateSequence null
            val stepId = fuzzer.nextStepId()
            DynamicTest.dynamicTest("SimpleFuzzer($seed) step #${stepCounter++} ($stepId)") {
                execute(stepId)
            }
        }.asStream()
    }

}