/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.webworkers.compiler.workers

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.FqName

private val WEB_WORKER_ANNOTATION = FqName("kotlinx.webworkers.annotations.WebWorker")
private val DEDICATED_WORKER_GLOBAL_SCOPE = FqName("org.w3c.dom.DedicatedWorkerGlobalScope")

fun collectWorkerFunctions(moduleFragment: IrModuleFragment): Map<String, IrSimpleFunction> {
    val collector = WorkerFunctionsCollector()
    collector.visitModuleFragment(moduleFragment)
    return collector.collectedWorkers
}

private class WorkerFunctionsCollector : IrElementVisitorVoid {
    private val workerFunctions = mutableMapOf<String, IrSimpleFunction>()

    val collectedWorkers: Map<String, IrSimpleFunction>
        get() = workerFunctions

    override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
        super.visitSimpleFunction(declaration)
        val workerAnnotation = declaration.getAnnotation(WEB_WORKER_ANNOTATION) ?: return

        @Suppress("UNCHECKED_CAST")
        val workerId = (workerAnnotation.getValueArgument(0) as IrConst<String>).value

        // TODO: should be done on FE?
        require(workerId !in workerFunctions) { "Found two worker functions with the same worker id: '$workerId'" }

        workerFunctions[workerId] = declaration
    }
}