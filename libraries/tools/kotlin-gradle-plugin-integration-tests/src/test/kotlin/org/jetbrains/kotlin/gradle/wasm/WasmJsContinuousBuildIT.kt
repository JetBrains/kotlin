/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.wasm

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.js.AbstractWebContinuousBuildIT
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.replaceText
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.jupiter.api.Timeout
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

class WasmJsContinuousBuildIT : AbstractWebContinuousBuildIT() {

    @GradleTest
    @TestMetadata("wasm-run-continuous")
    // Timeout is much longer than expected test duration because sometimes KGP needs to download JS tools.
    @Timeout(value = 10, unit = TimeUnit.MINUTES)
    fun testWasmJsRunContinuousBuild(
        gradleVersion: GradleVersion,
    ) {
        doTest(
            gradleVersion,
            "wasm",
            "wasmJs"
        ) {
            assertFileContains(
                it,
                "Hello, world!"
            )

            assertFileContains(
                it,
                "Hello again!!!"
            )
        }
    }
}
