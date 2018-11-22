/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.imported
import org.jetbrains.kotlin.js.backend.ast.metadata.localAlias
import org.jetbrains.kotlin.js.backend.ast.metadata.staticRef
import org.jetbrains.kotlin.js.inline.clean.removeUnusedFunctionDefinitions
import org.jetbrains.kotlin.js.inline.clean.removeUnusedImports
import org.jetbrains.kotlin.js.inline.clean.simplifyWrappedFunctions
import org.jetbrains.kotlin.js.inline.context.FunctionContext
import org.jetbrains.kotlin.js.inline.util.*
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import java.util.*


// Handles interpreting an inline function in terms of the current context.
// Either an program fragment, or a public inline function
sealed class InliningScope {

    private val cache = mutableMapOf<String, Map<JsName, JsNameRef>>()

    protected fun computeIfAbsent(tag: String?, fn: () -> Map<JsName, JsNameRef>): Map<JsName, JsNameRef> {
        if (tag == null) return fn()

        return cache.computeIfAbsent(tag) { fn() }
    }

    abstract fun importFunctionDefinition(f: InlineFunctionDefinition): JsFunction

    abstract fun process()

    abstract val fragment: JsProgramFragment
}

class ProgramFragmentInliningScope(
    override val fragment: JsProgramFragment,
    val functionContext: FunctionContext,
    val rootInliner: JsInliner
) : InliningScope() {

    private val existingModules = fragment.importedModules.mapTo(IdentitySet()) { it.internalName }

    private val existingImports = fragment.nameBindings.associateTo(mutableMapOf()) { it.key to it.name }

    private val existingNameBindings = fragment.nameBindings.associateTo(IdentityHashMap()) { it.name to it.key }

    private val additionalDeclarations = mutableListOf<JsStatement>()

    private var processed = false

    override fun process() {
        if (!processed) {
            // TODO is this even needed?
            processed = true

            val inliner = InlinerImpl(rootInliner.cycleReporter, functionContext, this)

            // TODO any way and/or need to visit everything inside the fragment?
            inliner.acceptStatement(fragment.declarationBlock)

            // TODO Atm it's placed after inliner in order not to perform the body inlining twice. Is that OK?
            // Ideally it could be moved to the coroutine transformers. The info regarding which inline function wrappers have been imported
            // on top level should be persisted for that sake. Also it going to be needed in order to avoid duplicate code.
            InlineSuspendFunctionSplitter(this).accept(fragment.declarationBlock)

            // Mostly for the sake of post-processor
            // TODO are inline function marked with @Test possible?
            if (fragment.tests != null) {
                inliner.acceptStatement(fragment.tests)
            }
            // TODO wrap in a function in order to do the post-processing
            inliner.acceptStatement(fragment.initializerBlock)

            updateProgramFragment()
        }
    }

    private fun updateProgramFragment() {
        // TODO fix the order
        // TODO this probably will be replaced with a special tag -> block map for the imported stuff, so that we can merge same imports.
        // TODO in that case this method will become obsolete
        fragment.declarationBlock.statements.addAll(0, additionalDeclarations)

        // post-processing
        val block = JsBlock(fragment.declarationBlock, fragment.initializerBlock, fragment.exportBlock)
        fragment.tests?.let { block.statements.add(it) }
        fragment.mainFunction?.let { block.statements.add(it) }

        simplifyWrappedFunctions(block)
        removeUnusedFunctionDefinitions(block, collectNamedFunctions(block))

        // TODO simplify
        val usedImports = removeUnusedImports(block)
        fragment.nameBindings.filter {
            !it.name.imported || it.name in usedImports
        }.let {
            fragment.nameBindings.clear()
            fragment.nameBindings.addAll(it)
        }
        val existingTags = fragment.nameBindings.map { it.key }.toSet()
        val newImports = fragment.imports.filter { (k, _) -> k in existingTags }
        fragment.imports.clear()
        for ((k, v) in newImports) {
            fragment.imports[k] = v
        }
    }

    private fun addInlinedModule(moduleName: JsName) {
        if (moduleName !in existingModules) {
            fragment.importedModules.add(moduleMap[moduleName]!!.let {
                // Copy so that the Merger.kt doesn't operate on the same instance in different fragments.
                JsImportedModule(it.externalName, it.internalName, it.plainReference)
            })
        }
    }

    private fun addImport(tag: String, e: JsExpression) {
        fragment.imports[tag] = e
    }

    private fun addNameBinding(binding: JsNameBinding) {
        fragment.nameBindings.add(binding)
        existingNameBindings[binding.name] = binding.key
    }

    override fun importFunctionDefinition(f: InlineFunctionDefinition): JsFunction {

        // Apparently we should avoid this trick when we implement fair support for crossinline
        // That's because crossinline lambdas inline into the declaration block and specialize those.
        val replacements = computeIfAbsent(f.tag) {
            val newReplacements = HashMap<JsName, JsNameRef>()

            val copiedStatements = ArrayList<JsStatement>()
            val importStatements = ArrayList<Pair<String, JsVars.JsVar>>()

            f.functionWithWrapper.wrapperBody?.let {
                it.statements.asSequence()
                    .filterNot { it is JsReturn }
                    .map { it.deepCopy() }
                    .forEach { statement ->
                        replaceExpressionsWithLocalAliases(statement)

                        if (statement is JsVars) {
                            val tag = getImportTag(statement)
                            if (tag != null) {
                                // TODO handle JsVars with multiple vars?
                                val name = statement.vars[0].name
                                var existingName: JsName? = name.localAlias
                                if (existingName == null) {
                                    existingName = existingImports.computeIfAbsent(tag) {
                                        importStatements.add(tag to statement.vars[0])
                                        val alias = JsScope.declareTemporaryName(name.ident)
                                        alias.copyMetadataFrom(name)
                                        newReplacements[name] = JsAstUtils.pureFqn(alias, null)
                                        alias
                                    }
                                }

                                if (name !== existingName) {
                                    val replacement = JsAstUtils.pureFqn(existingName, null)
                                    newReplacements[name] = replacement
                                }

                                return@forEach
                            }
                        }

                        copiedStatements.add(statement)
                    }
            }

            (importStatements.asSequence().map { JsVars(it.second) } + copiedStatements.asSequence())
                .flatMap { node -> collectDefinedNamesInAllScopes(node).asSequence() }
                .filter { name -> !newReplacements.containsKey(name) }
                .forEach { name ->
                    val alias = JsScope.declareTemporaryName(name.ident)
                    alias.copyMetadataFrom(name)
                    val replacement = JsAstUtils.pureFqn(alias, null)
                    newReplacements[name] = replacement
                }


            for ((tag, statement) in importStatements) {
                val renamed = replaceNames(statement, newReplacements)
                // TODO shouldn't this be done at `existingImports.computeIfAbsent` moment?
                addImport(tag, renamed.initExpression)

                addNameBinding(JsNameBinding(tag, renamed.name))
            }

            if (f.tag != null) {
                fragment.inlinedFunctionWrappers[f.tag!!] = JsGlobalBlock().also {
                    copiedStatements.mapTo(it.statements) { replaceNames(it, newReplacements) }
                }
            } else {
                // TODO Handle it better?
                for (statement in copiedStatements) {
                    additionalDeclarations.add(replaceNames(statement, newReplacements))
                }
            }

            // TODO shouldn't this be moved to renamer?
            for ((key, value) in collectNamedFunctions(JsBlock(copiedStatements))) {
                if (key.staticRef is JsFunction) {
                    key.staticRef = value
                }
            }

            newReplacements
        }

        val paramMap = f.functionWithWrapper.function.parameters.associate {
            val alias = JsScope.declareTemporaryName(it.name.ident)
            alias.copyMetadataFrom(it.name)
            it.name to JsAstUtils.pureFqn(alias, null)
        }

        val result = f.functionWithWrapper.function.deepCopy()

        replaceNames(result, replacements)
        replaceNames(result, paramMap)

        return result
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
                    addInlinedModule(alias)
                }
            }

        }.accept(statement)
    }
}

class PublicInlineFunctionInliningScope(
    override val fragment: JsProgramFragment,
    cycleReporter: InlinerCycleReporter,
    functionContext: FunctionContext,
    val function: JsFunction,
    val wrapperBody: JsBlock
) : InliningScope() {

    val additionalStatements = mutableListOf<JsStatement>()

    val innerInliner = InlinerImpl(
        cycleReporter,
        functionContext,
        this
    )

    override fun process() {
        for (statement in wrapperBody.statements) {
            if (statement !is JsReturn) {
                innerInliner.acceptStatement(statement)
            } else {
                innerInliner.accept((statement.expression as JsFunction).body)
            }
        }

        // TODO keep order
        wrapperBody.statements.addAll(0, additionalStatements)
    }

    private fun addPrevious(statement: JsStatement) {
        // TODO Is this correct?
        additionalStatements.add(innerInliner.accept(statement))
    }

    override fun importFunctionDefinition(f: InlineFunctionDefinition): JsFunction {
        // TODO Decrypt the comment below
        // Apparently we should avoid this trick when we implement fair support for crossinline
        val replacements = computeIfAbsent(f.tag) {
            val newReplacements = HashMap<JsName, JsNameRef>()

            // TODO Why don't we collect existing imports?

            val copiedStatements = f.functionWithWrapper.wrapperBody!!.statements.asSequence()
                .filterNot { it is JsReturn }
                .map { it.deepCopy() }.toList()

            val definedNames = copiedStatements.asSequence()
                .flatMap { node -> collectDefinedNamesInAllScopes(node).asSequence() }
                .filter { name -> !newReplacements.containsKey(name) }
                .toSet()
            for (name in definedNames) {
                val alias = JsScope.declareTemporaryName(name.ident)
                alias.copyMetadataFrom(name)
                val replacement = JsAstUtils.pureFqn(alias, null)
                newReplacements[name] = replacement
            }

            for (statement in copiedStatements) {
                addPrevious(replaceNames(statement, newReplacements))
            }

            for ((key, value) in collectNamedFunctions(JsBlock(copiedStatements))) {
                if (key.staticRef is JsFunction) {
                    key.staticRef = value
                }
            }

            newReplacements
        }

        val paramMap = f.functionWithWrapper.function.parameters.associate {
            val alias = JsScope.declareTemporaryName(it.name.ident)
            alias.copyMetadataFrom(it.name)
            it.name to JsAstUtils.pureFqn(alias, null)
        }

        val result = f.functionWithWrapper.function.deepCopy()

        replaceNames(result, replacements)
        replaceNames(result, paramMap)

        return result
    }
}