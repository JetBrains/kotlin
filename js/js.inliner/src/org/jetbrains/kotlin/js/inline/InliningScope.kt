/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.localAlias
import org.jetbrains.kotlin.js.backend.ast.metadata.staticRef
import org.jetbrains.kotlin.js.inline.clean.removeUnusedFunctionDefinitions
import org.jetbrains.kotlin.js.inline.clean.removeUnusedImports
import org.jetbrains.kotlin.js.inline.clean.renameLabels
import org.jetbrains.kotlin.js.inline.clean.simplifyWrappedFunctions
import org.jetbrains.kotlin.js.inline.util.*
import org.jetbrains.kotlin.js.translate.declaration.transformSpecialFunctionsToCoroutineMetadata
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import java.util.*

// Handles interpreting an inline function in terms of the current context.
// Either an program fragment, or a public inline function
sealed class InliningScope {

    abstract val fragment: JsProgramFragment

    protected abstract fun addInlinedDeclaration(tag: String?, declaration: JsStatement)

    protected abstract fun hasImport(name: JsName, tag: String): JsName?

    protected abstract fun addImport(tag: String, vars: JsVars)

    protected open fun addLocalDeclarationBinding(inlineFunctionTag: String, name: JsName, index: Int) {}

    protected open fun preprocess(statement: JsStatement) {}

    private val publicFunctionCache = mutableMapOf<String, JsFunction>()

    private val localFunctionCache = mutableMapOf<JsFunction, JsFunction>()

    private fun computeIfAbsent(tag: String?, function: JsFunction, fn: () -> JsFunction): JsFunction {
        if (tag == null) return localFunctionCache.computeIfAbsent(function) { fn() }

        return publicFunctionCache.computeIfAbsent(tag) { fn() }
    }

    fun importFunctionDefinition(definition: InlineFunctionDefinition): JsFunction {
        // Apparently we should avoid this trick when we implement fair support for crossinline
        // That's because crossinline lambdas inline into the declaration block and specialize those.
        val result = computeIfAbsent(definition.tag, definition.fn.function) {
            val newReplacements = HashMap<JsName, JsNameRef>()

            val copiedStatements = ArrayList<JsStatement>()
            val importStatements = mutableMapOf<JsVars, String>()

            definition.fn.wrapperBody?.let {
                it.statements.asSequence()
                    .filterNot { it is JsReturn }
                    .map { it.deepCopy() }
                    .forEach { statement ->
                        preprocess(statement)

                        if (statement is JsVars) {
                            val tag = getImportTag(statement)
                            if (tag != null) {
                                val name = statement.vars[0].name
                                val existingName = hasImport(name, tag) ?: JsScope.declareTemporaryName(name.ident).also {
                                    it.copyMetadataFrom(name)
                                    importStatements[statement] = tag
                                    copiedStatements.add(statement)
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

            val localDeclarations = mutableListOf<JsName>()

            copiedStatements.asSequence()
                .flatMap { node -> collectDefinedNamesInAllScopes(node).asSequence() }
                .filter { name -> !newReplacements.containsKey(name) }
                .forEach { name ->
                    val alias = JsScope.declareTemporaryName(name.ident)
                    alias.copyMetadataFrom(name)
                    localDeclarations += alias
                    val replacement = JsAstUtils.pureFqn(alias, null)
                    newReplacements[name] = replacement
                }

            // Add local declarations to the name bindings in order to correctly rename usages of the same imported local declaraions
            definition.tag?.let { tag ->
                localDeclarations.forEachIndexed { index, name ->
                    addLocalDeclarationBinding(tag, name, index)
                }
            }

            // Apply renaming and restore the static ref links
            JsBlock(copiedStatements).let {
                replaceNames(it, newReplacements)

                // Restore the staticRef links
                for ((key, value) in collectNamedFunctions(it)) {
                    if (key.staticRef is JsFunction) {
                        key.staticRef = value
                    }
                }
            }

            copiedStatements.forEach {
                if (it is JsVars && it in importStatements) {
                    addImport(importStatements[it]!!, it)
                } else {
                    addInlinedDeclaration(definition.tag, it)
                }
            }

            val result = definition.fn.function.deepCopy()

            replaceNames(result, newReplacements)

            result.body = transformSpecialFunctionsToCoroutineMetadata(result.body)

            result
        }.deepCopy()

        // Copy parameter JsName's
        val paramMap = result.parameters.associate {
            val alias = JsScope.declareTemporaryName(it.name.ident)
            alias.copyMetadataFrom(it.name)
            it.name to JsAstUtils.pureFqn(alias, null)
        }

        replaceNames(result, paramMap)

        return result
    }
}

class ImportIntoFragmentInliningScope private constructor(
    override val fragment: JsProgramFragment
) : InliningScope() {

    val allCode: JsBlock
        get() = JsBlock(
            JsBlock(fragment.inlinedLocalDeclarations.values.toList()),
            fragment.declarationBlock,
            fragment.exportBlock,
            JsExpressionStatement(JsFunction(JsDynamicScope, fragment.initializerBlock, ""))
        ).also { block ->
            fragment.tests?.let { block.statements.add(it) }
            fragment.mainFunction?.let { block.statements.add(it) }
        }

    private val existingModules = fragment.importedModules.associateTo(mutableMapOf()) { it.key to it }

    private val existingBindings = fragment.nameBindings.associateTo(mutableMapOf()) { it.key to it.name }

    private val additionalDeclarations = mutableListOf<JsStatement>()

    override fun hasImport(name: JsName, tag: String): JsName? {
        return name.localAlias?.let { (name, tag) ->
            if (tag != null) {
                if (tag !in existingBindings) {
                    addNameBinding(name, tag)
                }
                existingBindings[tag]
            } else name
        } ?: existingBindings[tag]
    }

    private fun addNameBinding(name: JsName, tag: String) {
        fragment.nameBindings.add(JsNameBinding(tag, name))
        existingBindings[tag] = name
    }


    override fun addImport(tag: String, vars: JsVars) {
        val name = vars.vars[0].name
        val expr = vars.vars[0].initExpression
        fragment.imports[tag] = expr
        addNameBinding(name, tag)
    }

    override fun addLocalDeclarationBinding(inlineFunctionTag: String, name: JsName, index: Int) {
        addNameBinding(name, "\$local:$inlineFunctionTag:$index")
    }

    override fun addInlinedDeclaration(tag: String?, declaration: JsStatement) {
        if (tag != null) {
            fragment.inlinedLocalDeclarations.computeIfAbsent(tag) { JsGlobalBlock() }.statements.add(declaration)
        } else {
            additionalDeclarations.add(declaration)
        }
    }

    override fun preprocess(statement: JsStatement) {
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
                    ctx.replaceMe(addInlinedModule(alias).makeRef())
                }
            }

        }.accept(statement)
    }

    private fun addInlinedModule(module: JsImportedModule): JsName {
        return existingModules.computeIfAbsent(module.key) {
            // Copy so that the Merger.kt doesn't operate on the same instance in different fragments.
            JsImportedModule(module.externalName, module.internalName, module.plainReference).also {
                fragment.importedModules.add(it)
            }
        }.internalName
    }

    companion object {
        fun process(fragment: JsProgramFragment, fn: (ImportIntoFragmentInliningScope) -> Unit) {
            val scope = ImportIntoFragmentInliningScope(fragment)
            fn(scope)

            scope.apply {
                // TODO fix the order?
                fragment.declarationBlock.statements.addAll(0, additionalDeclarations)

                // post-processing

                // If run separately `private inline suspend fun`'s local declarations get inlined twice.
                InlineSuspendFunctionSplitter(this).accept(allCode)

                simplifyWrappedFunctions(allCode)
                removeUnusedFunctionDefinitions(allCode, collectNamedFunctions(allCode))
                removeUnusedImports(fragment, allCode)
                renameLabels(allCode)
            }
        }
    }
}

class ImportIntoWrapperInliningScope private constructor(
    private val wrapperBody: JsBlock,
    override val fragment: JsProgramFragment
) : InliningScope() {
    private val importList = mutableListOf<JsVars>()

    private val otherLocalStatements = mutableListOf<JsStatement>()

    private val existingImports = mutableMapOf<String, JsName>()

    init {
        for (s in wrapperBody.statements) {
            if (s is JsVars) {
                val tag = getImportTag(s)
                if (tag != null) {
                    importList.add(s)
                    existingImports[tag] = s.vars[0].name
                    continue
                }
            }

            otherLocalStatements.add(s)
        }
    }

    private val additionalStatements = mutableListOf<JsStatement>()

    override fun addInlinedDeclaration(tag: String?, declaration: JsStatement) {
        additionalStatements.add(declaration)
    }

    override fun hasImport(name: JsName, tag: String): JsName? = existingImports[tag]

    override fun addImport(tag: String, vars: JsVars) {
        existingImports[tag] = vars.vars[0].name
        importList.add(vars)
    }

    companion object {
        fun process(wrapperBody: JsBlock, fragment: JsProgramFragment, fn: (ImportIntoWrapperInliningScope) -> Unit) {
            val scope = ImportIntoWrapperInliningScope(wrapperBody, fragment)
            fn(scope)
            wrapperBody.statements.apply {
                clear()
                addAll(scope.importList)
                addAll(scope.additionalStatements)
                addAll(scope.otherLocalStatements)
            }
        }
    }
}