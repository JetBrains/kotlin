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
import kotlin.random.*

fun buildProgram(programId: ProgramId): Program = when (programId) {
    is ProgramId.Initial -> generate(programId.seed)
}

fun AbstractNativeSimpleTest.execute(id: ProgramId) {
    val name = id.toString()
    val dslGeneratedDir = resolveDslDir(name)
    val output = buildProgram(id).translate()
    output.save(dslGeneratedDir)
    runDSL(
        name,
        output,
        testRunSettings.get<Timeouts>().executionTimeout
    )
}

class SimpleFuzzer(seed: Int = 0) {
    private val random = Random(seed)
    fun nextStepId() = ProgramId.Initial(random.nextUInt())
}
