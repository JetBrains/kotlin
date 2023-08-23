/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.serialization.SerializedIrFileFingerprint
import org.jetbrains.kotlin.backend.common.serialization.SerializedKlibFingerprint
import org.jetbrains.kotlin.backend.konan.DECLARATION_ORIGIN_FUNCTION_CLASS
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.ir.objcinterop.isExternalObjCClass
import org.jetbrains.kotlin.backend.konan.isFunctionInterfaceFile
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.backend.konan.serialization.KonanPartialModuleDeserializer
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.library.impl.javaFile

internal class CacheInfoBuilder(
        private val generationState: NativeGenerationState,
        private val moduleDeserializer: KonanPartialModuleDeserializer,
        private val irModule: IrModuleFragment
) {
    fun build() {
        if (!generationState.config.producePerFileCache)
            generationState.klibHash = SerializedKlibFingerprint(moduleDeserializer.klib.libraryFile.javaFile()).klibFingerprint
        irModule.files.forEach { irFile ->
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
                        val declaredFields = generationState.context.getLayoutBuilder(declaration).getDeclaredFields(generationState.llvm)
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

            if (generationState.config.producePerFileCache && !irFile.isFunctionInterfaceFile)
                generationState.klibHash = SerializedIrFileFingerprint(moduleDeserializer.klib, moduleDeserializer.getKlibFileIndexOf(irFile)).fileFingerprint
        }
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
                if (generationState.context.irLinker.getCachedDeclarationModuleDeserializer(function) == null) {
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
                val singleton = expression.symbol.owner
                val isExternalObjCCompanion = singleton.isCompanion && (singleton.parent as IrClass).isExternalObjCClass()
                if (!isExternalObjCCompanion) {
                    processFunction(generationState.context.getObjectClassInstanceFunction(singleton))
                }
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