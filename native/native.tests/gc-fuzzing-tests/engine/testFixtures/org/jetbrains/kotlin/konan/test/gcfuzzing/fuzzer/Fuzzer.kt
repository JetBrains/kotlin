/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.test.gcfuzzing.fuzzer

import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Program
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.translate
import org.jetbrains.kotlin.konan.test.gcfuzzing.execution.*
import org.jetbrains.kotlin.konan.test.blackbox.AbstractNativeSimpleTest
import org.jetbrains.kotlin.konan.test.blackbox.support.settings.Timeouts
import org.jetbrains.kotlin.konan.test.gcfuzzing.dsl.Config
import kotlin.random.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

fun buildProgram(programId: ProgramId): Program = when (programId) {
    is ProgramId.Initial -> generate(programId.seed)
}

fun AbstractNativeSimpleTest.execute(id: ProgramId) {
    val name = id.toString()
    val dslGeneratedDir = resolveDslDir(name)
    val output = buildProgram(id).translate(
        Config(
            maximumStackDepth = Config.DEFAULT.maximumStackDepth,
            mainLoopRepeatCount = Config.DEFAULT.mainLoopRepeatCount,
            maxThreadCount = Config.DEFAULT.maxThreadCount,
            memoryPressureHazardZoneBytes = Config.DEFAULT.memoryPressureHazardZoneBytes,
            memoryPressureCheckInterval = Config.DEFAULT.memoryPressureCheckInterval,
            softTimeout = System.getProperty("gcfuzzing.softTimeout")?.let { Duration.parse(it) } ?: Config.DEFAULT.softTimeout,
            hardTimeout = testRunSettings.get<Timeouts>().executionTimeout,
        )
    )
    output.save(dslGeneratedDir)
    runDSL(
        name,
        output,
        testRunSettings.get<Timeouts>().executionTimeout + 1.minutes
    )
}

class SimpleFuzzer(seed: Int = 0) {
    private val random = Random(seed)
    fun nextStepId() = ProgramId.Initial(random.nextUInt())
}
