/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimizations.dataflow

import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.js.test.converters.LoweredJsIrBackendInput
import org.jetbrains.kotlin.test.backend.handlers.AbstractIrHandler
import org.jetbrains.kotlin.test.backend.ir.IrBackendInput
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.assertions
import org.jetbrains.kotlin.test.services.moduleStructure

/**
 * Runs [analyzeJsProgram] and dumps per-function summaries to a golden `.facts.txt`.
 */
@OptIn(UnsafeDuringIrConstructionAPI::class)
class JsConcreteAnalysisHandler(testServices: TestServices) : AbstractIrHandler(testServices) {
    private val builder = StringBuilder()

    override fun processModule(module: TestModule, info: IrBackendInput) {
        if (module != testServices.moduleStructure.modules.last()) return
        check(info is LoweredJsIrBackendInput) {
            "Expected LoweredJsIrBackendInput, got ${info::class.simpleName}"
        }
        val result = with(info.context) { analyzeJsProgram(info.allModules) }
        for (entry in result.functionFacts.entries) {
            dumpFunction(entry.key, entry.value)
        }
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val testFile = testServices.moduleStructure.originalTestDataFiles.first()
        val dumpFile = testFile.resolveSibling("${testFile.nameWithoutExtension}.facts.txt")
        testServices.assertions.assertEqualsToFile(dumpFile, builder.toString())
    }

    private fun dumpFunction(function: IrFunction, facts: JsFunctionFacts) {
        builder.appendLine("// FUNCTION: ${function.name.asString()}")
        val unsupportedConstruct = facts.cfg.unsupportedConstruct
        if (unsupportedConstruct != null) {
            builder.appendLine("// unsupportedConstruct: ${unsupportedConstruct::class.simpleName}")
        }
        val values = linkedSetOf<IrValueDeclaration>()
        values.addAll(function.parameters)
        values.addAll(facts.allSummaries().keys)
        val named = values.sortedBy { it.name.asString() }
        for (value in named) {
            val fact = facts.summary(value)
            if (fact == JsFact.Bottom) continue
            val label = valueLabel(value)
            builder.appendLine("$label: value=${formatValue(fact.value)} type=${formatType(fact.type)}")
        }
        builder.appendLine()
    }

    private fun valueLabel(value: IrValueDeclaration): String = when (value) {
        is IrVariable -> "local ${value.name.asString()}"
        else -> "param ${value.name.asString()}"
    }

    private fun formatValue(value: JsValueLattice): String = when (value) {
        JsValueLattice.Top -> "Top"
        JsValueLattice.Bottom -> "Bottom"
        JsValueLattice.Unit -> "Unit"
        is JsValueLattice.Enum -> value.toString()
        is JsValueLattice.Const -> when (value.kind) {
            IrConstKind.Null -> "null"
            IrConstKind.Boolean -> value.value.toString()
            IrConstKind.Char -> "'${value.value}'"
            IrConstKind.String -> "\"${value.value}\""
            IrConstKind.Byte, IrConstKind.Short, IrConstKind.Int, IrConstKind.Long ->
                "${value.value}:${value.kind.asString}"
            else -> "${value.value}:${value.kind.asString}"
        }
    }

    private fun formatType(type: JsTypeLattice): String = when (type) {
        JsTypeLattice.Top -> "Top"
        JsTypeLattice.Bottom -> "Bottom"
        is JsTypeLattice.Exact -> {
            val nullability = if (type.nullable) "?" else ""
            val final = if (type.isFinal) " final" else ""
            "Exact(${type.irClass.name.asString()}$nullability)$final"
        }
        is JsTypeLattice.UpperBound -> {
            val nullability = if (type.nullable) "?" else ""
            "UpperBound(${type.irClass.name.asString()}$nullability)"
        }
    }
}
