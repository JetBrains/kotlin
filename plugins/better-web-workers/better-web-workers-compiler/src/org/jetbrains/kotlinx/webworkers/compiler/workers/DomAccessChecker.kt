/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.webworkers.compiler.workers

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrDeclarationReference
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import java.util.*

fun checkDomAccessInWorker(workerFunctions: List<IrSimpleFunction>, context: JsIrBackendContext) {
    DomAccessChecker(context).checkReferencesFromWorkerContext(workerFunctions)
}

private class DomAccessChecker(private val context: JsIrBackendContext) {
    private val visitedElements = hashSetOf<IrDeclarationBase>()
    private val queue = LinkedList<IrDeclarationBase>()

    private class ReferenceCollector(private val visited: MutableSet<IrDeclarationBase>, private val sink: LinkedList<IrDeclarationBase>) :
        IrElementVisitorVoid {
        override fun visitElement(element: IrElement) = element.acceptChildrenVoid(this)

        override fun visitDeclarationReference(expression: IrDeclarationReference) {
            val declaration = expression.symbol.owner
            if (declaration is IrDeclarationBase && declaration !in visited) {
                sink.addLast(declaration)
                visited.add(declaration)
            }
            super.visitDeclarationReference(expression)
        }
    }

    private val refCollector = ReferenceCollector(visitedElements, queue)

    private fun isFromBrowserPackage(decl: IrDeclarationBase): Boolean {
        return decl.getPackageFragment().fqName.asString().startsWith("kotlinx.browser")
    }

    fun checkReferencesFromWorkerContext(
        contextRoots: Iterable<IrDeclarationBase>,
        predicate: (IrDeclarationBase) -> Boolean = ::isFromBrowserPackage
    ): Boolean {
        queue.addAll(contextRoots)
        visitedElements.addAll(contextRoots)

        while (!queue.isEmpty()) {
            val decl = queue.pollFirst()

            if (predicate(decl)) {
                context.report(
                    decl,
                    decl.file,
                    "Invalid access to DOM from worker context detected",
                    true
                )
                return false
            }

            refCollector.visitDeclaration(decl)
        }

        return true
    }
}