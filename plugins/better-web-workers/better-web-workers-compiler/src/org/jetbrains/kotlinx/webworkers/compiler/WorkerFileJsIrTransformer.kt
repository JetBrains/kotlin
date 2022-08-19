/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.webworkers.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.backend.js.utils.getJsName
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid

private const val KOTLINX_WEBWORKERS_PKG = "kotlinx.webworkers"
private const val WEBWORKER_ANNOTATION_TYPE = "WebWorker"
private const val WORKER_MAIN_METHOD = "main"
private const val STDLIB_WORKER_TYPE = "org.w3c.dom.Worker"

class WorkerFileJsIrTransformer(private val pluginContext: IrPluginContext) : IrElementVisitorVoid {
    override fun visitFile(declaration: IrFile) {
        super.visitFile(declaration)
        val workerAnnotation = declaration.annotations
            .firstOrNull { it.type.matches(KOTLINX_WEBWORKERS_PKG, WEBWORKER_ANNOTATION_TYPE) } ?: return
        println(workerAnnotation.valueArgumentsCount)

//        val mainMethod = declaration.declarations
//            .filterIsInstance<IrFunction>()
//            .firstOrNull { it.getJsNameOrKotlinName().asString() == "main" }
//        println()
//
//        val factory = pluginContext.irFactory
    }

    private fun IrType.matches(packageName: String, typeName: String) =
        getSignature()?.let { sig ->
            sig.packageFqName == packageName && sig.declarationFqName == typeName
        } ?: false

    private fun IrType.getSignature(): IdSignature.CommonSignature? = classOrNull?.let { it.signature?.asPublic() }
}