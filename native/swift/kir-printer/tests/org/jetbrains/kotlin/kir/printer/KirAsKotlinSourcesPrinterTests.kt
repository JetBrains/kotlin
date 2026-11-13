/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kir.printer

import org.jetbrains.kotlin.kir.CFunctionBridge
import org.jetbrains.kotlin.kir.KirFunctionBody
import org.jetbrains.kotlin.kir.KirFunctionBridge
import org.jetbrains.kotlin.kir.KirModule
import org.jetbrains.kotlin.kir.KotlinFunctionBridge
import org.jetbrains.kotlin.kir.SwiftFunctionBridge
import org.jetbrains.kotlin.kir.builder.buildFunction
import org.jetbrains.kotlin.kir.builder.buildModule
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class KirAsKotlinSourcesPrinterTests {

    @Test
    fun `should print empty module`() {
        runTest(buildModule {}, "testData/empty_module")
    }

    @Test
    fun `should print top level function`() {
        val module = buildModule {
            declarations.add(buildFunction { name = "foo" })
        }

        runTest(module, "testData/top_level_function")
    }

    @Test
    fun `should print multiple functions`() {
        val module = buildModule {
            declarations.add(buildFunction { name = "foo1" })
            declarations.add(buildFunction { name = "foo2" })
        }

        runTest(module, "testData/multiple_functions")
    }

    @Test
    fun `should print function body`() {
        val module = buildModule {
            declarations.add(buildFunction {
                name = "foo"
                body = KirFunctionBody(listOf("println(1)", "println(2)"))
            })
        }

        runTest(module, "testData/function_with_body")
    }

    @Test
    fun `should print function with bridge`() {
        val module = moduleWithBridgedFunction()

        runTest(module, "testData/function_with_bridge")
    }

    @Test
    fun `should print swift part of bridges`() {
        val actual = KirPrinter().printSwiftBridges(moduleWithBridgedFunction())

        assertEqualsToFile("testData/function_with_bridge.golden.swift", actual)
    }

    @Test
    fun `should print c part of bridges`() {
        val actual = KirPrinter().printCDeclarations(moduleWithBridgedFunction())

        assertEqualsToFile("testData/function_with_bridge.golden.h", actual)
    }

    @Test
    fun `should print nothing for swift and c parts of module without bridges`() {
        val module = buildModule {
            declarations.add(buildFunction { name = "foo" })
        }

        assertEquals("", KirPrinter().printSwiftBridges(module))
        assertEquals("", KirPrinter().printCDeclarations(module))
    }

    private fun moduleWithBridgedFunction(): KirModule {
        val bridge = KirFunctionBridge(
            name = "Lib_foo",
            kotlinFunctionBridge = KotlinFunctionBridge(
                lines = listOf(
                    "@ImportedBridge(\"Lib_foo\")",
                    "internal external fun Lib_foo(): Unit",
                ),
                packageDependencies = listOf("kotlin.native.internal.ImportedBridge", "kotlinx.cinterop.*"),
            ),
            swiftFunctionBridge = SwiftFunctionBridge(
                lines = listOf(
                    "@_cdecl(\"Lib_foo\")",
                    "package func Lib_foo() -> Void {",
                    "    Lib.foo()",
                    "}",
                ),
            ),
            cDeclarationBridge = CFunctionBridge(
                lines = listOf("void Lib_foo();"),
                headerDependencies = listOf("Foundation/Foundation.h", "stdint.h"),
            ),
        )
        return buildModule {
            declarations.add(buildFunction {
                name = "foo"
                bridges.add(bridge)
                body = KirFunctionBody(listOf("Lib_foo()"))
            })
        }
    }

    private fun runTest(module: KirModule, goldenDataFile: String) {
        assertEqualsToFile("$goldenDataFile.golden.kt", KirPrinter().print(module))
    }

    private fun assertEqualsToFile(path: String, actual: String) {
        val expected = File(KtTestUtil.getHomeDirectory()).resolve(path)
        JUnit5Assertions.assertEqualsToFile(expected, actual)
    }
}
