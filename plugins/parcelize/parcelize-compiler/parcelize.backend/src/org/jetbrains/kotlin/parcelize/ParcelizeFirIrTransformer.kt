/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELER_FQN
import org.jetbrains.kotlin.parcelize.fir.ParcelizePluginKey

class ParcelizeFirIrTransformer(
    context: IrPluginContext,
    androidSymbols: AndroidSymbols
) : ParcelizeIrTransformerBase(context, androidSymbols) {

    fun transform(moduleFragment: IrModuleFragment) {
        moduleFragment.accept(this, null)
        deferredOperations.forEach { it() }
    }

    override fun visitElement(element: IrElement) = element.acceptChildren(this, null)

    override fun visitClass(declaration: IrClass) {
        declaration.acceptChildren(this, null)

        // Sealed classes can be annotated with `@Parcelize`, but that only implies that we
        // should process their immediate subclasses.
        if (!declaration.isParcelize || declaration.modality == Modality.SEALED)
            return

        val parcelableProperties = declaration.parcelableProperties

        // If the companion extends Parceler, it can override parts of the generated implementation.
        val parcelerObject = declaration.companionObject()?.takeIf {
            it.isSubclassOfFqName(PARCELER_FQN.asString())
        }

        for (function in declaration.functions) {
            val origin = function.origin
            if (origin !is GeneratedByPlugin || origin.pluginKey != ParcelizePluginKey) continue
            when (function.name.identifier) {
                ParcelizeSyntheticComponent.ComponentKind.DESCRIBE_CONTENTS.methodName -> {
                    function.generateDescribeContentsBody(parcelableProperties)
                }
                ParcelizeSyntheticComponent.ComponentKind.WRITE_TO_PARCEL.methodName -> {
                    function.apply {
                        val receiverParameter = dispatchReceiverParameter!!
                        val (parcelParameter, flagsParameter) = function.valueParameters

                        // We need to defer the construction of the writer, since it may refer to the [writeToParcel] methods in other
                        // @Parcelize classes in the current module, which might not be constructed yet at this point.
                        defer {
                            generateWriteToParcelBody(
                                declaration,
                                parcelerObject,
                                parcelableProperties,
                                receiverParameter,
                                parcelParameter,
                                flagsParameter
                            )
                        }
                    }
                }
                else -> error("Generated declaration with unknown name: ${function.render()}")
            }
        }

        generateCreator(declaration, parcelerObject, parcelableProperties)
    }
}

