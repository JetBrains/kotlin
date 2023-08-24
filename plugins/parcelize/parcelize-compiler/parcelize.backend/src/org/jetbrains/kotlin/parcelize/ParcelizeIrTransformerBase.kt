/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATE_FROM_PARCEL_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATOR_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.IGNORED_ON_PARCEL_FQ_NAMES
import org.jetbrains.kotlin.parcelize.ParcelizeNames.NEW_ARRAY_NAME
import org.jetbrains.kotlin.parcelize.serializers.ParcelizeExtensionBase

abstract class ParcelizeIrTransformerBase(
    protected val context: IrPluginContext,
    protected val androidSymbols: AndroidSymbols
) : ParcelizeExtensionBase, IrElementVisitorVoid {
    private val irFactory: IrFactory = IrFactoryImpl

    protected val deferredOperations = mutableListOf<() -> Unit>()
    protected fun defer(block: () -> Unit) = deferredOperations.add(block)

    protected fun IrSimpleFunction.generateDescribeContentsBody(parcelableProperties: List<ParcelableProperty?>) {
        val flags = if (parcelableProperties.any { it != null && it.field.type.containsFileDescriptors }) 1 else 0
        body = context.createIrBuilder(symbol).run {
            irExprBody(irInt(flags))
        }
    }

    protected fun IrSimpleFunction.generateWriteToParcelBody(
        irClass: IrClass,
        parcelerObject: IrClass?,
        parcelableProperties: List<ParcelableProperty?>,
        receiverParameter: IrValueParameter,
        parcelParameter: IrValueParameter,
        flagsParameter: IrValueParameter
    ) {
        body = androidSymbols.createBuilder(symbol).run {
            irBlockBody {
                when {
                    parcelerObject != null ->
                        +parcelerWrite(parcelerObject, parcelParameter, flagsParameter, irGet(receiverParameter))

                    parcelableProperties.isNotEmpty() ->
                        for (property in parcelableProperties) {
                            if (property != null) {
                                +writeParcelWith(
                                    property.parceler,
                                    parcelParameter,
                                    flagsParameter,
                                    irGetField(irGet(receiverParameter), property.field)
                                )
                            }
                        }

                    else ->
                        +writeParcelWith(
                            irClass.classParceler,
                            parcelParameter,
                            flagsParameter,
                            irGet(receiverParameter)
                        )
                }
            }
        }
    }

    protected fun generateCreator(declaration: IrClass, parcelerObject: IrClass?, parcelableProperties: List<ParcelableProperty?>) {
        // Since the `CREATOR` object cannot refer to the type parameters of the parcelable class we use a star projected type
        val declarationType = declaration.symbol.starProjectedType
        val creatorType = androidSymbols.androidOsParcelableCreator.typeWith(declarationType)

        declaration.addField {
            name = CREATOR_NAME
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
                addFunction(NEW_ARRAY_NAME.identifier, arrayType).apply {
                    overriddenSymbols = listOf(androidSymbols.androidOsParcelableCreator.getSimpleFunction(name.asString())!!)
                    val sizeParameter = addValueParameter("size", context.irBuiltIns.intType)
                    body = context.createIrBuilder(symbol).run {
                        irExprBody(
                            parcelerNewArray(parcelerObject, sizeParameter)
                                ?: irCall(context.irBuiltIns.arrayOfNulls, arrayType).apply {
                                    putTypeArgument(0, arrayType)
                                    putValueArgument(0, irGet(sizeParameter))
                                }
                        )
                    }
                }

                addFunction(CREATE_FROM_PARCEL_NAME.identifier, declarationType).apply {
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
                                                if (property != null) {
                                                    putValueArgument(index, readParcelWith(property.parceler, parcelParameter))
                                                }
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

    private val IrClass.classParceler: IrParcelSerializer
        get() = if (kind == ClassKind.CLASS) {
            IrNoParameterClassParcelSerializer(this)
        } else {
            serializerFactory.get(defaultType, parcelizeType = defaultType, strict = true, toplevel = true, scope = getParcelerScope())
        }

    protected class ParcelableProperty(val field: IrField, parcelerThunk: () -> IrParcelSerializer) {
        val parceler by lazy(parcelerThunk)
    }

    private val serializerFactory = IrParcelSerializerFactory(androidSymbols)

    protected val IrClass.parcelableProperties: List<ParcelableProperty?>
        get() {
            if (kind != ClassKind.CLASS) return emptyList()

            val constructor = primaryConstructor ?: return emptyList()
            val topLevelScope = getParcelerScope()

            return constructor.valueParameters.map { parameter ->
                val property = properties.firstOrNull { it.name == parameter.name }
                if (property == null || property.hasAnyAnnotation(IGNORED_ON_PARCEL_FQ_NAMES)) {
                    null
                } else {
                    val localScope = property.getParcelerScope(topLevelScope)
                    ParcelableProperty(property.backingField!!) {
                        serializerFactory.get(parameter.type, parcelizeType = defaultType, scope = localScope)
                    }
                }
            }
        }

    // *Heuristic* to determine if a Parcelable contains file descriptors.
    private val IrType.containsFileDescriptors: Boolean
        get() = erasedUpperBound.fqNameWhenAvailable == ParcelizeExtensionBase.FILE_DESCRIPTOR_FQNAME ||
                (this as? IrSimpleType)?.arguments?.any { argument ->
                    argument.typeOrNull?.containsFileDescriptors == true
                } == true

    private fun IrPluginContext.createIrBuilder(symbol: IrSymbol): DeclarationIrBuilder {
        return DeclarationIrBuilder(this, symbol, symbol.owner.startOffset, symbol.owner.endOffset)
    }
}
