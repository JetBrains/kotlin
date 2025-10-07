/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.utils

import org.jetbrains.kotlin.wasm.ir.*

object WasmIrCheckUtils {

    private fun <T : WasmNamedModuleField> getUnique(found: List<T>, name: String, kind: String): T {
        if (found.size > 1) {
            throw IllegalArgumentException("$name is an ambiguous name for $kind\n ${found.joinToString { it.name }}")
        }
        if (found.isEmpty()) {
            throw IllegalArgumentException("No $kind found: $name")
        }
        return found.first()
    }

    fun getDefinedFunction(module: WasmModule, name: String): WasmFunction.Defined {
        val found = module.definedFunctions.filter { it.name == name }
        return getUnique(found, name, "defined function")
    }

    fun getImportedFunction(module: WasmModule, name: String): WasmFunction.Imported {
        val found = module.importedFunctions.filter { it.name == name }
        return getUnique(found, name, "imported function")
    }

    fun countCalls(module: WasmModule, function: WasmFunction.Defined): Int =
        countCalls(module, function, emptySet())

    private fun countCallsWithPredicate(module: WasmModule, function: WasmFunction.Defined, predicate: (String) -> Boolean): Int {
        return function.instructions.count { inst ->
            inst.operator in setOf(WasmOp.CALL, WasmOp.CALL_INDIRECT) &&
                    module.resolver.resolve(inst.firstImmediateOrNull() as WasmImmediate.FuncIdx).name
                        .let(predicate)
        }
    }

    fun countCalls(module: WasmModule, function: WasmFunction.Defined, exceptFunctionNames: Set<String>): Int {
        return countCallsWithPredicate(module, function) { funcName -> exceptFunctionNames.none { funcName.contains(it) } }
    }

    fun countCalls(module: WasmModule, function: WasmFunction.Defined, targetFunction: String): Int {
        return countCallsWithPredicate(module, function) { funcName -> targetFunction in funcName }
    }

    fun countInstOperator(function: WasmFunction.Defined, operator: WasmOp): Int {
        return function.instructions.count { inst ->
            inst.operator == operator
        }
    }
}