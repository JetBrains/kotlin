/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.lower

import org.jetbrains.kotlin.backend.common.serialization.SerializedIrFileFingerprint
import org.jetbrains.kotlin.backend.common.serialization.SerializedKlibFingerprint
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.serialization.InlineFunctionSerializer
import org.jetbrains.kotlin.backend.konan.isCalledFromExportedInlineFunction
import org.jetbrains.kotlin.backend.konan.isConstructedFromExportedInlineFunctions
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.backend.konan.serialization.KonanPartialModuleDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.SerializedEagerInitializedFile
import org.jetbrains.kotlin.backend.konan.serialization.SerializedFileReference
import org.jetbrains.kotlin.backend.konan.serialization.SerializedRawValGetter
import org.jetbrains.kotlin.backend.konan.serialization.buildSerializedClassFields
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrBasedFunctionFactory
import org.jetbrains.kotlin.ir.IrBasedFunctionFactory.Companion.isFunctionInterfaceFile
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.objcinterop.isExternalObjCClass
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

private fun IrDeclarationWithVisibility.isExportedFromCompilerCache(moduleDeserializer: KonanPartialModuleDeserializer): Boolean {
    if (Visibilities.isPrivate(visibility.delegate)) {
        return false // Private declarations are not visible outside the compiler cache
    }
    return with(KonanManglerIr) {
        isExported(compatibleMode = moduleDeserializer.compatibilityMode.legacySignaturesForPrivateAndLocalDeclarations)
    }
}

private fun IrFunction.isInlineFunctionExportedFromCompilerCache(moduleDeserializer: KonanPartialModuleDeserializer): Boolean {
    // For older klibs (without the first-stage inliner) we need to export even private inline functions:
    // private inline foo() = ...
    // internal inline bar() = foo()
    // When `bar()` is used in a different module (e.g. a friend module), that module also needs access to the private `foo`.
    // TODO(KT-91880): remove inline function bodies from caches altogether.
    return isInline && with(KonanManglerIr) {
        isExported(compatibleMode = moduleDeserializer.compatibilityMode.legacySignaturesForPrivateAndLocalDeclarations)
    }
}

/**
 * Cache information built before the lowerings are run:
 * * `klibHash`
 * * `inlineFunctionBodies`
 * * `rawValGetters`
 *
 * Consider using [PostLoweringsCacheInfoBuilder] instead: lowerings are allowed to modify IR, collecting info after lowerings is safer.
 *
 * @see PostLoweringsCacheInfoBuilder
 */
internal class PreLoweringsCacheInfoBuilder(
        private val generationState: NativeGenerationState,
        private val moduleDeserializer: KonanPartialModuleDeserializer,
        private val irModule: IrModuleFragment
) {
    fun build() {
        if (!generationState.config.producePerFileCache)
            generationState.klibHash = SerializedKlibFingerprint(moduleDeserializer.klib.path.toFile()).klibFingerprint
        irModule.files.forEach { irFile ->
            irFile.acceptChildrenVoid(object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitFunction(declaration: IrFunction) {
                    // Don't need to visit the children here: classes would be all local and wouldn't be handled anyway,
                    // as for functions - both their callees will be handled and inline bodies will be built for the top function.

                    if (!declaration.isFakeOverride && declaration.isInlineFunctionExportedFromCompilerCache(moduleDeserializer)) {
                        val inlineFunctionSerializer = InlineFunctionSerializer(moduleDeserializer)
                        generationState.inlineFunctionBodies.add(inlineFunctionSerializer.buildInlineFunctionReference(declaration))
                        trackCallees(declaration)
                    }
                }

                override fun visitProperty(declaration: IrProperty) {
                    declaration.acceptChildrenVoid(this)

                    val getter = declaration.getter
                    if (getter != null
                            && !getter.isFakeOverride
                            && getter.isExportedFromCompilerCache(moduleDeserializer)
                            && generationState.context.isRawValGetter(getter)) {
                        val signature = getter.symbol.signature
                        if (signature != null) {
                            generationState.rawValGetters.add(
                                    SerializedRawValGetter(SerializedFileReference(irFile), signature)
                            )
                        }
                    }
                }
            })

            if (generationState.config.producePerFileCache && !irFile.isFunctionInterfaceFile)
                generationState.klibHash = SerializedIrFileFingerprint(moduleDeserializer.klib, moduleDeserializer.getKlibFileIndexOf(irFile)).fileFingerprint
        }
    }

    private val visitedInlineFunctions = mutableSetOf<IrFunction>()

    private fun trackCallees(irFunction: IrFunction) {
        if (irFunction in visitedInlineFunctions) return
        visitedInlineFunctions += irFunction

        irFunction.acceptChildrenVoid(object : IrVisitorVoid() {
            override fun visitElement(element: IrElement) {
                element.acceptChildrenVoid(this)
            }

            private fun processFunction(function: IrFunction) {
                if (generationState.context.moduleDeserializerProvider.getDeserializerOrNull(function) == null) {
                    function.isCalledFromExportedInlineFunction = true
                    (function as? IrConstructor)?.constructedClass?.isConstructedFromExportedInlineFunctions = true
                    if (function.isInline && !function.isInlineFunctionExportedFromCompilerCache(moduleDeserializer)) {
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

/**
 * Cache information built after the lowerings are run:
 * * `classFields` (class layout)
 * * `eagerInitializedFiles`
 *
 * @see PreLoweringsCacheInfoBuilder
 */
internal class PostLoweringsCacheInfoBuilder(
        private val generationState: NativeGenerationState,
        private val moduleDeserializer: KonanPartialModuleDeserializer,
        private val irModule: IrModuleFragment
) {
    fun build() {
        irModule.files.forEach { irFile ->
            var hasEagerlyInitializedProperties = false

            irFile.acceptChildrenVoid(object : IrVisitorVoid() {
                override fun visitElement(element: IrElement) {
                    element.acceptChildrenVoid(this)
                }

                override fun visitClass(declaration: IrClass) {
                    declaration.acceptChildrenVoid(this)

                    if (!declaration.isInterface && !declaration.isOriginallyLocal
                            && declaration.isExportedFromCompilerCache(moduleDeserializer) && declaration.origin != IrBasedFunctionFactory.classOrigin
                    ) {
                        val declaredFields = generationState.context.getLayoutBuilder(declaration).getDeclaredFields(generationState.llvm)
                        generationState.classFields.add(buildSerializedClassFields(declaration, declaredFields, moduleDeserializer))
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
                generationState.eagerInitializedFiles.add(SerializedEagerInitializedFile(SerializedFileReference(irFile)))
        }
    }
}
