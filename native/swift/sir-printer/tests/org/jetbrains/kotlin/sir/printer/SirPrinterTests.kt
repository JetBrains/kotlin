/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir.printer

import org.jetbrains.kotlin.sir.*
import org.jetbrains.sir.printer.SirPrinter
import org.junit.Test
import kotlin.test.assertEquals

enum class Builtins {
    INT, DOUBLE, VOID
}

val swiftBuiltIns: Map<Builtins, SirType> = mapOf(
    Builtins.INT to SirType(name = "Int32"),
    Builtins.DOUBLE to SirType(name = "Double"),
    Builtins.VOID to SirType(name = "Void")
)

fun basicModule(): SirModule = SirModule().apply {
    swiftBuiltIns.values.forEach { declarations.add(it) }
}

class SirPrinterTests {
    @Test
    fun `should ignore foreign elements`() {
        val module = basicModule()
            .addingForeignFunction()

        val expected = ""
        runTest(module, expected)
    }

    @Test
    fun `should print simple function`() {
        val module = basicModule()

        module.declarations.add(
            SirFunction(
                name = "foo",
                arguments = mutableListOf(),
                returnType = swiftBuiltIns[Builtins.VOID]!!
            )
        )

        val expected = "func foo() -> Void { fatalError() }"

        runTest(module, expected)
    }

    @Test
    fun `should print function returning Double`() {
        val module = basicModule()

        module.declarations.add(
            SirFunction(
                name = "foo",
                arguments = mutableListOf(),
                returnType = swiftBuiltIns[Builtins.DOUBLE]!!
            )
        )

        val expected = "func foo() -> Double { fatalError() }"

        runTest(module, expected)
    }

    @Test
    fun `should print single argument`() {
        val module = basicModule()

        module.declarations.add(
            SirFunction(
                name = "foo",
                arguments = mutableListOf(
                    SirParameter(
                        name = "arg",
                        type = swiftBuiltIns[Builtins.INT]!!
                    )
                ),
                returnType = swiftBuiltIns[Builtins.VOID]!!
            )
        )

        val expected =
            """
                |func foo(
                |    arg: Int32
                |) -> Void { fatalError() }
            """
                .trimMargin()

        runTest(module, expected)
    }

    @Test
    fun `should print couple argument`() {
        val module = basicModule()

        module.declarations.add(
            SirFunction(
                name = "foo",
                arguments = mutableListOf(
                    SirParameter(
                        name = "arg1",
                        type = swiftBuiltIns[Builtins.INT]!!
                    ),
                    SirParameter(
                        name = "arg2",
                        type = swiftBuiltIns[Builtins.DOUBLE]!!
                    )
                ),
                returnType = swiftBuiltIns[Builtins.VOID]!!
            )
        )

        val expected =
            """
                |func foo(
                |    arg1: Int32,
                |    arg2: Double
                |) -> Void { fatalError() }
            """
                .trimMargin()

        runTest(module, expected)
    }

    @Test
    fun `should everything`() {
        val module = basicModule()
            .addingForeignFunction()

        module.declarations.add(
            SirFunction(
                name = "foo",
                arguments = mutableListOf(
                    SirParameter(
                        name = "arg1",
                        type = swiftBuiltIns[Builtins.INT]!!
                    ),
                    SirParameter(
                        name = "arg2",
                        type = swiftBuiltIns[Builtins.DOUBLE]!!
                    )
                ),
                returnType = swiftBuiltIns[Builtins.INT]!!
            )
        )

        val expected =
            """
                |func foo(
                |    arg1: Int32,
                |    arg2: Double
                |) -> Int32 { fatalError() }
            """
                .trimMargin()

        runTest(module, expected)
    }

    private fun runTest(module: SirModule, expected: String) = assertEquals(
        expected,
        SirPrinter.print(module)
    )

    private fun SirModule.addingForeignFunction(): SirModule {
        declarations.add(SirForeignFunction(listOf("foo")))
        return this
    }
}