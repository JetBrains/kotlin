/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_FUNCTION_CLASS
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.serialization.KonanIrLinker
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

internal class CacheInfoBuilder(
        private val generationState: NativeGenerationState,
        private val moduleDeserializer: KonanIrLinker.KonanPartialModuleDeserializer,
        private val irModule: IrModuleFragment
) {
    fun build() = irModule.files.forEach { irFile ->
        var hasEagerlyInitializedProperties = false

        irFile.acceptChildrenVoid(object : IrElementVisitorVoid {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            override fun visitClass(declaration: IrClass) {
                declaration.acceptChildrenVoid(this)

                if (!declaration.isInterface && !declaration.isLocal
                        && declaration.isExported && declaration.origin != DECLARATION_ORIGIN_FUNCTION_CLASS
                ) {
                    val declaredFields = generationState.context.getLayoutBuilder(declaration).getDeclaredFields()
                    generationState.classFields.add(moduleDeserializer.buildClassFields(declaration, declaredFields))
                }
            }

            override fun visitFunction(declaration: IrFunction) {
                // Don't need to visit the children here: classes would be all local and wouldn't be handled anyway,
                // as for functions - both their callees will be handled and inline bodies will be built for the top function.

                if (!declaration.isFakeOverride && declaration.isInline && declaration.isExported) {
                    generationState.inlineFunctionBodies.add(moduleDeserializer.buildInlineFunctionReference(declaration))
                    trackCallees(declaration)
                }
            }

            override fun visitProperty(declaration: IrProperty) {
                declaration.acceptChildrenVoid(this)

                if (declaration.parent == irFile
                        && declaration.hasAnnotation(KonanFqNames.eagerInitialization)
                ) {
                    hasEagerlyInitializedProperties = true
                }
            }
        })

        if (hasEagerlyInitializedProperties)
            generationState.eagerInitializedFiles.add(moduleDeserializer.buildEagerInitializedFile(irFile))
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
                    generationState.calledFromExportedInlineFunctions.add(function)
                    (function as? IrConstructor)?.constructedClass?.let {
                        generationState.constructedFromExportedInlineFunctions.add(it)
                    }
                    if (function.isInline && !function.isExported) {
                        // An exported inline function calls a non-exported inline function:
                        // should track its callees as well as it won't be handled by the main visitor.
                        trackCallees(function)
                    }
                }
            }

            override fun visitGetObjectValue(expression: IrGetObjectValue) {
                expression.acceptChildrenVoid(this)

                processFunction(generationState.context.getObjectClassInstanceFunction(expression.symbol.owner))
            }

            override fun visitGetEnumValue(expression: IrGetEnumValue) {
                expression.acceptChildrenVoid(this)

                processFunction(generationState.context.enumsSupport.getValueGetter(expression.symbol.owner.parentAsClass))
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