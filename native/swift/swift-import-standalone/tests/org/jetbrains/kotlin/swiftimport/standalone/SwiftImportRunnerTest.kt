/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone

import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.swiftimport.standalone.config.SwiftImportConfig
import org.jetbrains.kotlin.swiftimport.standalone.reader.SwiftExtractModuleReader
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.writeText

class SwiftImportRunnerTest {
    @TempDir
    lateinit var tempDir: Path

    fun translateToSIR(module: SwiftInputModule, config: SwiftImportConfig = SwiftImportConfig()) =
        SwiftExtractModuleReader.read(module, config)

    @Test
    fun `top-level functions without parameters are translated to SIR`() {
        val source = (tempDir / "Lib.swift").apply {
            writeText(
                """
                public func foo() {}
                public func bar() {}
                public func withParameters(x: Int) {}
                func hidden() {}
                public struct S {
                    public func member() {}
                }
                """.trimIndent()
            )
        }

        val module = translateToSIR(SwiftInputModule(name = "Lib", sources = listOf(source)))

        assertEquals(listOf("foo", "bar", "withParameters"), module.render())
    }

    private fun SirModule.render(): List<String> = declarations.filterIsInstance<SirFunction>().map { it.name }
}
