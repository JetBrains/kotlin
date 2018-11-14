/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.backend.common.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.inline.clean.*
import org.jetbrains.kotlin.js.inline.context.FunctionContext
import org.jetbrains.kotlin.js.inline.context.InliningContext
import org.jetbrains.kotlin.js.inline.context.NamingContext
import org.jetbrains.kotlin.js.inline.util.*
import org.jetbrains.kotlin.js.translate.expression.InlineMetadata
import org.jetbrains.kotlin.resolve.inline.InlineStrategy

import java.util.*

import org.jetbrains.kotlin.js.inline.util.getImportTag
import org.jetbrains.kotlin.js.inline.util.IdentitySet
import org.jetbrains.kotlin.js.translate.declaration.transformSpecialFunctionsToCoroutineMetadata
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.flattenStatement
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn

class JsInliner private constructor(
    private val config: JsConfig,
    private val functions: Map<JsName, FunctionWithWrapper>,
    private val accessors: Map<String, FunctionWithWrapper>,
    private val inverseNameBindings: Map<JsName, String>,
    private val functionReader: FunctionReader,
    private val trace: DiagnosticSink
) : JsVisitorWithContextImpl() {
    private val namedFunctionsSet: MutableSet<JsFunction> = functions.values.mapTo(IdentitySet()) { it.function }
    private val inliningContexts = Stack<JsInliningContext>()
    private val processedFunctions = IdentitySet<JsFunction>()
    private val inProcessFunctions = IdentitySet<JsFunction>()
    private var existingImports: MutableMap<String, JsName> = HashMap()
    private var statementContextForInline: JsContext<JsStatement>? = null

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
    private val canBeExtractedByInliner: (JsNode) -> Boolean = { node -> node is JsInvocation && hasToBeInlined(node) }
    private var inlineFunctionDepth: Int = 0

    private val replacementsInducedByWrappers = HashMap<JsWrapperKey, Map<JsName, JsNameRef>>()

    private var existingNameBindings: MutableMap<JsName, String> = HashMap()

    private val additionalNameBindings = ArrayList<JsNameBinding>()

    private val inlinedModuleAliases = HashSet<JsName>()

    private val inliningContext: JsInliningContext
        get() = inliningContexts.peek()

    private val functionContext: FunctionContext
        get() = inliningContext.functionContext

    private val currentNamedFunction: JsFunction?
        get() = if (namedFunctionsStack.empty()) null else namedFunctionsStack.peek()

    private fun addInlinedModules(fragment: JsProgramFragment, moduleMap: Map<JsName, JsImportedModule>) {
        val localMap = buildModuleMap(listOf(fragment)).keys
        for (inlinedModuleName in inlinedModuleAliases) {
            if (!localMap.contains(inlinedModuleName)) {
                fragment.importedModules.add(moduleMap[inlinedModuleName]!!)
            }
        }
    }

    private fun processImportStatement(statement: JsStatement) {
        if (statement is JsVars) {
            val tag = getImportTag(statement)
            if (tag != null) {
                existingImports[tag] = statement.vars[0].name
            }
        }
    }

    override fun visit(function: JsFunction, context: JsContext<*>): Boolean {
        val functionWithWrapper = functionsByFunctionNodes[function]
        if (functionWithWrapper != null) {
            visit(functionWithWrapper)
            return false
        } else {
            if (statementContextForInline == null) {
                statementContextForInline = lastStatementLevelContext
                startFunction(function)
                val result = super.visit(function, context)
                statementContextForInline = null
                return result
            } else {
                startFunction(function)
                return super.visit(function, context)
            }
        }
    }

    override fun endVisit(function: JsFunction, context: JsContext<*>) {
        super.endVisit(function, context)
        if (!functionsByFunctionNodes.containsKey(function)) {
            endFunction(function)
        }
    }

    private fun startFunction(function: JsFunction) {
        inliningContexts.push(JsInliningContext(statementContextForInline!!))

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

        inliningContexts.pop()

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
        val oldContextForInline = statementContextForInline
        val oldExistingImports = existingImports
        val oldInlineFunctionDepth = inlineFunctionDepth

        val innerContext = ListContext<JsStatement>()

        val wrapperBody = functionWithWrapper.wrapperBody
        var statements: MutableList<JsStatement>? = null
        if (wrapperBody != null) {
            existingImports = HashMap()
            statementContexts.push(innerContext)
            statementContextForInline = innerContext
            inlineFunctionDepth++

            for (statement in wrapperBody.statements) {
                processImportStatement(statement)
            }
            statements = wrapperBody.statements
            if (!statements.isEmpty() && statements[statements.size - 1] is JsReturn) {
                statements = statements.subList(0, statements.size - 1)
            }

            innerContext.traverse(statements)
            statementContexts.pop()
        } else {
            if (statementContextForInline == null) statementContextForInline = lastStatementLevelContext
        }

        startFunction(functionWithWrapper.function)

        val block = JsBlock(functionWithWrapper.function.body)
        innerContext.traverse(block.statements)
        functionWithWrapper.function.body.traverse(this, innerContext)

        endFunction(functionWithWrapper.function)

        statements?.addAll(block.statements.subList(0, block.statements.size - 1))

        statementContextForInline = oldContextForInline
        existingImports = oldExistingImports
        inlineFunctionDepth = oldInlineFunctionDepth
    }

    override fun visit(call: JsInvocation, context: JsContext<*>): Boolean {
        if (!hasToBeInlined(call)) return true

        val containingFunction = currentNamedFunction

        if (containingFunction != null) {
            inlineCallInfos.add(JsCallInfo(call, containingFunction))
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

        var lastCallInfo: JsCallInfo? = null

        if (!inlineCallInfos.isEmpty()) {
            lastCallInfo = inlineCallInfos.last
        }

        if (lastCallInfo != null && lastCallInfo.call == x) {
            inlineCallInfos.removeLast()
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
            }
        }

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
                    applyWrapper(wrapperBody, function, originalFunction, JsInliningContext(statementContext))
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
        // at top level of js ast, contexts stack can be empty,
        // but there is no inline calls anyway
        if (!inliningContexts.isEmpty()) {
            var i = 0

            while (i < statements.size) {
                val additionalStatements = ExpressionDecomposer.preserveEvaluationOrder(statements[i], canBeExtractedByInliner)
                statements.addAll(i, additionalStatements)
                i += additionalStatements.size + 1
            }
        }

        super.doAcceptStatementList(statements)
    }

    private fun inline(call: JsInvocation, context: JsContext<JsNode>) {
        val callDescriptor = call.descriptor
        if (isSuspendWithCurrentContinuation(
                callDescriptor,
                config.configuration.languageVersionSettings
            )
        ) {
            inlineSuspendWithCurrentContinuation(call, context)
            return
        }

        val inliningContext = inliningContext
        var functionWithWrapper = inliningContext.functionContext.getFunctionDefinition(call)

        // Since we could get functionWithWrapper as a simple function directly from staticRef (which always points on implementation)
        // we should check if we have a known wrapper for it
        functionsByFunctionNodes[functionWithWrapper.function]?.let {
            functionWithWrapper = it
        }

        val function = functionWithWrapper.function.deepCopy()
        function.body = transformSpecialFunctionsToCoroutineMetadata(function.body)
        if (functionWithWrapper.wrapperBody != null) {
            applyWrapper(functionWithWrapper.wrapperBody!!, function, functionWithWrapper.function, inliningContext)
        }
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
        wrapper: JsBlock, function: JsFunction, originalFunction: JsFunction,
        inliningContext: InliningContext
    ) {
        val key = JsWrapperKey(inliningContext.statementContextBeforeCurrentFunction, originalFunction)

        // Apparently we should avoid this trick when we implement fair support for crossinline
        val replacements = replacementsInducedByWrappers.computeIfAbsent(key) { k ->
            val ctx = k.context

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
                ctx.addPrevious(accept(replaceNames(statement, newReplacements)))
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

    private fun inlineSuspendWithCurrentContinuation(call: JsInvocation, context: JsContext<JsNode>) {
        val lambda = call.arguments[0]
        val continuationArg = call.arguments[call.arguments.size - 1]

        val invocation = JsInvocation(lambda, continuationArg)
        invocation.isSuspend = true
        context.replaceMe(accept(invocation))
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

    private fun hasToBeInlined(call: JsInvocation): Boolean {
        val strategy = call.inlineStrategy
        return if (strategy == null || !strategy.isInline) false else functionContext.hasFunctionDefinition(call)

    }

    private inner class JsInliningContext internal constructor(override val statementContextBeforeCurrentFunction: JsContext<JsStatement>) :
        InliningContext {
        override val functionContext: FunctionContext

        override val statementContext: JsContext<JsStatement>
            get() = lastStatementLevelContext

        init {
            functionContext = object : FunctionContext(functionReader, config) {
                override fun lookUpStaticFunction(functionName: JsName?): FunctionWithWrapper? {
                    return functions[functionName]
                }

                override fun lookUpStaticFunctionByTag(functionTag: String): FunctionWithWrapper? {
                    return accessors[functionTag]
                }
            }
        }

        override fun newNamingContext(): NamingContext {
            return NamingContext(statementContext)
        }
    }

    private class JsCallInfo(val call: JsInvocation, val containingFunction: JsFunction)

    internal class JsWrapperKey(val context: JsContext<JsStatement>, private val function: JsFunction) {

        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val key = o as JsWrapperKey?
            return context == key!!.context && function == key.function
        }

        override fun hashCode(): Int {
            return Objects.hash(context, function)
        }
    }

    companion object {

        fun process(
            reporter: JsConfig.Reporter,
            config: JsConfig,
            trace: DiagnosticSink,
            currentModuleName: JsName,
            fragments: List<JsProgramFragment>,
            fragmentsToProcess: List<JsProgramFragment>,
            importStatements: List<JsStatement>
        ) {
            val functions = collectNamedFunctionsAndWrappers(fragments)
            val accessors = collectAccessors(fragments)
            val inverseNameBindings = collectNameBindings(fragments)

            val accessorInvocationTransformer = DummyAccessorInvocationTransformer()
            for (fragment in fragmentsToProcess) {
                accessorInvocationTransformer.accept<JsGlobalBlock>(fragment.declarationBlock)
                accessorInvocationTransformer.accept<JsGlobalBlock>(fragment.initializerBlock)
            }
            val functionReader = FunctionReader(reporter, config, currentModuleName, fragments)
            val inliner = JsInliner(config, functions, accessors, inverseNameBindings, functionReader, trace)

            for (statement in importStatements) {
                inliner.processImportStatement(statement)
            }

            val moduleMap = fillModuleMap(buildModuleMap(fragments), fragmentsToProcess)

            for (fragment in fragmentsToProcess) {
                inliner.existingImports.clear()
                inliner.additionalNameBindings.clear()
                inliner.inlinedModuleAliases.clear()
                inliner.existingNameBindings = collectNameBindings(listOf(fragment))

                inliner.acceptStatement<JsGlobalBlock>(fragment.declarationBlock)
                // Mostly for the sake of post-processor
                // TODO are inline function marked with @Test possible?
                if (fragment.tests != null) {
                    inliner.acceptStatement(fragment.tests)
                }

                // There can be inlined function in top-level initializers, we need to optimize them as well
                val fakeInitFunction = JsFunction(JsDynamicScope, fragment.initializerBlock, "")
                val initWrapper = JsGlobalBlock()
                initWrapper.statements.add(JsExpressionStatement(fakeInitFunction))
                inliner.accept(initWrapper)
                initWrapper.statements.removeAt(initWrapper.statements.size - 1)

                fragment.initializerBlock.getStatements().addAll(0, initWrapper.statements)
                fragment.nameBindings.addAll(inliner.additionalNameBindings)
                inliner.addInlinedModules(fragment, moduleMap)
            }

            for (fragment in fragmentsToProcess) {
                val block = JsBlock(fragment.declarationBlock, fragment.initializerBlock, fragment.exportBlock)
                removeUnusedImports(block)
                simplifyWrappedFunctions(block)
                removeUnusedFunctionDefinitions(block, collectNamedFunctions(block))
            }
        }

        private fun buildModuleMap(fragments: List<JsProgramFragment>): MutableMap<JsName, JsImportedModule> {
            return fillModuleMap(HashMap(), fragments)
        }

        private fun fillModuleMap(
            map: MutableMap<JsName, JsImportedModule>,
            fragments: List<JsProgramFragment>
        ): MutableMap<JsName, JsImportedModule> {
            for (fragment in fragments) {
                for (module in fragment.importedModules) {
                    map[module.internalName] = module
                }
            }
            return map
        }

        private fun isSuspendWithCurrentContinuation(
            descriptor: DeclarationDescriptor?,
            languageVersionSettings: LanguageVersionSettings
        ): Boolean {
            return (descriptor as? FunctionDescriptor)?.original?.isBuiltInSuspendCoroutineUninterceptedOrReturn(
                languageVersionSettings
            ) ?: false
        }
    }
}
