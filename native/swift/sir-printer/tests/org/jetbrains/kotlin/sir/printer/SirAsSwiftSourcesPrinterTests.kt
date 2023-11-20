/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.printer

import org.jetbrains.kotlin.sir.*
import org.jetbrains.sir.printer.SirAsSwiftSourcesPrinter
import org.junit.Test
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File

enum class KotlinBuiltins {
    BOOLEAN,
    BYTE,
    SHORT,
    INT,
    LONG,
    DOUBLE,
    FLOAT,
}

val swiftBuiltIns: Map<KotlinBuiltins, SirType> = mapOf(
    KotlinBuiltins.BOOLEAN to SirType(name = "Bool"),
    KotlinBuiltins.BYTE to SirType(name = "Int8"),
    KotlinBuiltins.SHORT to SirType(name = "Int16"),
    KotlinBuiltins.INT to SirType(name = "Int32"),
    KotlinBuiltins.LONG to SirType(name = "Int64"),
    KotlinBuiltins.DOUBLE to SirType(name = "Double"),
    KotlinBuiltins.FLOAT to SirType(name = "Float"),
)

fun basicModule(): SirModule = SirModule()

class SirAsSwiftSourcesPrinterTests {
    @Test
    fun `should ignore foreign elements`() {
        val module = basicModule()
            .addingForeignFunction()

        runTest(
            module,
            "native/swift/sir-printer/testData/empty"
        )
    }

    @Test
    fun `should print simple function`() {
        val module = basicModule()

        module.declarations.add(
            SirFunction(
                name = "foo",
                parameters = mutableListOf(),
                returnType = swiftBuiltIns[KotlinBuiltins.BOOLEAN]!!
            )
        )

        runTest(
            module,
            "native/swift/sir-printer/testData/simple_function"
        )
    }

    @Test
    fun `should print multiple function`() {
        val module = basicModule()

        module.declarations.add(
            SirFunction(
                name = "foo1",
                parameters = mutableListOf(),
                returnType = swiftBuiltIns[KotlinBuiltins.BOOLEAN]!!
            )
        )
        module.declarations.add(
            SirFunction(
                name = "foo2",
                parameters = mutableListOf(),
                returnType = swiftBuiltIns[KotlinBuiltins.BOOLEAN]!!
            )
        )

        runTest(
            module,
            "native/swift/sir-printer/testData/simple_multiple_function"
        )
    }

    @Test
    fun `should print single argument`() {
        val module = basicModule()

        module.declarations.add(
            SirFunction(
                name = "foo",
                parameters = mutableListOf(
                    SirParameter(
                        name = "arg",
                        type = swiftBuiltIns[KotlinBuiltins.INT]!!
                    )
                ),
                returnType = swiftBuiltIns[KotlinBuiltins.BOOLEAN]!!
            )
        )

        runTest(
            module,
            "native/swift/sir-printer/testData/single_argument"
        )
    }

    @Test
    fun `should print two argument`() {
        val module = basicModule()

        module.declarations.add(
            SirFunction(
                name = "foo",
                parameters = mutableListOf(
                    SirParameter(
                        name = "arg1",
                        type = swiftBuiltIns[KotlinBuiltins.INT]!!
                    ),
                    SirParameter(
                        name = "arg2",
                        type = swiftBuiltIns[KotlinBuiltins.DOUBLE]!!
                    )
                ),
                returnType = swiftBuiltIns[KotlinBuiltins.BOOLEAN]!!
            )
        )

        runTest(
            module,
            "native/swift/sir-printer/testData/two_arguments"
        )
    }

    @Test
    fun `should all types as parameter be handled`() {
        val module = basicModule()
            .addingForeignFunction()

        module.declarations.add(
            SirFunction(
                name = "foo1",
                parameters = mutableListOf(
                    SirParameter(
                        name = "arg1",
                        type = swiftBuiltIns[KotlinBuiltins.BOOLEAN]!!
                    ),
                    SirParameter(
                        name = "arg2",
                        type = swiftBuiltIns[KotlinBuiltins.BYTE]!!
                    ),
                    SirParameter(
                        name = "arg3",
                        type = swiftBuiltIns[KotlinBuiltins.SHORT]!!
                    ),
                    SirParameter(
                        name = "arg4",
                        type = swiftBuiltIns[KotlinBuiltins.INT]!!
                    ),
                    SirParameter(
                        name = "arg5",
                        type = swiftBuiltIns[KotlinBuiltins.LONG]!!
                    ),
                    SirParameter(
                        name = "arg6",
                        type = swiftBuiltIns[KotlinBuiltins.DOUBLE]!!
                    ),
                    SirParameter(
                        name = "arg7",
                        type = swiftBuiltIns[KotlinBuiltins.FLOAT]!!
                    ),
                ),
                returnType = swiftBuiltIns[KotlinBuiltins.BOOLEAN]!!
            )
        )

        runTest(
            module,
            "native/swift/sir-printer/testData/all_types_argument"
        )
    }

    private fun runTest(module: SirModule, goldenDataFile: String) {
        val expectedSwiftSrc = File(KtTestUtil.getHomeDirectory()).resolve("$goldenDataFile.golden.swift")

        val actualSwiftSrc = SirAsSwiftSourcesPrinter.print(module)
        JUnit5Assertions.assertEqualsToFile(expectedSwiftSrc, actualSwiftSrc)
    }

    private fun SirModule.addingForeignFunction(): SirModule {
        declarations.add(SirForeignFunction(listOf("foo")))
        return this
    }
}