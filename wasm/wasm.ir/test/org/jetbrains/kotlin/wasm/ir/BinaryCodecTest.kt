/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.ir

import org.junit.Ignore
import org.junit.Test

class BinaryCodecTest {
    @Test
    @Ignore
    fun core() {
        runSpecTests("core", wasmTestSuitePath, emptyList())
    }

    @Test
    @Ignore
    fun `bulk-memory-operations`() =
        testProposal("bulk-memory-operations")

    @Test
    @Ignore
    fun `exception-handling`() =
        testProposal("exception-handling")

    @Test
    @Ignore
    fun `function-references`() =
        testProposal("function-references")

    @Test
    @Ignore
    fun `reference-types`() =
        testProposal("reference-types", ignoreFiles = listOf("ref_func.wast"))

    @Test
    @Ignore
    fun simd() =
        testProposal("simd")

    @Test
    @Ignore
    fun `tail-call`() =
        testProposal("tail-call")

    @Test
    @Ignore
    fun threads() =
        testProposal("threads")
}
