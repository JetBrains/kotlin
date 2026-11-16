/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftimport.standalone

import org.jetbrains.kotlin.kir.KirFunction
import org.jetbrains.kotlin.kir.KirModule
import org.jetbrains.kotlin.sir.SirFunction
import org.jetbrains.kotlin.sir.SirModule
import org.jetbrains.kotlin.sir.SirUnsupportedType
import org.jetbrains.kotlin.sir.SirVisibility
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.kotlin.sir.builder.buildStruct
import org.jetbrains.kotlin.sir.util.addChild
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

    @Test
    fun `top-level functions are imported from swift sources to KIR`() {
        val source = (tempDir / "Lib.swift").apply {
            writeText(
                """
                public func foo() {}
                public func bar() {}
                func hidden() {}
                public struct S {
                    public func member() {}
                }
                """.trimIndent()
            )
        }

        val module = runSwiftImport(SwiftInputModule(name = "Lib", sources = listOf(source))).getOrThrow()

        assertEquals(listOf("foo", "bar"), module.render())
    }

    @Test
    fun `top-level SIR functions are translated to KIR`() {
        val module = runSwiftImport(emptyInputModule, SwiftImportConfig()) { module, _ ->
            buildModule {
                name = module.name
            }.apply {
                addChild { buildFunction { name = "foo"; returnType = SirUnsupportedType } }
                addChild { buildFunction { name = "bar"; returnType = SirUnsupportedType } }
                addChild { buildFunction { name = "internalFunc"; returnType = SirUnsupportedType; visibility = SirVisibility.INTERNAL } }
                addChild { buildStruct { name = "Baz" } }
            }
        }.getOrThrow()

        assertEquals(listOf("foo", "bar"), module.render())
    }

    @Test
    fun `reader failure is reported`() {
        val result = runSwiftImport(emptyInputModule, SwiftImportConfig()) { _, _ -> error("Failed to read") }
        assertTrue(result.isFailure)
    }

    private val emptyInputModule = SwiftInputModule(name = "MyModule", sources = emptyList())

    private fun SirModule.render(): List<String> = declarations.filterIsInstance<SirFunction>().map { it.name }

    private fun KirModule.render(): List<String> = declarations.filterIsInstance<KirFunction>().map { it.name }
}
