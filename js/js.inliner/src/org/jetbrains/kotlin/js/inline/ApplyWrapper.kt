/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.localAlias
import org.jetbrains.kotlin.js.backend.ast.metadata.staticRef
import org.jetbrains.kotlin.js.inline.util.collectDefinedNamesInAllScopes
import org.jetbrains.kotlin.js.inline.util.collectNamedFunctions
import org.jetbrains.kotlin.js.inline.util.getImportTag
import org.jetbrains.kotlin.js.inline.util.replaceNames
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import java.util.ArrayList
import java.util.HashMap

fun applyWrapper(
    wrapper: JsBlock, function: JsFunction, originalFunction: JsFunction,
    inlineFunctionDepth: Int,
    replacementsInducedByWrappers: MutableMap<JsFunction, Map<JsName, JsNameRef>>,
    existingImports: MutableMap<String, JsName>,
    additionalImports: MutableList<Triple<String, JsExpression, JsName>>,
    existingNameBindings: MutableMap<JsName, String>,
    additionalNameBindings: MutableList<JsNameBinding>,
    inlinedModuleAliases: MutableSet<JsName>,
    inverseNameBindings: Map<JsName, String>,
    addPrevious: (JsStatement) -> Unit
) {

    // TODO Decrypt the comment below
    // Apparently we should avoid this trick when we implement fair support for crossinline
    val replacements = replacementsInducedByWrappers.computeIfAbsent(originalFunction) { k ->
        val newReplacements = HashMap<JsName, JsNameRef>()

        val copiedStatements = ArrayList<JsStatement>()
        val importStatements = ArrayList<Pair<String, JsVars.JsVar>>()
        wrapper.statements.asSequence()
            .filterNot { it is JsReturn }
            .map { it.deepCopy() }
            .forEach { statement ->
                if (inlineFunctionDepth == 0) {
                    replaceExpressionsWithLocalAliases(statement, inlinedModuleAliases)
                }

                if (statement is JsVars) {
                    val tag = getImportTag(statement)
                    if (tag != null) {
                        // TODO handle JsVars with multiple vars?
                        val name = statement.vars[0].name
                        var existingName: JsName? = if (inlineFunctionDepth == 0) name.localAlias else null
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

        val definedNames = (importStatements.asSequence().map { it.second } + copiedStatements.asSequence())
            .flatMap { node -> collectDefinedNamesInAllScopes(node).asSequence() }
            .filter { name -> !newReplacements.containsKey(name) }
            .toSet()
        for (name in definedNames) {
            val alias = JsScope.declareTemporaryName(name.ident)
            alias.copyMetadataFrom(name)
            val replacement = JsAstUtils.pureFqn(alias, null)
            newReplacements[name] = replacement
        }

        for ((tag, statement) in importStatements) {
            val renamed = replaceNames(statement, newReplacements)
            additionalImports.add(Triple(tag,renamed.initExpression, renamed.name))
            additionalNameBindings.add(JsNameBinding(tag, renamed.name))
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

private fun replaceExpressionsWithLocalAliases(statement: JsStatement, inlinedModuleAliases: MutableSet<JsName>) {
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