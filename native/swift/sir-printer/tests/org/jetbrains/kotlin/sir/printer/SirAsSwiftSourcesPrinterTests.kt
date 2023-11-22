/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.printer

import org.jetbrains.kotlin.sir.*
import org.jetbrains.kotlin.sir.builder.buildFunction
import org.jetbrains.kotlin.sir.builder.buildModule
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter
import org.junit.Test
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

class SirAsSwiftSourcesPrinterTests {

    @Test
    fun `should print simple function`() {
        val module = buildModule {
            declarations.add(buildFunction {
                name = "foo"
                returnType = SirNominalType(BuiltinSirTypeDeclaration.Bool)
                origin = DummyFunctionOrigin()
                visibility = SirVisibility.PUBLIC
            })
        }
        runTest(
            module,
            "native/swift/sir-printer/testData/simple_function"
        )
    }

    @Test
    fun `should print multiple function`() {
        val module = buildModule {
            declarations += buildFunction {
                name = "foo1"
                returnType = SirNominalType(BuiltinSirTypeDeclaration.Bool)
                origin = DummyFunctionOrigin()
                visibility = SirVisibility.PUBLIC
            }

            declarations += buildFunction {
                name = "foo2"
                returnType = SirNominalType(BuiltinSirTypeDeclaration.Bool)
                origin = DummyFunctionOrigin()
                visibility = SirVisibility.PUBLIC
            }
        }

        runTest(
            module,
            "native/swift/sir-printer/testData/simple_multiple_function"
        )
    }

    private fun runTest(module: SirModule, goldenDataFile: String) {
        val expectedSwiftSrc = File(KtTestUtil.getHomeDirectory()).resolve("$goldenDataFile.golden.swift")

        val actualSwiftSrc = SirAsSwiftSourcesPrinter.print(module)
        JUnit5Assertions.assertEqualsToFile(expectedSwiftSrc, actualSwiftSrc)
    }
}