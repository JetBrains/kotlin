/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.inline.clean.*
import org.jetbrains.kotlin.js.inline.context.FunctionContext
import org.jetbrains.kotlin.js.inline.context.InliningContext
import org.jetbrains.kotlin.js.inline.util.*
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata
import org.jetbrains.kotlin.resolve.inline.InlineStrategy

import java.util.*

import org.jetbrains.kotlin.js.inline.util.getImportTag
import org.jetbrains.kotlin.js.inline.util.IdentitySet
import org.jetbrains.kotlin.js.translate.declaration.transformSpecialFunctionsToCoroutineMetadata
import org.jetbrains.kotlin.js.translate.general.AstGenerationResult
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.flattenStatement
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn

class JsInliner private constructor(
    private val config: JsConfig,
    private val functions: Map<JsName, FunctionWithWrapper>,
    private val accessors: Map<String, FunctionWithWrapper>,
    private val inverseNameBindings: Map<JsName, String>,
    private val functionReader: FunctionReader,
    private val trace: DiagnosticSink,
    private val moduleMap: Map<JsName, JsImportedModule>
) {
    private val namedFunctionsSet: MutableSet<JsFunction> = functions.values.mapTo(IdentitySet()) { it.function }

    private val processedFunctions = IdentitySet<JsFunction>()
    private val inProcessFunctions = IdentitySet<JsFunction>()

    private val functionsByWrapperNodes = HashMap<JsBlock, FunctionWithWrapper>()
    private val functionsByFunctionNodes = HashMap<JsFunction, FunctionWithWrapper>()

    init {
        (functions.values.asSequence() + accessors.values.asSequence()).forEach { f ->
            functionsByFunctionNodes[f.function] = f
            if (f.wrapperBody != null) {
                functionsByWrapperNodes[f.wrapperBody] = f
            }
        }
    }

    // these are needed for error reporting, when inliner detects cycle
    private val namedFunctionsStack = Stack<JsFunction>()
    private val inlineCallInfos = LinkedList<JsCallInfo>()

    private val currentNamedFunction: JsFunction?
        get() = if (namedFunctionsStack.empty()) null else namedFunctionsStack.peek()

    private inner class InlinerImpl(
        val existingNameBindings: MutableMap<JsName, String>,
        val existingImports: MutableMap<String, JsName>,
        val inlineFunctionDepth: Int,
        val addPrevious: (JsStatement) -> Unit
    ) : JsVisitorWithContextImpl() {

        val replacementsInducedByWrappers = HashMap<JsFunction, Map<JsName, JsNameRef>>()

        val inlinedModuleAliases = HashSet<JsName>()

        val additionalNameBindings = ArrayList<JsNameBinding>()

        override fun visit(function: JsFunction, context: JsContext<*>): Boolean {
            val functionWithWrapper = functionsByFunctionNodes[function]
            if (functionWithWrapper != null) {
                visit(functionWithWrapper)
                return false
            } else {
                startFunction(function)
                return super.visit(function, context)
            }
        }

        override fun endVisit(function: JsFunction, context: JsContext<*>) {
            super.endVisit(function, context)
            if (!functionsByFunctionNodes.containsKey(function)) {
                endFunction(function)
            }
        }


        private fun startFunction(function: JsFunction) {
            assert(!inProcessFunctions.contains(function)) { "Inliner has revisited function" }
            inProcessFunctions.add(function)

            if (namedFunctionsSet.contains(function)) {
                namedFunctionsStack.push(function)
            }
        }

        private fun endFunction(function: JsFunction) {
            refreshLabelNames(function.body, function.scope)

            removeUnusedLocalFunctionDeclarations(function)
            processedFunctions.add(function)

            FunctionPostProcessor(function).apply()

            assert(inProcessFunctions.contains(function))
            inProcessFunctions.remove(function)

            if (!namedFunctionsStack.empty() && namedFunctionsStack.peek() == function) {
                namedFunctionsStack.pop()
            }
        }

        override fun visit(x: JsBlock, ctx: JsContext<*>): Boolean {
            val functionWithWrapper = functionsByWrapperNodes[x]
            if (functionWithWrapper != null) {
                visit(functionWithWrapper)
                return false
            }
            return super.visit(x, ctx)
        }

        private fun visit(functionWithWrapper: FunctionWithWrapper) {
            startFunction(functionWithWrapper.function)

            val wrapperBody = functionWithWrapper.wrapperBody
            if (wrapperBody != null) {
//                statementContexts.push(ListContext())

                val existingImports = HashMap<String, JsName>()

                for (statement in wrapperBody.statements) {
                    if (statement is JsVars) {
                        val tag = getImportTag(statement)
                        if (tag != null) {
                            existingImports[tag] = statement.vars[0].name
                        }
                    }
                }

                val additionalStatements = mutableListOf<JsStatement>()
                val innerInliner = InlinerImpl(existingNameBindings, existingImports, inlineFunctionDepth + 1) {
                    additionalStatements.add(it)
                }
                for (statement in wrapperBody.statements) {
                    if (statement !is JsReturn) {
                        innerInliner.acceptStatement(statement)
                    } else {
                        innerInliner.accept((statement.expression as JsFunction).body)
                    }
                }

//                innerInliner.acceptStatement(wrapperBody)
                // TODO keep order
                wrapperBody.statements.addAll(0, additionalStatements)

//                statementContexts.pop()
            } else {
                accept(functionWithWrapper.function.body)
            }

            endFunction(functionWithWrapper.function)
        }

        override fun visit(call: JsInvocation, context: JsContext<*>): Boolean {
            if (!hasToBeInlined(call)) return true

            currentNamedFunction?.let {
                inlineCallInfos.add(JsCallInfo(call, it))
            }

            val definition = functionContext.getFunctionDefinition(call)

            if (inProcessFunctions.contains(definition.function)) {
                reportInlineCycle(call, definition.function)
            } else if (!processedFunctions.contains(definition.function)) {
                for (i in 0 until call.arguments.size) {
                    val argument = call.arguments[i]
                    call.arguments[i] = accept(argument)
                }
                visit(definition)
                return false
            }

            return true
        }

        override fun endVisit(x: JsInvocation, ctx: JsContext<JsNode>) {
            if (hasToBeInlined(x)) {
                inline(x, ctx)
            }
            if (!inlineCallInfos.isEmpty()) {
                if (inlineCallInfos.last.call == x) {
                    inlineCallInfos.removeLast()
                }
            }
        }

        override fun endVisit(x: JsExpressionStatement, ctx: JsContext<*>) {
            val e = x.expression
            if (e is JsBinaryOperation) {
                if (e.operator == JsBinaryOperator.ASG) {
                    e.arg2?.let { argument2 ->
                    val splitSuspendInlineFunction = splitExportedSuspendInlineFunctionDeclarations(argument2)
                    if (splitSuspendInlineFunction != null) {
                        e.arg2 = splitSuspendInlineFunction
                    }
                }
            }}

            super.endVisit(x, ctx)
        }

        override fun endVisit(x: JsVars.JsVar, ctx: JsContext<*>) {
            val initExpression = x.initExpression ?: return

            val splitSuspendInlineFunction = splitExportedSuspendInlineFunctionDeclarations(initExpression)
            if (splitSuspendInlineFunction != null) {
                x.initExpression = splitSuspendInlineFunction
            }
        }

        private fun splitExportedSuspendInlineFunctionDeclarations(expression: JsExpression): JsFunction? {
            val inlineMetadata = InlineMetadata.decompose(expression)
            if (inlineMetadata != null) {
                val (originalFunction, wrapperBody) = inlineMetadata.function
                if (originalFunction.coroutineMetadata != null) {
                    val statementContext = lastStatementLevelContext

                    // This function will be exported to JS
                    val function = originalFunction.deepCopy()

                    // Original function should be not be transformed into a state machine
                    originalFunction.setName(null)
                    originalFunction.coroutineMetadata = null
                    originalFunction.isInlineableCoroutineBody = true
                    if (wrapperBody != null) {
                        // Extract local declarations
                        applyWrapper(wrapperBody, function, originalFunction)
                    }

                    // Keep the `defineInlineFunction` for the inliner to find
                    statementContext.addNext(expression.makeStmt())

                    // Return the function body to be used without inlining.
                    return function
                }
            }
            return null
        }

        override fun doAcceptStatementList(statements: MutableList<JsStatement>) {
            var i = 0

            while (i < statements.size) {
                val additionalStatements =
                    ExpressionDecomposer.preserveEvaluationOrder(statements[i]) { node -> node is JsInvocation && hasToBeInlined(node) }
                statements.addAll(i, additionalStatements)
                i += additionalStatements.size + 1
            }

            super.doAcceptStatementList(statements)
        }

        private fun inline(call: JsInvocation, context: JsContext<JsNode>) {
            var functionWithWrapper = functionContext.getFunctionDefinition(call)

            // Since we could get functionWithWrapper as a simple function directly from staticRef (which always points on implementation)
            // we should check if we have a known wrapper for it
            functionsByFunctionNodes[functionWithWrapper.function]?.let {
                functionWithWrapper = it
            }

            val function = functionWithWrapper.function.deepCopy()
            function.body = transformSpecialFunctionsToCoroutineMetadata(function.body)
            if (functionWithWrapper.wrapperBody != null) {
                applyWrapper(functionWithWrapper.wrapperBody!!, function, functionWithWrapper.function)
            }

            val inliningContext = InliningContext(lastStatementLevelContext)

            val inlineableResult = FunctionInlineMutator.getInlineableCallReplacement(call, function, inliningContext)

            val inlineableBody = inlineableResult.inlineableBody
            var resultExpression = inlineableResult.resultExpression
            val statementContext = inliningContext.statementContext
            // body of inline function can contain call to lambdas that need to be inlined
            val inlineableBodyWithLambdasInlined = accept(inlineableBody)
            assert(inlineableBody === inlineableBodyWithLambdasInlined)

            // Support non-local return from secondary constructor
            // Returns from secondary constructors should return `$this` object.
            val currentFunction = currentNamedFunction
            if (currentFunction != null) {
                val returnVariable = currentFunction.forcedReturnVariable
                if (returnVariable != null) {
                    inlineableBody.accept(object : RecursiveJsVisitor() {
                        override fun visitReturn(x: JsReturn) {
                            x.expression = returnVariable.makeRef()
                        }
                    })
                }
            }

            statementContext.addPrevious(flattenStatement(inlineableBody))

            /*
             * Assumes, that resultExpression == null, when result is not needed.
             * @see FunctionInlineMutator.isResultNeeded()
             */
            if (resultExpression == null) {
                statementContext.removeMe()
                return
            }

            resultExpression = accept(resultExpression)
            resultExpression.synthetic = true
            context.replaceMe(resultExpression)
        }


        private fun applyWrapper(
            wrapper: JsBlock, function: JsFunction, originalFunction: JsFunction
        ) {

            // TODO Decrypt the comment below
            // Apparently we should avoid this trick when we implement fair support for crossinline
            val replacements = replacementsInducedByWrappers.computeIfAbsent(originalFunction) { k ->
                val newReplacements = HashMap<JsName, JsNameRef>()

                val copiedStatements = ArrayList<JsStatement>()
                wrapper.statements.asSequence()
                    .filterNot { it is JsReturn }
                    .map { it.deepCopy() }
                    .forEach { statement ->
                        if (inlineFunctionDepth == 0) {
                            replaceExpressionsWithLocalAliases(statement)
                        }

                        if (statement is JsVars) {
                            val tag = getImportTag(statement)
                            if (tag != null) {
                                val name = statement.vars[0].name
                                var existingName: JsName? = if (inlineFunctionDepth == 0) name.localAlias else null
                                if (existingName == null) {
                                    existingName = existingImports.computeIfAbsent(tag) {
                                        copiedStatements.add(statement)
                                        val alias = JsScope.declareTemporaryName(name.ident)
                                        alias.copyMetadataFrom(name)
                                        newReplacements[name] = pureFqn(alias, null)
                                        alias
                                    }
                                }

                                if (name !== existingName) {
                                    val replacement = pureFqn(existingName, null)
                                    newReplacements[name] = replacement
                                }

                                return@forEach
                            }
                        }

                        copiedStatements.add(statement)
                    }

                val definedNames = copiedStatements.asSequence()
                    .flatMap { node -> collectDefinedNamesInAllScopes(node).asSequence() }
                    .filter { name -> !newReplacements.containsKey(name) }
                    .toSet()
                for (name in definedNames) {
                    val alias = JsScope.declareTemporaryName(name.ident)
                    alias.copyMetadataFrom(name)
                    val replacement = pureFqn(alias, null)
                    newReplacements[name] = replacement
                }

                for (statement in copiedStatements) {
                    addPrevious(accept(replaceNames(statement, newReplacements)))
                }

                for ((key, value) in collectNamedFunctions(JsBlock(copiedStatements))) {
                    if (key.staticRef is JsFunction) {
                        key.staticRef = value
                    }
                }

                newReplacements
            }

            replaceNames(function, replacements)

            // Copy nameBinding's for inlined localAlias'es
            for (nameRef in replacements.values) {
                val name = nameRef.name
                if (name != null && !existingNameBindings.containsKey(name)) {
                    val tag = inverseNameBindings[name]
                    if (tag != null) {
                        existingNameBindings[name] = tag
                        additionalNameBindings.add(JsNameBinding(tag, name))
                    }
                }
            }
        }

        private fun replaceExpressionsWithLocalAliases(statement: JsStatement) {
            object : JsVisitorWithContextImpl() {
                override fun endVisit(x: JsNameRef, ctx: JsContext<JsNode>) {
                    replaceIfNecessary(x, ctx)
                }

                override fun endVisit(x: JsArrayAccess, ctx: JsContext<JsNode>) {
                    replaceIfNecessary(x, ctx)
                }

                private fun replaceIfNecessary(expression: JsExpression, ctx: JsContext<JsNode>) {
                    val alias = expression.localAlias
                    if (alias != null) {
                        ctx.replaceMe(alias.makeRef())
                        inlinedModuleAliases.add(alias)
                    }
                }

            }.accept(statement)
        }

        private fun hasToBeInlined(call: JsInvocation): Boolean {
            val strategy = call.inlineStrategy
            return if (strategy == null || !strategy.isInline) false else functionContext.hasFunctionDefinition(call)
        }
    }

    private fun reportInlineCycle(call: JsInvocation, calledFunction: JsFunction) {
        call.inlineStrategy = InlineStrategy.NOT_INLINE
        val it = inlineCallInfos.descendingIterator()

        while (it.hasNext()) {
            val callInfo = it.next()
            val psiElement = callInfo.call.psiElement

            val descriptor = callInfo.call.descriptor
            if (psiElement != null && descriptor != null) {
                trace.report(Errors.INLINE_CALL_CYCLE.on(psiElement, descriptor))
            }

            if (callInfo.containingFunction == calledFunction) {
                break
            }
        }
    }


    private fun addInlinedModules(fragment: JsProgramFragment, inliner: InlinerImpl) {
        val existingModules = fragment.importedModules.mapTo(IdentitySet()) { it.internalName }
        inliner.inlinedModuleAliases.forEach {
            if (it !in existingModules) {
                fragment.importedModules.add(moduleMap[it]!!.let {
                    // Copy so that the Merger.kt doesn't operate on the same instance in different fragments.
                    JsImportedModule(it.externalName, it.internalName, it.plainReference)
                })
            }
        }
    }


    val functionContext: FunctionContext = object : FunctionContext(functionReader, config) {
        override fun lookUpStaticFunction(functionName: JsName?): FunctionWithWrapper? = functions[functionName]

        override fun lookUpStaticFunctionByTag(functionTag: String): FunctionWithWrapper? = accessors[functionTag]
    }

    private class JsCallInfo(val call: JsInvocation, val containingFunction: JsFunction)

    fun process(fragment: JsProgramFragment, existingImports: MutableMap<String, JsName>) {
        val existingNameBindings = fragment.nameBindings.associateTo(IdentityHashMap()) { it.name to it.key }

        val additionalDeclarations = mutableListOf<JsStatement>()
        val inliner = InlinerImpl(existingNameBindings, existingImports, 0) {
            additionalDeclarations.add(it)
        }

        inliner.acceptStatement(fragment.declarationBlock)
        // Mostly for the sake of post-processor
        // TODO are inline function marked with @Test possible?
        if (fragment.tests != null) {
            inliner.acceptStatement(fragment.tests)
        }
        inliner.acceptStatement(fragment.initializerBlock)

        // TODO fix the order
        fragment.declarationBlock.statements.addAll(0, additionalDeclarations)
        fragment.nameBindings.addAll(inliner.additionalNameBindings)

        addInlinedModules(fragment, inliner)
    }

    companion object {

        fun process(
            reporter: JsConfig.Reporter,
            config: JsConfig,
            trace: DiagnosticSink,
            translationResult: AstGenerationResult
        ) {
            val functions = collectNamedFunctionsAndWrappers(translationResult.newFragments)
            val accessors = collectAccessors(translationResult.fragments)

            val inverseNameBindings = inverseNameBindings(*translationResult.fragments.toTypedArray())

            val accessorInvocationTransformer = DummyAccessorInvocationTransformer()
            for (fragment in translationResult.newFragments) {
                accessorInvocationTransformer.accept<JsGlobalBlock>(fragment.declarationBlock)
                accessorInvocationTransformer.accept<JsGlobalBlock>(fragment.initializerBlock)
            }
            val functionReader = FunctionReader(reporter, config, translationResult.innerModuleName, translationResult.fragments)
            val moduleMap = translationResult.importedModuleList.associate { it.internalName to it }

            val inliner = JsInliner(config, functions, accessors, inverseNameBindings, functionReader, trace, moduleMap)
            for (fragment in translationResult.newFragments) {
                inliner.process(fragment, inverseNameBindings(fragment).entries.associateTo(mutableMapOf()) { (name, tag) -> tag to name })
            }

            for (fragment in translationResult.newFragments) {
                val block = JsBlock(fragment.declarationBlock, fragment.initializerBlock, fragment.exportBlock)
                removeUnusedImports(block)
                simplifyWrappedFunctions(block)
                removeUnusedFunctionDefinitions(block, collectNamedFunctions(block))
            }
        }

        private fun inverseNameBindings(vararg fragments: JsProgramFragment): MutableMap<JsName, String> {
            val name2Tag = mutableMapOf<JsName, String>()
            fragments.forEach {
                it.nameBindings.forEach { (tag, name) ->
                    name2Tag[name] = tag
                }
            }

            return name2Tag
        }

    }
}
