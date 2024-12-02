/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin.GeneratedByPlugin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELER_FQN
import org.jetbrains.kotlin.parcelize.fir.ParcelizePluginKey

class ParcelizeFirIrTransformer(
    context: IrPluginContext,
    androidSymbols: AndroidSymbols,
    parcelizeAnnotations: List<FqName>,
    experimentalCodeGeneration: Boolean,
) : ParcelizeIrTransformerBase(context, androidSymbols, parcelizeAnnotations, experimentalCodeGeneration) {

    fun transform(moduleFragment: IrModuleFragment) {
        moduleFragment.accept(this, null)
        deferredOperations.forEach { it() }

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = expression.symbol.owner
                if (callee.isParcelableCreatorIntrinsic()) {
                    expression.typeArguments[0]?.getClass()?.let { parcelableClass ->
                        androidSymbols.createBuilder(expression.symbol).apply {
                            return getParcelableCreator(parcelableClass)
                        }
                    }
                }
                return expression
            }
        })
    }

    override fun visitElement(element: IrElement) = element.acceptChildren(this, null)

    override fun visitClass(declaration: IrClass) {
        declaration.acceptChildren(this, null)

        // Sealed classes can be annotated with `@Parcelize`, but that only implies that we
        // should process their immediate subclasses.
        if (!declaration.isParcelize(parcelizeAnnotations))
            return

        val parcelableProperties = declaration.parcelableProperties

        // If the companion extends Parceler, it can override parts of the generated implementation.
        val parcelerObject = declaration.companionObject()?.takeIf {
            it.isSubclassOfFqName(PARCELER_FQN.asString())
        }
        val generateInheritanceConstructor = declaration.canGenerateInheritanceConstructor()
        if (generateInheritanceConstructor) {
            // The signature for this constructor is not generated in [FirParcelizeDeclarationGenerator] so the checks
            // need to be repeated here. This is on purpose. The declaration generator does not see through `expect` and `actual`
            // which prevents us from checking if the construct's property has been marked with `IgnoredOnParcel` and should
            // not be part of this specialized constructor.
            declaration.addConstructor {}.apply {
                origin = GeneratedByPlugin(ParcelizePluginKey)
                val constructorArguments = declaration.inheritanceConstructorArguments()
                constructorArguments.forEach {
                    addValueParameter(it.field.name, it.field.type)
                }
                addValueParameter(ParcelizeNames.MARKER_NAME, androidSymbols.directInitializerMarker.defaultType)
                // Might reference constructors from super classes and those might not be yet generated.
                // This is why defer here is needed. We will have signature but not body.
                defer {
                    generateInheritanceConstructor(declaration, parcelableProperties, constructorArguments)
                }
            }
        }

        // At this point we generated constructor for the sealed class and there is nothing more to do.
        if (declaration.modality == Modality.SEALED)
            return

        for (function in declaration.functions) {
            val origin = function.origin
            if (origin !is GeneratedByPlugin || origin.pluginKey != ParcelizePluginKey) continue
            when (function.name.identifier) {
                ParcelizeSyntheticComponent.ComponentKind.DESCRIBE_CONTENTS.methodName -> {
                    function.generateDescribeContentsBody(parcelableProperties)
                }
                ParcelizeSyntheticComponent.ComponentKind.WRITE_TO_PARCEL.methodName -> {
                    function.apply {
                        val (receiverParameter, parcelParameter, flagsParameter) = function.parameters

                        // We need to defer the construction of the writer, since it may refer to the [writeToParcel] methods in other
                        // @Parcelize classes in the current module, which might not be constructed yet at this point.
                        defer {
                            if (generateInheritanceConstructor) {
                                generateWriteToParcelBodyForInheritanceConstructor(
                                    declaration,
                                    parcelableProperties,
                                    receiverParameter,
                                    parcelParameter,
                                    flagsParameter
                                )
                            } else {
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
                }
                else -> error("Generated declaration with unknown name: ${function.render()}")
            }
        }

        generateCreator(declaration, parcelerObject, parcelableProperties)
    }
}

