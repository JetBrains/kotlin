/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.ir.superClasses
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.declarations.moduleDescriptor
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.render

internal class TestsDumper(private val context: Context) : FileLoweringPass {
    private val symbols = context.symbols

    private val baseClassSuite = symbols.baseClassSuite
    private val topLevelSuite = symbols.topLevelSuite

    private fun shouldProcessFile(irFile: IrFile): Boolean =
            irFile.moduleDescriptor in context.sourcesModules // Process test annotations in source libraries too.

    override fun lower(irFile: IrFile) {
        val testDumpFile = context.config.testDumpFile ?: return

        // TODO: uses descriptors.
        if (!shouldProcessFile(irFile)) return

        /* test suite class -> test function names */
        val testCasesToDump = mutableMapOf<String, MutableList<String>>()

        fun registerTestCases(testSuiteName: String, statements: List<IrStatement>) {
            val testCases = testCasesToDump.getOrPut(testSuiteName) { mutableListOf() }
            statements.forEach {
                if (it !is IrCall) return@forEach
                if (it.symbol.owner.name.asString() != "registerTestCase") return@forEach
                val testCaseName = (it.arguments[1] as? IrConst)?.value as? String
                        ?: error("Can't get the test case name: ${it.arguments[1]?.render()}")
                testCases.add(testCaseName)
            }
        }

        for (topLevelField in irFile.declarations.filterIsInstance<IrField>()) {
            val initializer = topLevelField.initializer ?: continue
            (initializer.expression as? IrContainerExpression)?.statements?.forEach {
                when (it) {
                    is IrConstructorCall -> {
                        val constructor = it.symbol.owner
                        if (baseClassSuite !in constructor.constructedClass.superClasses) return@forEach

                        val constructorStatements = (constructor.body as? IrBlockBody)?.statements
                                ?: error("Unexpected body of a test suite constructor: ${constructor.render()}")
                        check(constructorStatements.isNotEmpty()) { "Empty test suite constructor body: ${constructor.render()}" }
                        val delegatingConstructorCall = constructorStatements[0] as? IrDelegatingConstructorCall
                                ?: error("Expected a delegating constructor call for a " +
                                        "test suite constructor ${constructor.render()}: ${constructorStatements[0].render()}")
                        val testSuiteName = (delegatingConstructorCall.arguments[0] as? IrConst)?.value as? String
                                ?: error("Can't get the name of a test suite: ${delegatingConstructorCall.arguments[0]?.render()}")
                        registerTestCases(testSuiteName, constructorStatements.drop(1))
                    }
                    is IrBlock -> {
                        val statements = it.statements
                        if (statements.isEmpty()) return@forEach
                        val variable = statements[0] as? IrVariable ?: return@forEach
                        if (variable.type != topLevelSuite.defaultType) return@forEach

                        val constructorCall = variable.initializer as? IrConstructorCall
                                ?: error("Expected a call to the TopLevelSuite constructor: ${variable.initializer?.render()}")
                        val testSuiteName = (constructorCall.arguments[0] as? IrConst)?.value as? String
                                ?: error("Can't get the name of a test suite: ${constructorCall.arguments[0]?.render()}")
                        registerTestCases(testSuiteName, statements.drop(1))
                    }
                }
            }
        }

        if (testCasesToDump.isEmpty())
            return

        if (!testDumpFile.exists)
            testDumpFile.createNew()
        testDumpFile.appendLines(
                testCasesToDump.flatMap { (suiteName, functionNames) -> functionNames.asSequence().map { "$suiteName:$it" } }
        )
    }
}
