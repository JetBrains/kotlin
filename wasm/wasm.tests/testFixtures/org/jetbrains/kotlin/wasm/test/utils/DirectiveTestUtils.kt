/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.wasm.test.utils

import org.jetbrains.kotlin.cli.pipeline.web.wasm.WasmCompilationMode.Companion.wasmCompilationMode
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.InTextDirectivesUtils.findLinesWithPrefixesRemoved
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilderBase
import org.jetbrains.kotlin.test.directives.WasmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.impl.testConfiguration
import org.jetbrains.kotlin.test.model.TestFailureSuppressor
import org.jetbrains.kotlin.test.services.*
import org.jetbrains.kotlin.test.utils.WasmIgnoreForConfig
import org.jetbrains.kotlin.wasm.ir.WasmModule
import org.jetbrains.kotlin.wasm.ir.WasmOp
import org.jetbrains.kotlin.wasm.test.handlers.WasiBoxRunner
import org.jetbrains.kotlin.wasm.test.handlers.WasmBoxRunnerBase
import org.jetbrains.kotlin.wasm.test.handlers.WasmVMException
import org.jetbrains.kotlin.wasm.test.tools.WasmVM
import org.junit.Assert.*
import org.junit.runners.model.MultipleFailureException

object DirectiveTestUtils {

    private fun createSimpleDirectiveHandler(
        directive: String,
        processEntryOverride: (module: WasmModule, arguments: ArgumentsHelper) -> Unit,
    ) = object : DirectiveHandler(directive) {
        override fun processEntry(module: WasmModule, arguments: ArgumentsHelper) = processEntryOverride(module, arguments)
    }

    private val FUNCTION_CONTAINS_NO_CALLS = createSimpleDirectiveHandler("WASM_CHECK_CONTAINS_NO_CALLS") { module, arguments ->
        val functionName = arguments.getNamedArgument("inFunction")
        val exceptNames = mutableSetOf<String>()
        val exceptNamesArg = arguments.findNamedArgument("except")
        if (exceptNamesArg != null) {
            for (exceptName in exceptNamesArg.split(";")) {
                exceptNames.add(exceptName.trim())
            }
        }
        checkFunctionContainsNoCalls(module, functionName, exceptNames)
    }

    private val FUNCTION_CALLED_IN_FUNCTION = createSimpleDirectiveHandler("WASM_CHECK_CALLED_IN_FUNCTION") { module, arguments ->
        checkCalledInScope(
            module,
            arguments.getNamedArgument("shouldBeCalled"),
            arguments.getNamedArgument("inFunction"),
        )
    }

    private val INSTRUCTION_IN_FUNCTION = createSimpleDirectiveHandler("WASM_CHECK_INSTRUCTION_IN_FUNCTION") { module, arguments ->
        checkInstructionInScope(
            module,
            arguments.getNamedArgument("instruction"),
            arguments.getNamedArgument("inFunction"),
        )
    }

    private val FUNCTION_NOT_CALLED_IN_FUNCTION = createSimpleDirectiveHandler("WASM_CHECK_NOT_CALLED_IN_FUNCTION") { module, arguments ->
        checkNotCalledInScope(
            module,
            arguments.getNamedArgument("shouldNotBeCalled"),
            arguments.getNamedArgument("inFunction"),
        )
    }

    private val INSTRUCTION_NOT_IN_FUNCTION = createSimpleDirectiveHandler("WASM_CHECK_INSTRUCTION_NOT_IN_FUNCTION") { module, arguments ->
        checkInstructionNotInScope(
            module,
            arguments.getNamedArgument("instruction"),
            arguments.getNamedArgument("inFunction"),
        )
    }

    private val COUNT_INSTRUCTION_IN_FUNCTION = createSimpleDirectiveHandler("WASM_COUNT_INSTRUCTION_IN_FUNCTION") { module, arguments ->
        val instruction = arguments.getNamedArgument("instruction")
        val scopeFunctionName = arguments.getNamedArgument("inFunction")
        val expectedCount = arguments.getNamedArgument("count").toInt()

        val operator = WasmOp.entries.find { it.mnemonic == instruction }!!
        val scopeFunction = WasmIrCheckUtils.getDefinedFunction(module, scopeFunctionName)
        val actualCount = WasmIrCheckUtils.countInstOperator(scopeFunction, operator)

        assertEquals("Count mismatch for instruction `$instruction` in function `$scopeFunctionName`", expectedCount, actualCount)
    }

    private val LOCAL_IN_FUNCTION = createSimpleDirectiveHandler("WASM_CHECK_LOCAL_IN_FUNCTION") { module, arguments ->
        checkLocalInScope(module, arguments, true)
    }

    private val LOCAL_NOT_IN_FUNCTION = createSimpleDirectiveHandler("WASM_CHECK_LOCAL_NOT_IN_FUNCTION") { module, arguments ->
        checkLocalInScope(module, arguments, false)
    }

    private fun checkLocalInScope(module: WasmModule, arguments: ArgumentsHelper, expectExists: Boolean) {
        val localName = arguments.getNamedArgument("name")
        val scopeFunctionName = arguments.getNamedArgument("inFunction")

        val scopeFunction = WasmIrCheckUtils.getDefinedFunction(module, scopeFunctionName)

        var locals = scopeFunction.locals

        // add locals of any (nested) lambdas
        // there will be quite a bit of unnecessary ones in here, but we only need to get the real ones as well.
        // TODO: would be nicer to have a more robust solution here than text search. Somehow getting from the name of the local that holds the lambda, to the lambda's invoke function, that has the actual locals we're searching for
        module.definedFunctions.filter { it.name.contains(Regex("${scopeFunctionName}(\\\$lambda)+\\.invoke")) }
            .forEach { lambda -> locals += lambda.locals }

        val local = locals.find { it.name == localName }

        if (expectExists)
            assertNotNull("Local variable `$localName` *not* found in function `${scopeFunction.name}`", local)
        else
            assertNull("Local variable `$localName` found in function `${scopeFunction.name}`", local)
    }

    private fun checkFunctionContainsNoCalls(module: WasmModule, functionName: String, exceptFunctionNames: Set<String>) {
        val function = WasmIrCheckUtils.getDefinedFunction(module, functionName)
        val counter = WasmIrCheckUtils.countCalls(module, function, exceptFunctionNames)

        val errorMessage = "$functionName contains calls"
        assertEquals(errorMessage, 0, counter)
    }

    private fun checkCalledInScope(
        module: WasmModule,
        functionName: String,
        scopeFunctionName: String,
    ) {
        val errorMessage = "`$functionName` is not called inside `$scopeFunctionName`"
        assertTrue(errorMessage, isCalledInScope(module, functionName, scopeFunctionName))
    }

    private fun checkInstructionInScope(
        module: WasmModule,
        instructionName: String,
        scopeFunctionName: String,
    ) {
        val operator = WasmOp.entries.find { it.mnemonic == instructionName }!!
        val errorMessage = "Instruction `$instructionName` does not appear inside `$scopeFunctionName`"
        assertTrue(errorMessage, isInstructionInScope(module, operator, scopeFunctionName))
    }

    private fun checkNotCalledInScope(
        module: WasmModule,
        functionName: String,
        scopeFunctionName: String,
    ) {
        val errorMessage = "`$functionName` is called inside `$scopeFunctionName`"
        assertFalse(errorMessage, isCalledInScope(module, functionName, scopeFunctionName))
    }

    private fun checkInstructionNotInScope(
        module: WasmModule,
        instructionName: String,
        scopeFunctionName: String,
    ) {
        val operator = WasmOp.entries.find { it.mnemonic == instructionName }!!
        val errorMessage = "Instruction `$instructionName` appears inside `$scopeFunctionName`"
        assertFalse(errorMessage, isInstructionInScope(module, operator, scopeFunctionName))
    }

    private fun isCalledInScope(
        module: WasmModule,
        functionName: String,
        scopeFunctionName: String,
    ): Boolean {
        val scopeFunction = WasmIrCheckUtils.getDefinedFunction(module, scopeFunctionName)

        return WasmIrCheckUtils.countCalls(module, scopeFunction, functionName) != 0
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
        FUNCTION_CALLED_IN_FUNCTION,
        INSTRUCTION_IN_FUNCTION,
        FUNCTION_NOT_CALLED_IN_FUNCTION,
        INSTRUCTION_NOT_IN_FUNCTION,
        COUNT_INSTRUCTION_IN_FUNCTION,
        LOCAL_IN_FUNCTION,
        LOCAL_NOT_IN_FUNCTION
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

    // TODO would be nice to share/replace the old js DiretiveTestUtils.java, as this is also a bit out of place here
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

private class WasmIgnoredTestSuppressorGroup(
    testServices: TestServices,
) : TestFailureSuppressor(testServices) {
    private class WasmIgnoredTestSuppressor(
        private val testServices: TestServices,
        private val ignoreForConfig: WasmIgnoreForConfig,
    ) {

        private val compilerConfiguration = testServices.compilerConfigurationProvider.getCompilerConfiguration(
            testServices.moduleStructure.modules.first(),
            CompilationStage.SECOND
        )

        private val inDebugMode = DebugMode.fromSystemProperty("kotlin.wasm.debugMode") >= DebugMode.DEBUG

        fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> =
            failedAssertions.filter { failedAssertion ->
                // will return "true", i.e., keep this assertion, if any of the specified conditions are NOT met

                if (hasModeMismatch())
                    return@filter true

                if (hasOsMismatch())
                    return@filter true

                if (hasVmMismatchInConcreteFailure(failedAssertion))
                    return@filter true

                // we know that all conditions that were specified are met
                if (inDebugMode)
                    println("------ Suppressing test failure because the test is running with $ignoreForConfig (matches current environment)")

                false
            }

        // With any of the following "has...Mismatch" functions, if the corresponding option is null (meaning it is not specified), then there's no mismatch: null means "any"

        private fun hasVmMismatchInConcreteFailure(
            failedAssertion: WrappedException,
        ): Boolean {
            val expectedVm = ignoreForConfig.vmName
                ?: return false

            // there's at most one exception in here, because the outer suppressor expands groups of exceptions
            // if its not a vm exception, but we do expect a vm to fail, then it's a mismatch, i.e., we keep this failure
            val vmException = failedAssertion.cause as? WasmVMException ?: return true

            return expectedVm != vmException.vmName
        }

        private fun hasOsMismatch(): Boolean {
            val os = ignoreForConfig.os
                ?: return false

            assert(os.lowercase() in listOf("linux", "windows", "mac")) {
                "This is checked in WasmEnvironmentConfigurationDirectives.kt, and should never change from the sanitizing there"
            }

            return !System.getProperty("os.name").startsWith(os, ignoreCase = true)
        }

        private fun hasModeMismatch(): Boolean {
            val mode = ignoreForConfig.mode
                ?: return false

            return compilerConfiguration.wasmCompilationMode() != mode
        }

        fun checkIfTestShouldBeUnmuted() {
            // This function is only called if the whole test succeeded

            // If anything specified to fail *doesn't* match the current environment, then the test succeeding means
            // we can't conclude anything, as the directive may still be needed in other environments.
            // -> check for mismatches with the current environment

            if (hasModeMismatch()) return
            if (hasOsMismatch()) return
            if (hasMismatchInVMsRun()) return

            // All specified conditions match the current environment
            throw AssertionError(
                "Test is expected to fail ($ignoreForConfig), but it passed. Consider unmuting the test, or narrowing the WASM_IGNORE_FOR directive."
            )
        }

        @OptIn(TestInfrastructureInternals::class)
        private val executedVMs: List<WasmVM>
            get() {
                val handlers = testServices.testConfiguration.steps
                    .filterIsInstance<TestStep.HandlersStep<*>>()
                    .flatMap { it.handlers }

                val wasiRunner = handlers.filterIsInstance<WasiBoxRunner>()
                    .singleOrNull()

                val wasmRunner = handlers.filterIsInstance<WasmBoxRunnerBase>()
                    .singleOrNull()

                // not executed in vms
                if (wasiRunner == null && wasmRunner == null)
                    return listOf()

                if (wasiRunner != null)
                    return wasiRunner.vmsToCheck

                if (wasmRunner != null)
                    return wasmRunner.wasmEngines

                throw RuntimeException("Unexpected test runner configuration: neither WASI nor WASM runner found")
            }

        /**
         * Unlike [hasVmMismatchInConcreteFailure], which inspects the VM that produced a [WasmVMException],
         * this variant is used when the test passed (no exception to inspect). If the directive's target VM
         * isn't among the VMs that the test runner actually executes (e.g. `vm=WasmEdge` on a WASM_JS test,
         * which attow only runs V8/SpiderMonkey/JavaScriptCore), we cannot conclude the directive is stale,
         * as the test simply never tried that VM.
         */
        private fun hasMismatchInVMsRun(): Boolean {
            val expectedVm = ignoreForConfig.vmName ?: return false
            return expectedVm !in this.executedVMs.map { it.vmName }
        }
    }


    /**
     * Create one suppressor per WASM_IGNORE_FOR directive.
     * (lazily, because moduleStructure isn't available at construction time)
     */
    private val suppressors: List<WasmIgnoredTestSuppressor> by lazy {
        testServices.moduleStructure
            .allDirectives[WasmEnvironmentConfigurationDirectives.WASM_IGNORE_FOR]
            .map { config ->
                WasmIgnoredTestSuppressor(testServices, config)
            }
    }

    /**
     * As exceptions thrown by the VMs run in WasiBoxRunner/WasmBoxRunner are consolidated into a single exception,
     * we need to expand them back into individual assertions, so they can be properly suppressed individually,
     * and the entire list of exceptions is not accidentally suppressed all at once.
     */
    private fun expandFailedAssertions(failedAssertions: List<WrappedException>): List<WrappedException> {
        return failedAssertions.flatMap { exception ->
            // processed exception causes are either a list of suppressed exceptions, or just the cause itself
            if (exception.cause.suppressedExceptions.isNotEmpty())
                exception.cause.suppressedExceptions.map { exception.withReplacedCause(it) }
            else
                listOf(exception)
        }
    }

    /**
     * Apply suppressors one by one.
     */
    override fun suppressIfNeeded(failedAssertions: List<WrappedException>): List<WrappedException> =
        suppressors.fold(
            expandFailedAssertions(failedAssertions)
        ) { remaining, suppressor ->
            suppressor.suppressIfNeeded(remaining)
        }

    override fun checkIfTestShouldBeUnmuted() {
        val errors = mutableListOf<Throwable>()
        for (suppressor in suppressors) {
            try {
                suppressor.checkIfTestShouldBeUnmuted()
            } catch (e: AssertionError) {
                errors.add(e)
            }
        }
        testServices.assertions.failAll(errors)
    }
}

fun TestConfigurationBuilderBase<*, *>.configureIgnoredTestSuppressor() {
    useFailureSuppressors(::WasmIgnoredTestSuppressorGroup)
}
