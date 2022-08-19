/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.webworkers.compiler

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.extensions.IrToJsExtensionKey
import org.jetbrains.kotlin.ir.backend.js.extensions.IrToJsTransformationExtension
import org.jetbrains.kotlin.ir.backend.js.transformers.irToJs.*
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.IdSignatureRetriever
import org.jetbrains.kotlinx.webworkers.compiler.workers.collectWorkerFunctions
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.interpreter.toIrConst
import org.jetbrains.kotlin.ir.types.classFqName
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.js.ModuleKind
import org.jetbrains.kotlinx.webworkers.compiler.workers.checkDomAccessInWorker

class WebWorkersIrToJsExtension : IrToJsTransformationExtension {
    override fun getAdditionalDceRoots(module: IrModuleFragment): List<IrDeclaration> {
        return collectWorkerFunctions(module).values.toList()
    }

    override fun transformMainFunction(mainInvocation: JsStatement): JsStatement {
        // if (typeof WorkerGlobalScope === 'undefined') {
        // stmt
        // }
        return with(JsAstUtils) {
            newJsIf(
                typeOfIs(
                    JsNameRef("WorkerGlobalScope"),
                    JsStringLiteral("undefined")
                ),
                mainInvocation
            )
        }
    }

    object WebWorkersIrToJsExtensionKey : IrToJsExtensionKey()

    override val extensionKey: IrToJsExtensionKey
        get() = WebWorkersIrToJsExtensionKey

    private fun generateEntryPointForWorker(
        workerFun: IrSimpleFunction,
        context: JsIrBackendContext,
        minimizedMemberNames: Boolean
    ): JsIrProgramFragment {
        val nameGenerator = JsNameLinkingNamer(context, minimizedMemberNames)
        val result = JsIrProgramFragment(workerFun.file.fqName.asString()).apply {
            // later we also add importScripts(...)
            declarations.statements += JsInvocation(
                nameGenerator.getNameForStaticFunction(workerFun).makeRef(),
                listOf(JsNameRef("self"))
            ).makeStmt()
        }
        result.fillImportsInfo(nameGenerator, context)

        return result
    }

    private fun JsIrProgramFragment.fillImportsInfo(
        nameGenerator: JsNameLinkingNamer,
        context: JsIrBackendContext,
        declarations: Set<IrDeclaration> = emptySet()
    ) {
        fun computeTag(declaration: IrDeclaration): String? {
            val tag = (context.irFactory as IdSignatureRetriever).declarationSignature(declaration)?.toString()

            if (tag == null && declaration !in declarations) {
                error("signature for ${declaration.render()} not found")
            }

            return tag
        }

        nameGenerator.nameMap.entries.forEach { (declaration, name) ->
            computeTag(declaration)?.let { tag ->
                this.nameBindings[tag] = name
            }
        }

        nameGenerator.imports.entries.forEach { (declaration, importExpression) ->
            val tag = computeTag(declaration) ?: error("No tag for imported declaration ${declaration.render()}")
            this.imports[tag] = importExpression
        }
    }

    override fun generateAdditionalJsIrModules(
        module: IrModuleFragment,
        context: JsIrBackendContext,
        minimizedMemberNames: Boolean
    ): List<JsIrModule> {
        val workerModulesMapping = collectWorkerFunctions(module)
            .mapValues { "${it.value.file.module.safeName}_worker_${it.key}".safeModuleName }
        transformWorkerReferences(workerModulesMapping, module)

        val workerFunctions = collectWorkerFunctions(module)
        checkDomAccessInWorker(workerFunctions.map { it.value }, context)

        return workerFunctions.map { (workerName, workerFun) ->
            val workerModuleName = workerModulesMapping[workerName] ?: workerName
            JsIrModule(
                workerModuleName,
                sanitizeName(workerModuleName),
                listOf(
                    generateEntryPointForWorker(workerFun, context, minimizedMemberNames)
                )
            )
        }
    }

    private fun transformWorkerReferences(workerFunctionToModuleName: Map<String, String>, module: IrModuleFragment) {
        val workerFqN = FqName("org.w3c.dom.Worker")
        val workerRefTransformer = object : IrElementTransformerVoid() {
            override fun visitConstructorCall(expression: IrConstructorCall): IrExpression {
                if (expression.type.classFqName == workerFqN) {
                    @Suppress("UNCHECKED_CAST")
                    val workerIdValue = expression.getValueArgument(0) as IrConst<String>
                    val workerId = workerIdValue.value
                    val transformedWorkerId = if (workerId in workerFunctionToModuleName) {
                        "./${workerFunctionToModuleName[workerId]}.js"
                    } else {
                        workerId
                    }
                    expression.putValueArgument(0, transformedWorkerId.toIrConst(workerIdValue.type))
                }
                return super.visitConstructorCall(expression)
            }
        }
        module.transformChildrenVoid(workerRefTransformer)
    }

    override fun postprocessJsAst(
        jsProgram: JsProgram,
        moduleKind: ModuleKind,
        moduleOrigin: JsModuleOrigin,
        importedJsModules: List<JsImportedModule>
    ) {
        super.postprocessJsAst(jsProgram, moduleKind, moduleOrigin, importedJsModules)

        if (moduleOrigin !is JsModuleOrigin.Extension || moduleOrigin.extensionKey != WebWorkersIrToJsExtensionKey) return

        jsProgram.globalBlock.statements.add(0, importScripts(importedJsModules))
    }

    private fun importScripts(importedModules: List<JsImportedModule>): JsStatement {
        return JsInvocation(
            JsNameRef("importScripts", JsNameRef("self")),
            importedModules.map { JsStringLiteral(it.requireName) }
        ).makeStmt()
    }
}