/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.js.inline

import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.config.JsConfig
import org.jetbrains.kotlin.js.inline.clean.*
import org.jetbrains.kotlin.js.inline.context.FunctionContext
import org.jetbrains.kotlin.js.inline.util.*

import java.util.*

import org.jetbrains.kotlin.js.inline.util.IdentitySet
import org.jetbrains.kotlin.js.translate.general.AstGenerationResult

class JsInliner private constructor(
    config: JsConfig,
    functionReader: FunctionReader,
    val functions: Map<JsName, FunctionWithWrapper>,
    val accessors: Map<String, FunctionWithWrapper>,
    private val inverseNameBindings: Map<JsName, String>,
    private val moduleMap: Map<JsName, JsImportedModule>,
    private val trace: DiagnosticSink
) {

    private val functionContext = FunctionContext(functionReader, config, functions, accessors)

    private fun addInlinedModules(fragment: JsProgramFragment, inlinedModuleAliases: Set<JsName>) {
        val existingModules = fragment.importedModules.mapTo(IdentitySet()) { it.internalName }
        inlinedModuleAliases.forEach {
            if (it !in existingModules) {
                fragment.importedModules.add(moduleMap[it]!!.let {
                    // Copy so that the Merger.kt doesn't operate on the same instance in different fragments.
                    JsImportedModule(it.externalName, it.internalName, it.plainReference)
                })
            }
        }
    }

    fun process(fragment: JsProgramFragment, existingImports: MutableMap<String, JsName>) {
        val existingNameBindings = fragment.nameBindings.associateTo(IdentityHashMap()) { it.name to it.key }

        val dfsController = InlineDfsController(trace, functions, accessors, functionContext)

        val additionalDeclarations = mutableListOf<JsStatement>()
        val inliner = InlinerImpl(
            existingNameBindings,
            existingImports,
            0,
            dfsController,
            inverseNameBindings,
            functionContext
        ) {
            additionalDeclarations.add(it)
        }

        val inlineSuspendFnSplitter = InlineSuspendFunctionSplitter(
            existingNameBindings,
            existingImports,
            0,
            inverseNameBindings,
            inliner.replacementsInducedByWrappers
        ) {
            additionalDeclarations.add(it)
        }

        inliner.acceptStatement(fragment.declarationBlock)

        inlineSuspendFnSplitter.accept(fragment.declarationBlock)

        // Mostly for the sake of post-processor
        // TODO are inline function marked with @Test possible?
        if (fragment.tests != null) {
            inliner.acceptStatement(fragment.tests)
        }
        inliner.acceptStatement(fragment.initializerBlock)

        // TODO fix the order
        fragment.declarationBlock.statements.addAll(0, additionalDeclarations)
        fragment.nameBindings.addAll(inliner.additionalNameBindings)
        // TODO actually bogus
        fragment.nameBindings.addAll(inlineSuspendFnSplitter.additionalNameBindings)

        (inliner.additionalImports.asSequence() + inlineSuspendFnSplitter.additionalImports.asSequence()).forEach { (tag, e, _) ->
            fragment.imports[tag] = e
        }

        addInlinedModules(fragment, inliner.inlinedModuleAliases)
        addInlinedModules(fragment, inlineSuspendFnSplitter.inlinedModuleAliases)
    }

    companion object {

        // TODO decrypt
        // Since we compile each source file in its own context (and we may loose these context when performing incremental compilation)
        // we don't use contexts to generate proper names for modules. Instead, we generate all necessary information during
        // translation and rely on it here.
        private fun buildModuleNameMap(fragments: List<JsProgramFragment>): Map<String, JsExpression> {
            return fragments.flatMap { it.inlineModuleMap.entries }.associate { (k, v) -> k to v }
        }


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
            val functionReader = FunctionReader(reporter, config, translationResult.innerModuleName, buildModuleNameMap(translationResult.fragments))

            val moduleMap = translationResult.importedModuleList.associate { it.internalName to it }

            val inliner = JsInliner(config, functionReader, functions, accessors, inverseNameBindings, moduleMap, trace)
            for (fragment in translationResult.newFragments) {
                inliner.process(fragment, inverseNameBindings(fragment).entries.associateTo(mutableMapOf()) { (name, tag) -> tag to name })
            }

            for (fragment in translationResult.newFragments) {
                val block = JsBlock(fragment.declarationBlock, fragment.initializerBlock, fragment.exportBlock)
                val usedImports = removeUnusedImports(block)
                for ((key, name) in fragment.nameBindings) {
                    if (name !in usedImports && key in fragment.imports) {
                        fragment.imports.remove(key)
                    }
                }
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
