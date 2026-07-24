/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

import org.jetbrains.kotlin.testFederation.SmokeTest
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BinaryCodecTest {
    @SmokeTest
    @Test
    fun core() {
        runSpecTests("core", wasmTestSuitePath, emptyList())
    }

    @Test
    @Disabled
    fun `bulk-memory-operations`() =
        testProposal("bulk-memory-operations")

    @Test
    @Disabled
    fun `exception-handling`() =
        testProposal("exception-handling")

    @Test
    @Disabled
    fun `function-references`() =
        testProposal("function-references")

    @Test
    @Disabled
    fun `reference-types`() =
        testProposal("reference-types", ignoreFiles = listOf("ref_func.wast"))

    @Test
    @Disabled
    fun simd() =
        testProposal("simd")

    @Test
    @Disabled
    fun `tail-call`() =
        testProposal("tail-call")

    @Test
    @Disabled
    fun threads() =
        testProposal("threads")
}
