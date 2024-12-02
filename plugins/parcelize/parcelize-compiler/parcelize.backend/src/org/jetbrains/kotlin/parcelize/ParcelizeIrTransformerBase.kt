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
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATE_FROM_PARCEL_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.CREATOR_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.IGNORED_ON_PARCEL_FQ_NAMES
import org.jetbrains.kotlin.parcelize.ParcelizeNames.NEW_ARRAY_NAME
import org.jetbrains.kotlin.parcelize.ParcelizeNames.PARCELER_FQN
import org.jetbrains.kotlin.parcelize.fir.ParcelizePluginKey
import org.jetbrains.kotlin.parcelize.serializers.ParcelizeExtensionBase

abstract class ParcelizeIrTransformerBase(
    protected val context: IrPluginContext,
    protected val androidSymbols: AndroidSymbols,
    protected val parcelizeAnnotations: List<FqName>,
    protected val experimentalCodeGeneration: Boolean,
) : ParcelizeExtensionBase, IrElementVisitorVoid {
    private val irFactory: IrFactory = IrFactoryImpl

    protected val deferredOperations = mutableListOf<() -> Unit>()
    protected fun defer(block: () -> Unit) = deferredOperations.add(block)

    protected fun IrSimpleFunction.generateDescribeContentsBody(parcelableProperties: List<ParcelableProperty>) {
        val flags = if (parcelableProperties.any { it.field.type.containsFileDescriptors }) 1 else 0
        body = context.createIrBuilder(symbol).run {
            irExprBody(irInt(flags))
        }
    }

    protected fun IrSimpleFunction.generateWriteToParcelBodyForInheritanceConstructor(
        irClass: IrClass,
        parcelableProperties: List<ParcelableProperty>,
        receiverParameter: IrValueParameter,
        parcelParameter: IrValueParameter,
        flagsParameter: IrValueParameter,
    ) {
        require(experimentalCodeGeneration)
        body = androidSymbols.createBuilder(symbol).run {
            irBlockBody {
                val constructorArguments = irClass.inheritanceConstructorArguments()
                if (constructorArguments.isEmpty()) {
                    // In this case the parcel will be empty, which is illegal so we need to hack it here
                    +writeParcelWith(
                        irClass.classParceler, parcelParameter, flagsParameter, irGet(receiverParameter)
                    )
                    return@irBlockBody
                }
                if (irClass.superClass?.isParcelize(parcelizeAnnotations) == true) {
                    val writeToParcel = irClass.superClass?.getSimpleFunction("writeToParcel")!!
                    when (writeToParcel.owner.modality) {
                        // Can happen when the super class is a sealed class or a sealed interface.
                        Modality.ABSTRACT -> {
                            val superClassProperties = constructorArguments.dropLast(parcelableProperties.size)
                            for (property in superClassProperties) {
                                +writeParcelWith(
                                    property.parceler, parcelParameter, flagsParameter, irGetField(irGet(receiverParameter), property.field)
                                )
                            }
                        }
                        else -> {
                            +irCall(writeToParcel).apply {
                                superQualifierSymbol = irClass.superClass?.symbol
                                arguments[0] = irGet(receiverParameter)
                                arguments[1] = irGet(parcelParameter)
                                arguments[2] = irGet(flagsParameter)
                            }
                        }
                    }
                }
                for (property in parcelableProperties) {
                    +writeParcelWith(
                        property.parceler, parcelParameter, flagsParameter, irGetField(irGet(receiverParameter), property.field)
                    )
                }
            }
        }
    }

    protected fun IrSimpleFunction.generateWriteToParcelBody(
        irClass: IrClass,
        parcelerObject: IrClass?,
        parcelableProperties: List<ParcelableProperty>,
        receiverParameter: IrValueParameter,
        parcelParameter: IrValueParameter,
        flagsParameter: IrValueParameter,
    ) {
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
                            irClass.classParceler,
                            parcelParameter,
                            flagsParameter,
                            irGet(receiverParameter)
                        )
                }
            }
        }
    }

    protected fun generateCreator(declaration: IrClass, parcelerObject: IrClass?, parcelableProperties: List<ParcelableProperty>) {
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
                createThisReceiverParameter()

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
                                    typeArguments[0] = arrayType
                                    arguments[0] = irGet(sizeParameter)
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
                        val inheritanceConstructor = declaration.inheritanceConstructor()
                        body = androidSymbols.createBuilder(symbol).run {
                            irExprBody(
                                when {
                                    parcelerObject != null ->
                                        parcelerCreate(parcelerObject, parcelParameter)

                                   // just to handle empty parcel case, we need some arguments other than marker to use the constructor
                                    experimentalCodeGeneration && inheritanceConstructor != null && inheritanceConstructor.parameters.size > 1 -> {
                                        val constructorArguments = declaration.inheritanceConstructorArguments()
                                        irCall(inheritanceConstructor).apply {
                                            constructorArguments.forEachIndexed { index, property ->
                                                arguments[index] = readParcelWith(property.parceler, parcelParameter)
                                            }
                                            arguments[constructorArguments.size] = irGetObject(androidSymbols.directInitializerMarker)
                                        }
                                    }

                                    parcelableProperties.isNotEmpty() ->
                                        irCall(declaration.primaryConstructor!!).apply {
                                            for (property in parcelableProperties) {
                                                arguments[property.index] = readParcelWith(property.parceler, parcelParameter)
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

    protected class ParcelableProperty(val field: IrField, val index: Int, parcelerThunk: () -> IrParcelSerializer) {
        val parceler by lazy(parcelerThunk)
    }

    private val serializerFactory = IrParcelSerializerFactory(androidSymbols, parcelizeAnnotations)

    protected val IrClass.parcelableProperties: List<ParcelableProperty>
        get() {
            if (kind != ClassKind.CLASS) return emptyList()

            val constructor = primaryConstructor ?: return emptyList()
            val topLevelScope = getParcelerScope()
            return constructor.parameters.mapIndexedNotNull { index, parameter ->
                val property = properties.firstOrNull { it.name == parameter.name }
                if (property == null || property.hasAnyAnnotation(IGNORED_ON_PARCEL_FQ_NAMES)) {
                    return@mapIndexedNotNull null
                }
                val localScope = property.getParcelerScope(topLevelScope)
                val backingField = property.backingField ?: return@mapIndexedNotNull null
                ParcelableProperty(backingField, index) {
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

    private fun IrPluginContext.createIrBuilder(symbol: IrSymbol): DeclarationIrBuilder {
        return DeclarationIrBuilder(this, symbol, symbol.owner.startOffset, symbol.owner.endOffset)
    }

    /**
     * Generates a body of the constructor used when creating a parcelized class inheriting from other parcelized class.
     *
     * The logic is as follows:
     *  1. If the super class has a specialized constructor, delegate to it passing super call arguments
     *     and initalizer marker.
     *  2. If there is not specialized constructor in the super class use either its primary or default constructor.
     *  3. Set all the parcelable properties of the current class to their values passed as arguments.
     *  4. Run class initializers.
     *
     *  Things to note:
     *      - `parcelableProperties` references properties of the current class.
     *      - `constructorArguments` references all properties of the current inheritance chain including this class.
     *                               However, it does not contain the initializer marker.
     */
    protected fun IrConstructor.generateInheritanceConstructor(
        irClass: IrClass,
        parcelableProperties: List<ParcelableProperty>,
        constructorArguments: List<ParcelableProperty>,
    ) {
        require(experimentalCodeGeneration)
        val constructor = this
        // constructorArguments does not have a marker type present so we know there is additional
        // argument after all of the arguments
        val markerValueArgumentIndex = constructorArguments.size
        val superCallArguments = constructorArguments.dropLast(parcelableProperties.size)
        body = androidSymbols.createBuilder(symbol).run {
            irBlockBody {
                val superClass = irClass.superClass!!
                require(superClass.isParcelize(parcelizeAnnotations))
                val superClassConstructor = superClass.inheritanceConstructor()
                if (superClassConstructor != null) {
                    +irDelegatingConstructorCall(superClassConstructor).apply {
                        for (index in superCallArguments.indices) {
                            arguments[index] = irGet(constructor.parameters[index])
                        }
                        arguments[superCallArguments.size] = irGet(constructor.parameters[markerValueArgumentIndex])
                    }
                } else {
                    val constructorToCall = superClass.primaryConstructor ?: superClass.defaultConstructor
                    ?: throw RuntimeException("Class ${irClass.name} has neither primary nor default constructor. Cannot derive Parcelize constructor")
                    require(constructorToCall.isPrimary || superCallArguments.isEmpty())
                    +irDelegatingConstructorCall(constructorToCall).apply {
                        for (i in superCallArguments.indices) {
                            arguments[i] = irGet(constructor.parameters[i])
                        }
                    }
                }
                parcelableProperties.forEachIndexed { index, property ->
                    +irSetField(
                        irGet(irClass.thisReceiver!!), property.field, irGet(constructor.parameters[index + superCallArguments.size])
                    )
                }
                +IrInstanceInitializerCallImpl(startOffset, endOffset, irClass.symbol, context.irBuiltIns.unitType)
            }
        }
    }


    protected fun IrClass.inheritanceConstructorArguments(): List<ParcelableProperty> {
        val superClassArguments = if (superClass?.isParcelize(parcelizeAnnotations) == true) {
            superClass?.inheritanceConstructorArguments() ?: emptyList()
        } else {
            emptyList()
        }
        return superClassArguments + parcelableProperties
    }

    protected fun IrClass.hasCustomParcelerInChain(): Boolean {
        if (!isParcelize(parcelizeAnnotations)) return false
        return (companionObject()?.isSubclassOfFqName(PARCELER_FQN.asString()) == true) || superTypes.any {
            it.getClass()?.hasCustomParcelerInChain() == true
        }
    }

    private fun IrClass.inheritanceConstructor(): IrConstructor? {
        if (!isParcelize(parcelizeAnnotations)) return null
        return constructors.firstOrNull {
            val origin = it.origin as? IrDeclarationOrigin.GeneratedByPlugin
            origin?.pluginKey == ParcelizePluginKey
        }
    }

    protected fun IrClass.canGenerateInheritanceConstructor(): Boolean {
        return !(isEnumClass || isInterface || hasCustomParcelerInChain() || isObject)
                && (superClass?.isParcelize(parcelizeAnnotations) == true)
                && experimentalCodeGeneration
    }
}