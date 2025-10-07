/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.utils

import org.jetbrains.kotlin.test.InTextDirectivesUtils.findLinesWithPrefixesRemoved
import org.jetbrains.kotlin.test.TargetBackend
import org.jetbrains.kotlin.wasm.ir.WasmModule
import org.jetbrains.kotlin.wasm.ir.WasmOp
import org.junit.Assert.*
import org.junit.runners.model.MultipleFailureException

object DirectiveTestUtils {

    private val FUNCTION_CONTAINS_NO_CALLS = object : DirectiveHandler("WASM_CHECK_CONTAINS_NO_CALLS") {

        override fun processEntry(module: WasmModule, arguments: ArgumentsHelper) {
            val exceptNames = mutableSetOf<String>()
            val exceptNamesArg = arguments.findNamedArgument("except")
            if (exceptNamesArg != null) {
                for (exceptName in exceptNamesArg.split(";")) {
                    exceptNames.add(exceptName.trim())
                }
            }
            checkFunctionContainsNoCalls(module, arguments.getFirst(), exceptNames)
        }
    };

    private val FUNCTION_CALLED_IN_SCOPE = object : DirectiveHandler("WASM_CHECK_CALLED_IN_SCOPE") {

        override fun processEntry(module: WasmModule, arguments: ArgumentsHelper) {
            checkCalledInScope(
                module,
                arguments.getNamedArgument("function"),
                arguments.getNamedArgument("scope_function"),
            )
        }
    };

    private val INSTRUCTION_IN_SCOPE = object : DirectiveHandler("WASM_CHECK_INSTRUCTION_IN_SCOPE") {

        override fun processEntry(module: WasmModule, arguments: ArgumentsHelper) {
            checkInstructionInScope(
                module,
                arguments.getNamedArgument("instruction"),
                arguments.getNamedArgument("scope_function"),
            )
        }
    };

    private val FUNCTION_NOT_CALLED_IN_SCOPE = object : DirectiveHandler("WASM_CHECK_NOT_CALLED_IN_SCOPE") {

        override fun processEntry(module: WasmModule, arguments: ArgumentsHelper) {
            checkNotCalledInScope(
                module,
                arguments.getNamedArgument("function"),
                arguments.getNamedArgument("scope_function"),
            )
        }
    };

    private val INSTRUCTION_NOT_IN_SCOPE = object : DirectiveHandler("WASM_CHECK_INSTRUCTION_NOT_IN_SCOPE") {

        override fun processEntry(module: WasmModule, arguments: ArgumentsHelper) {
            checkInstructionNotInScope(
                module,
                arguments.getNamedArgument("instruction"),
                arguments.getNamedArgument("scope_function"),
            )
        }
    };

    private val COUNT_INSTRUCTION_IN_SCOPE = object : DirectiveHandler("WASM_COUNT_INSTRUCTION_IN_SCOPE") {

        override fun processEntry(module: WasmModule, arguments: ArgumentsHelper) {

            val instruction = arguments.getNamedArgument("instruction")
            val scopeFunctionName = arguments.getNamedArgument("scope_function")
            val expectedCount = arguments.getNamedArgument("count").toInt()

            val operator = WasmOp.entries.find { it.mnemonic == instruction }!!
            val scopeFunction = WasmIrCheckUtils.getDefinedFunction(module, scopeFunctionName)
            val actualCount = WasmIrCheckUtils.countInstOperator(scopeFunction, operator)

            assertEquals("Instruction: $instruction", expectedCount, actualCount)
        }
    };

    private fun checkFunctionContainsNoCalls(module: WasmModule, functionName: String, exceptFunctionNames: Set<String>) {
        val function = WasmIrCheckUtils.getDefinedFunction(module, functionName)
        val counter = WasmIrCheckUtils.countCalls(function, exceptFunctionNames)

        val errorMessage = "$functionName contains calls"
        assertEquals(errorMessage, 0, counter)
    }

    private fun checkCalledInScope(
        module: WasmModule,
        functionName: String,
        scopeFunctionName: String,
    ) {
        val errorMessage = "$functionName is not called inside $scopeFunctionName"
        assertTrue(errorMessage, isCalledInScope(module, functionName, scopeFunctionName))
    }

    private fun checkInstructionInScope(
        module: WasmModule,
        instructionName: String,
        scopeFunctionName: String,
    ) {
        val operator = WasmOp.entries.find { it.mnemonic == instructionName }!!
        val errorMessage = "$instructionName is not called inside $scopeFunctionName"
        assertTrue(errorMessage, isInstructionInScope(module, operator, scopeFunctionName))
    }

    private fun checkNotCalledInScope(
        module: WasmModule,
        functionName: String,
        scopeFunctionName: String,
    ) {
        val errorMessage = "$functionName is called inside $scopeFunctionName"
        assertFalse(errorMessage, isCalledInScope(module, functionName, scopeFunctionName))
    }

    private fun checkInstructionNotInScope(
        module: WasmModule,
        instructionName: String,
        scopeFunctionName: String,
    ) {
        val operator = WasmOp.entries.find { it.mnemonic == instructionName }!!
        val errorMessage = "$instructionName is called inside $scopeFunctionName"
        assertFalse(errorMessage, isInstructionInScope(module, operator, scopeFunctionName))
    }

    private fun isCalledInScope(
        module: WasmModule,
        functionName: String,
        scopeFunctionName: String,
    ): Boolean {
        val scopeFunction = WasmIrCheckUtils.getDefinedFunction(module, scopeFunctionName)

        return WasmIrCheckUtils.countCalls(scopeFunction, functionName) != 0
    }

    private fun isInstructionInScope(
        module: WasmModule,
        operator: WasmOp,
        scopeFunctionName: String,
    ): Boolean {
        val scopeFunction = WasmIrCheckUtils.getDefinedFunction(module, scopeFunctionName)

        return WasmIrCheckUtils.countInstOperator(scopeFunction, operator) != 0
    }

    private val DIRECTIVE_HANDLERS = listOf(
        FUNCTION_CONTAINS_NO_CALLS,
        FUNCTION_CALLED_IN_SCOPE,
        INSTRUCTION_IN_SCOPE,
        FUNCTION_NOT_CALLED_IN_SCOPE,
        INSTRUCTION_NOT_IN_SCOPE,
        COUNT_INSTRUCTION_IN_SCOPE,
    )

    @Throws(Exception::class)
    fun processDirectives(
        module: WasmModule,
        sourceCode: String,
        targetBackend: TargetBackend,
    ) {
        val assertionErrors = mutableListOf<Throwable>()
        for (handler in DIRECTIVE_HANDLERS) {
            handler.process(module, sourceCode, targetBackend, assertionErrors)
        }
        MultipleFailureException.assertEmpty(assertionErrors)
    }

    private abstract class DirectiveHandler(directive: String) {

        companion object {
            private val TARGET_BACKENDS = "TARGET_BACKENDS"
            private val IGNORED_BACKENDS = "IGNORED_BACKENDS"

            private fun containsBackend(
                targetBackend: TargetBackend,
                backendsParameterName: String,
                arguments: ArgumentsHelper,
                ifNotSpecified: Boolean,
            ): Boolean {
                val backendsArg = arguments.findNamedArgument(backendsParameterName)
                return if (backendsArg != null) {
                    val backends = backendsArg.split(";")
                    backends.contains(targetBackend.name)
                } else {
                    ifNotSpecified
                }
            }
        }

        private val directive = "// $directive: "

        /**
         * Processes directive entries.
         *
         * Each entry is expected to have the following format:
         * `// DIRECTIVE: arguments
         *
         * @see ArgumentsHelper for arguments format
         */
        @Throws(Exception::class)
        fun process(
            module: WasmModule,
            sourceCode: String,
            targetBackend: TargetBackend,
            assertionErrors: MutableList<Throwable>,
        ) {
            val directiveEntries = findLinesWithPrefixesRemoved(sourceCode, directive)
            for (directiveEntry in directiveEntries) {
                val arguments = ArgumentsHelper(directiveEntry)

                if (!containsBackend(targetBackend, TARGET_BACKENDS, arguments, true) ||
                    containsBackend(targetBackend, IGNORED_BACKENDS, arguments, false)
                ) {
                    continue
                }

                try {
                    processEntry(module, arguments)
                } catch (e: AssertionError) {
                    assertionErrors.add(e)
                }
            }
        }

        abstract fun processEntry(module: WasmModule, arguments: ArgumentsHelper)

        override fun toString(): String = directive
    }

    private class ArgumentsHelper(
        val entry: String,
    ) {
        private val positionalArguments = mutableListOf<String>()
        private val namedArguments = mutableMapOf<String, String>()
        private val argumentsPattern = Regex("""[\w${'$'}_.;]+(=((".*?")|[\w${'$'}_.;]+))?""")

        init {
            for (match in argumentsPattern.findAll(entry)) {
                val argument = match.value
                val keyVal = argument.split("=", limit = 2)
                when (keyVal.size) {
                    1 -> positionalArguments.add(keyVal[0])
                    2 -> {
                        var value = keyVal[1]
                        if (value.startsWith('"') && value.endsWith('"')) {
                            value = value.substring(1, value.length - 1)
                        }
                        namedArguments[keyVal[0]] = value
                    }
                    else -> throw AssertionError("Wrong argument format: $argument")
                }
            }
        }

        fun getFirst(): String = getPositionalArgument(0)

        fun getPositionalArgument(index: Int): String {
            require(positionalArguments.size > index) { "Argument at index `$index` not found in entry: $entry" }
            return positionalArguments[index]
        }

        fun getNamedArgument(name: String): String {
            require(namedArguments.containsKey(name)) { "Argument `$name` not found in entry: $entry" }
            return namedArguments[name]!!
        }

        fun findNamedArgument(name: String): String? = namedArguments[name]
    }
}