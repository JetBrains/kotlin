/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dumpKotlinLike
import org.jetbrains.kotlin.js.test.converters.LoweredJsIrBackendInput
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * Dumps [JsControlFlowGraph] for each function with a body to a golden `.cfg.txt`.
 */
class JsControlFlowGraphHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    private val builder = StringBuilder()

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (module != testServices.moduleStructure.modules.last()) return
        check(info is LoweredJsIrBackendInput) {
            "Expected LoweredJsIrBackendInput, got ${info::class.simpleName}"
        }
        val index = with(info.context) { buildJsProgramIndex(info.allModules) }
        for (entry in index.functions.entries) {
            dumpFunction(entry.key, entry.value.cfg)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val testFile = testServices.moduleStructure.originalTestDataFiles.first()
        val dumpFile = testFile.resolveSibling("${testFile.nameWithoutExtension}.cfg.txt")
        testServices.assertions.assertEqualsToFile(dumpFile, builder.toString())
    }

    private fun dumpFunction(function: IrFunction, cfg: JsControlFlowGraph) {
        builder.appendLine("// FUNCTION: ${function.name.asString()}")
        val unsupportedConstruct = cfg.unsupportedConstruct
        if (unsupportedConstruct != null) {
            builder.appendLine("// unsupportedConstruct: ${unsupportedConstruct::class.simpleName}")
        }
        builder.appendLine("// blocks=${cfg.blocks.size} entry=BB${cfg.entry.id}")
        for (block in cfg.blocks) {
            builder.appendLine("BB${block.id}:")
            for (statement in block.statements) {
                builder.appendLine("  ${renderStatement(statement)}")
            }
            builder.appendLine("  terminator: ${renderTerminator(block.terminator)}")
            if (block.successors.isNotEmpty()) {
                builder.appendLine("  succs: ${block.successors.joinToString { "BB${it.id}" }}")
            }
        }
        builder.appendLine()
    }

    private fun renderStatement(statement: IrStatement): String =
        statement.dumpKotlinLike().lines().joinToString("\\n") { it.trim() }

    private fun renderTerminator(terminator: JsTerminator): String = when (terminator) {
        is JsTerminator.Goto -> "Goto(BB${terminator.target.id})"
        is JsTerminator.Cond ->
            "Cond(${renderExpr(terminator.condition)}, then=BB${terminator.thenTarget.id}, else=BB${terminator.elseTarget.id})"
        is JsTerminator.MultiCond -> {
            val arms = terminator.branches.joinToString {
                val cond = it.condition?.let(::renderExpr) ?: "else"
                "$cond -> BB${it.target.id}"
            }
            "MultiCond($arms)"
        }
        is JsTerminator.Return -> "Return(${terminator.value?.let(::renderExpr) ?: "Unit"})"
        is JsTerminator.Throw -> "Throw(${renderExpr(terminator.value)})"
        JsTerminator.Unreachable -> "Unreachable"
    }

    private fun renderExpr(expression: IrExpression): String =
        expression.dumpKotlinLike().lines().joinToString(" ") { it.trim() }.take(120)
}
