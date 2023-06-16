/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.DescriptorMetadataSource
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.companionObject
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.parcelize.ParcelizeNames.DESCRIBE_CONTENTS_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.FLAGS_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELABLE_FQN
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELER_FQN
import org.jetbrains.kotlin.parcelize.ParcelizeNames.WRITE_TO_PARCEL_NAME

@OptIn(ObsoleteDescriptorBasedAPI::class)
class ParcelizeIrTransformer(
    context: IrPluginContext,
    androidSymbols: AndroidSymbols
) : ParcelizeIrTransformerBase(context, androidSymbols) {
    private val symbolMap = mutableMapOf<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>()

    fun transform(moduleFragment: IrModuleFragment) {
        moduleFragment.accept(this, null)
        deferredOperations.forEach { it() }

        // Remap broken stubs, which psi2ir generates for the synthetic descriptors coming from the ParcelizeResolveExtension.
        // Replace the `parcelableCreator` intrinsic with a direct field access.
        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = expression.symbol.owner
                if (callee.isParcelableCreatorIntrinsic()) {
                    expression.getTypeArgument(0)?.getClass()?.let { parcelableClass ->
                        androidSymbols.createBuilder(expression.symbol).apply {
                            return getParcelableCreator(parcelableClass)
                        }
                    }
                }

                // Remap calls to `describeContents` and `writeToParcel`
                expression.transformChildren(this, null)
                expression.symbol = symbolMap[expression.symbol] ?: return expression
                return expression
            }

            private fun IrSimpleFunction.isParcelableCreatorIntrinsic(): Boolean =
                dispatchReceiverParameter == null
                        && extensionReceiverParameter == null
                        && valueParameters.isEmpty()
                        && isInline
                        && fqNameWhenAvailable?.asString() == "kotlinx.parcelize.ParcelableCreatorKt.parcelableCreator"
                        && typeParameters.singleOrNull()?.let {
                    it.isReified && it.superTypes.singleOrNull()?.classFqName == PARCELABLE_FQN
                } == true

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                expression.transformChildren(this, null)
                val remappedSymbol = symbolMap[expression.symbol]
                val remappedReflectionTarget = expression.reflectionTarget?.let { symbolMap[it] }
                if (remappedSymbol == null && remappedReflectionTarget == null) return expression

                expression.symbol = remappedSymbol ?: expression.symbol
                expression.reflectionTarget = remappedReflectionTarget
                return expression
            }

            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                // Remap overridden symbols, otherwise the code might break in BridgeLowering
                declaration.overriddenSymbols = declaration.overriddenSymbols.map { symbol ->
                    symbolMap[symbol] ?: symbol
                }
                return super.visitSimpleFunction(declaration)
            }
        })
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

        if (declaration.descriptor.hasSyntheticDescribeContents()) {
            val describeContents = declaration.addOverride(
                PARCELABLE_FQN,
                DESCRIBE_CONTENTS_NAME.identifier,
                context.irBuiltIns.intType,
                modality = Modality.OPEN
            ).apply {
                generateDescribeContentsBody(parcelableProperties)

                metadata = DescriptorMetadataSource.Function(
                    declaration.descriptor.findFunction(ParcelizeSyntheticComponent.ComponentKind.DESCRIBE_CONTENTS)!!
                )
            }

            declaration.functions.find {
                (it.descriptor as? ParcelizeSyntheticComponent)?.componentKind == ParcelizeSyntheticComponent.ComponentKind.DESCRIBE_CONTENTS
            }?.let { stub ->
                symbolMap[stub.symbol] = describeContents.symbol
                declaration.declarations.remove(stub)
            }
        }

        if (declaration.descriptor.hasSyntheticWriteToParcel()) {
            val writeToParcel = declaration.addOverride(
                PARCELABLE_FQN,
                WRITE_TO_PARCEL_NAME.identifier,
                context.irBuiltIns.unitType,
                modality = Modality.OPEN
            ).apply {
                val receiverParameter = dispatchReceiverParameter!!
                val parcelParameter = addValueParameter("out", androidSymbols.androidOsParcel.defaultType)
                val flagsParameter = addValueParameter(FLAGS_NAME, context.irBuiltIns.intType)

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

                metadata = DescriptorMetadataSource.Function(
                    declaration.descriptor.findFunction(ParcelizeSyntheticComponent.ComponentKind.WRITE_TO_PARCEL)!!
                )
            }

            declaration.functions.find {
                (it.descriptor as? ParcelizeSyntheticComponent)?.componentKind == ParcelizeSyntheticComponent.ComponentKind.WRITE_TO_PARCEL
            }?.let { stub ->
                symbolMap[stub.symbol] = writeToParcel.symbol
                declaration.declarations.remove(stub)
            }
        }

        if (!declaration.descriptor.hasCreatorField()) {
            generateCreator(declaration, parcelerObject, parcelableProperties)
        }
    }

    private fun IrClass.addOverride(
        baseFqName: FqName,
        name: String,
        returnType: IrType,
        modality: Modality = Modality.FINAL
    ): IrSimpleFunction = addFunction(name, returnType, modality).apply {
        overriddenSymbols = superTypes.mapNotNull { superType ->
            superType.classOrNull?.owner?.takeIf { superClass -> superClass.isSubclassOfFqName(baseFqName.asString()) }
        }.flatMap { superClass ->
            superClass.functions.filter { function ->
                function.name.asString() == name && function.overridesFunctionIn(baseFqName)
            }.map { it.symbol }.toList()
        }
    }
}
