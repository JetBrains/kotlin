/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_FUNCTION_CLASS
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.IrPropertyReference
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class CacheInfoBuilder(private val context: Context, private val moduleDeserializer: KonanIrLinker.KonanPartialModuleDeserializer) {
    fun build() = with(moduleDeserializer) {
        moduleFragment.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                declaration.acceptChildrenVoid(this)

                if (!declaration.isInterface && !declaration.isLocal
                        && declaration.isExported && declaration.origin != DECLARATION_ORIGIN_FUNCTION_CLASS
                ) {
                    context.generationState.classFields.add(buildClassFields(declaration, context.getLayoutBuilder(declaration).getDeclaredFields()))
                }
            }

            override fun visitFunction(declaration: IrFunction) {
                // Don't need to visit the children here: classes would be all local and wouldn't be handled anyway,
                // as for functions - both their callees will be handled and inline bodies will be built for the top function.

                if (!declaration.isFakeOverride && declaration.isInline && declaration.isExported) {
                    context.generationState.inlineFunctionBodies.add(buildInlineFunctionReference(declaration))
                    trackCallees(declaration)
                }
            }
        })
    }

    private val IrDeclaration.isExported
        get() = with(KonanManglerIr) { isExported(compatibleMode = moduleDeserializer.compatibilityMode.oldSignatures) }

    private val visitedInlineFunctions = mutableSetOf<IrFunction>()

    private fun trackCallees(irFunction: IrFunction) {
        if (irFunction in visitedInlineFunctions) return
        visitedInlineFunctions += irFunction

        irFunction.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            private fun processFunction(function: IrFunction) {
                if (function.getPackageFragment() !is IrExternalPackageFragment) {
                    context.generationState.calledFromExportedInlineFunctions.add(function)
                    (function as? IrConstructor)?.constructedClass?.let {
                        context.generationState.constructedFromExportedInlineFunctions.add(it)
                    }
                    if (function.isInline && !function.isExported) {
                        // An exported inline function calls a non-exported inline function:
                        // should track its callees as well as it won't be handled by the main visitor.
                        trackCallees(function)
                    }
                }
            }

            override fun visitCall(expression: IrCall) {
                expression.acceptChildrenVoid(this)

                processFunction(expression.symbol.owner)
            }

            override fun visitConstructorCall(expression: IrConstructorCall) {
                expression.acceptChildrenVoid(this)

                processFunction(expression.symbol.owner)
            }

            override fun visitFunctionReference(expression: IrFunctionReference) {
                expression.acceptChildrenVoid(this)

                processFunction(expression.symbol.owner)
            }

            override fun visitPropertyReference(expression: IrPropertyReference) {
                expression.acceptChildrenVoid(this)

                expression.getter?.owner?.let { processFunction(it) }
                expression.setter?.owner?.let { processFunction(it) }
            }
        })
    }
}