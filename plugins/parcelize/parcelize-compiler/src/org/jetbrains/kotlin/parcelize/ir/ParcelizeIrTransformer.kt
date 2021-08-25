/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parcelize.ANDROID_PARCELABLE_CLASS_FQNAME
import org.jetbrains.kotlin.parcelize.PARCELER_FQNAME
import org.jetbrains.kotlin.parcelize.ParcelizeSyntheticComponent
import org.jetbrains.kotlin.parcelize.serializers.ParcelizeExtensionBase
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

@OptIn(ObsoleteDescriptorBasedAPI::class)
class ParcelizeIrTransformer(private val context: IrPluginContext, private val androidSymbols: AndroidSymbols) :
    ParcelizeExtensionBase, IrElementVisitorVoid {
    private val serializerFactory = IrParcelSerializerFactory(androidSymbols)

    private val deferredOperations = mutableListOf<() -> Unit>()
    private fun defer(block: () -> Unit) = deferredOperations.add(block)

    private fun IrPluginContext.createIrBuilder(symbol: IrSymbol) =
        DeclarationIrBuilder(this, symbol, symbol.owner.startOffset, symbol.owner.endOffset)

    private val symbolMap = mutableMapOf<IrSimpleFunctionSymbol, IrSimpleFunctionSymbol>()

    private val irFactory: IrFactory = IrFactoryImpl

    fun transform(moduleFragment: IrModuleFragment) {
        moduleFragment.accept(this, null)
        deferredOperations.forEach { it() }

        // Remap broken stubs, which psi2ir generates for the synthetic descriptors coming from the ParcelizeResolveExtension.
        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val remappedSymbol = symbolMap[expression.symbol]
                    ?: return super.visitCall(expression)
                return IrCallImpl(
                    expression.startOffset, expression.endOffset, expression.type, remappedSymbol,
                    expression.typeArgumentsCount, expression.valueArgumentsCount, expression.origin,
                    expression.superQualifierSymbol
                ).apply {
                    copyTypeAndValueArgumentsFrom(expression)
                }
            }

            override fun visitFunctionReference(expression: IrFunctionReference): IrExpression {
                val remappedSymbol = symbolMap[expression.symbol]
                val remappedReflectionTarget = expression.reflectionTarget?.let { symbolMap[it] }
                if (remappedSymbol == null && remappedReflectionTarget == null)
                    return super.visitFunctionReference(expression)

                return IrFunctionReferenceImpl(
                    expression.startOffset, expression.endOffset, expression.type, remappedSymbol ?: expression.symbol,
                    expression.typeArgumentsCount, expression.valueArgumentsCount, remappedReflectionTarget,
                    expression.origin
                ).apply {
                    copyTypeAndValueArgumentsFrom(expression)
                }
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
        if (!declaration.isParcelize)
            return

        val parcelableProperties = declaration.parcelableProperties

        // If the companion extends Parceler, it can override parts of the generated implementation.
        val parcelerObject = declaration.companionObject()?.takeIf {
            it.isSubclassOfFqName(PARCELER_FQNAME.asString())
        }

        if (declaration.descriptor.hasSyntheticDescribeContents()) {
            val describeContents = declaration.addOverride(
                ANDROID_PARCELABLE_CLASS_FQNAME,
                "describeContents",
                context.irBuiltIns.intType,
                modality = Modality.OPEN
            ).apply {
                val flags = if (parcelableProperties.any { it.field.type.containsFileDescriptors }) 1 else 0
                body = context.createIrBuilder(symbol).run {
                    irExprBody(irInt(flags))
                }

                metadata = DescriptorMetadataSource.Function(
                    declaration.descriptor.findFunction(ParcelizeSyntheticComponent.ComponentKind.DESCRIBE_CONTENTS)!!
                )
            }

            declaration.functions.find {
                it.descriptor.safeAs<ParcelizeSyntheticComponent>()?.componentKind == ParcelizeSyntheticComponent.ComponentKind.DESCRIBE_CONTENTS
            }?.let { stub ->
                symbolMap[stub.symbol] = describeContents.symbol
                declaration.declarations.remove(stub)
            }
        }

        if (declaration.descriptor.hasSyntheticWriteToParcel()) {
            val writeToParcel = declaration.addOverride(
                ANDROID_PARCELABLE_CLASS_FQNAME,
                "writeToParcel",
                context.irBuiltIns.unitType,
                modality = Modality.OPEN
            ).apply {
                val receiverParameter = dispatchReceiverParameter!!
                val parcelParameter = addValueParameter("out", androidSymbols.androidOsParcel.defaultType)
                val flagsParameter = addValueParameter("flags", context.irBuiltIns.intType)

                // We need to defer the construction of the writer, since it may refer to the [writeToParcel] methods in other
                // @Parcelize classes in the current module, which might not be constructed yet at this point.
                defer {
                    body = androidSymbols.createBuilder(symbol).run {
                        irBlockBody {
                            when {
                                parcelerObject != null ->
                                    +parcelerWrite(parcelerObject, parcelParameter, flagsParameter, irGet(receiverParameter))

                                parcelableProperties.isNotEmpty() ->
                                    for (property in parcelableProperties) {
                                        +writeParcelWith(
                                            property.parceler,
                                            parcelParameter,
                                            flagsParameter,
                                            irGetField(irGet(receiverParameter), property.field)
                                        )
                                    }

                                else ->
                                    +writeParcelWith(
                                        declaration.classParceler,
                                        parcelParameter,
                                        flagsParameter,
                                        irGet(receiverParameter)
                                    )
                            }
                        }
                    }
                }

                metadata = DescriptorMetadataSource.Function(
                    declaration.descriptor.findFunction(ParcelizeSyntheticComponent.ComponentKind.WRITE_TO_PARCEL)!!
                )
            }

            declaration.functions.find {
                it.descriptor.safeAs<ParcelizeSyntheticComponent>()?.componentKind == ParcelizeSyntheticComponent.ComponentKind.WRITE_TO_PARCEL
            }?.let { stub ->
                symbolMap[stub.symbol] = writeToParcel.symbol
                declaration.declarations.remove(stub)
            }
        }

        // Since the `CREATOR` object cannot refer to the type parameters of the parcelable class we use a star projected type
        val declarationType = declaration.symbol.starProjectedType
        val creatorType = androidSymbols.androidOsParcelableCreator.typeWith(declarationType)

        if (!declaration.descriptor.hasCreatorField()) {
            declaration.addField {
                name = ParcelizeExtensionBase.CREATOR_NAME
                type = creatorType
                isStatic = true
                isFinal = true
            }.apply {
                val irField = this
                val creatorClass = irFactory.buildClass {
                    name = Name.identifier("Creator")
                    visibility = DescriptorVisibilities.LOCAL
                }.apply {
                    parent = irField
                    superTypes = listOf(creatorType)
                    createImplicitParameterDeclarationWithWrappedDescriptor()

                    addConstructor {
                        isPrimary = true
                    }.apply {
                        body = context.createIrBuilder(symbol).irBlockBody {
                            +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
                        }
                    }

                    val arrayType = context.irBuiltIns.arrayClass.typeWith(declarationType.makeNullable())
                    addFunction("newArray", arrayType).apply {
                        overriddenSymbols = listOf(androidSymbols.androidOsParcelableCreator.getSimpleFunction(name.asString())!!)
                        val sizeParameter = addValueParameter("size", context.irBuiltIns.intType)
                        body = context.createIrBuilder(symbol).run {
                            irExprBody(
                                parcelerNewArray(parcelerObject, sizeParameter)
                                    ?: irCall(androidSymbols.arrayOfNulls, arrayType).apply {
                                        putTypeArgument(0, arrayType)
                                        putValueArgument(0, irGet(sizeParameter))
                                    }
                            )
                        }
                    }

                    addFunction("createFromParcel", declarationType).apply {
                        overriddenSymbols = listOf(androidSymbols.androidOsParcelableCreator.getSimpleFunction(name.asString())!!)
                        val parcelParameter = addValueParameter("parcel", androidSymbols.androidOsParcel.defaultType)

                        // We need to defer the construction of the create method, since it may refer to the [Parcelable.Creator]
                        // instances in other @Parcelize classes in the current module, which may not exist yet.
                        defer {
                            body = androidSymbols.createBuilder(symbol).run {
                                irExprBody(
                                    when {
                                        parcelerObject != null ->
                                            parcelerCreate(parcelerObject, parcelParameter)

                                        parcelableProperties.isNotEmpty() ->
                                            irCall(declaration.primaryConstructor!!).apply {
                                                for ((index, property) in parcelableProperties.withIndex()) {
                                                    putValueArgument(index, readParcelWith(property.parceler, parcelParameter))
                                                }
                                            }

                                        else ->
                                            readParcelWith(declaration.classParceler, parcelParameter)
                                    }
                                )
                            }
                        }
                    }
                }

                initializer = context.createIrBuilder(symbol).run {
                    irExprBody(irBlock {
                        +creatorClass
                        +irCall(creatorClass.primaryConstructor!!)
                    })
                }
            }
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

    private class ParcelableProperty(val field: IrField, parcelerThunk: () -> IrParcelSerializer) {
        val parceler by lazy(parcelerThunk)
    }

    private val IrClass.classParceler: IrParcelSerializer
        get() = if (kind == ClassKind.CLASS) {
            IrNoParameterClassParcelSerializer(this)
        } else {
            serializerFactory.get(defaultType, parcelizeType = defaultType, strict = true, toplevel = true, scope = getParcelerScope())
        }

    private val IrClass.parcelableProperties: List<ParcelableProperty>
        get() {
            if (kind != ClassKind.CLASS) return emptyList()

            val constructor = primaryConstructor ?: return emptyList()
            val topLevelScope = getParcelerScope()

            return constructor.valueParameters.map { parameter ->
                val property = properties.first { it.name == parameter.name }
                val localScope = property.getParcelerScope(topLevelScope)
                ParcelableProperty(property.backingField!!) {
                    serializerFactory.get(parameter.type, parcelizeType = defaultType, scope = localScope)
                }
            }
        }

    // *Heuristic* to determine if a Parcelable contains file descriptors.
    private val IrType.containsFileDescriptors: Boolean
        get() = erasedUpperBound.fqNameWhenAvailable == ParcelizeExtensionBase.FILE_DESCRIPTOR_FQNAME ||
                (this as? IrSimpleType)?.arguments?.any { argument ->
                    argument.typeOrNull?.containsFileDescriptors == true
                } == true
}
