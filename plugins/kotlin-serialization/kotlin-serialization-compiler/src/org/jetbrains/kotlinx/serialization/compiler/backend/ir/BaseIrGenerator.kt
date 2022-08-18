/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.isSealed
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrVararg
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrPropertySymbol
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.*
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

abstract class BaseIrGenerator(private val currentClass: IrClass, final override val compilerContext: SerializationPluginContext): IrBuilderWithPluginContext {

    private val throwMissedFieldExceptionFunc
        = compilerContext.referenceFunctions(CallableId(SerializationPackages.internalPackageFqName, SerialEntityNames.SINGLE_MASK_FIELD_MISSING_FUNC_NAME)).singleOrNull()

    private val throwMissedFieldExceptionArrayFunc
        = compilerContext.referenceFunctions(CallableId(SerializationPackages.internalPackageFqName, SerialEntityNames.ARRAY_MASK_FIELD_MISSING_FUNC_NAME)).singleOrNull()

    private val enumSerializerFactoryFunc
        = compilerContext.referenceFunctions(CallableId(SerializationPackages.internalPackageFqName, SerialEntityNames.ENUM_SERIALIZER_FACTORY_FUNC_NAME)).singleOrNull()

    private val markedEnumSerializerFactoryFunc
        = compilerContext.referenceFunctions(CallableId(SerializationPackages.internalPackageFqName, SerialEntityNames.MARKED_ENUM_SERIALIZER_FACTORY_FUNC_NAME)).singleOrNull()

    fun useFieldMissingOptimization(): Boolean {
        return throwMissedFieldExceptionFunc != null && throwMissedFieldExceptionArrayFunc != null
    }

    private fun getClassListFromFileAnnotation(annotationFqName: FqName): List<IrClassSymbol> {
        val annotation = currentClass.fileParent.annotations.findAnnotation(annotationFqName) ?: return emptyList()
        val vararg = annotation.getValueArgument(0) as? IrVararg ?: return emptyList()
        return vararg.elements
            .mapNotNull { (it as? IrClassReference)?.symbol as? IrClassSymbol }
    }

    val contextualKClassListInCurrentFile: Set<IrClassSymbol> by lazy {
        getClassListFromFileAnnotation(
            SerializationAnnotations.contextualFqName,
        ).plus(
            getClassListFromFileAnnotation(
                SerializationAnnotations.contextualOnFileFqName,
            )
        ).toSet()
    }

    val additionalSerializersInScopeOfCurrentFile: Map<Pair<IrClassSymbol, Boolean>, IrClassSymbol> by lazy {
        getClassListFromFileAnnotation(SerializationAnnotations.additionalSerializersFqName,)
            .associateBy(
                { serializerSymbol ->
                    val kotlinType = (serializerSymbol.owner.superTypes.find(IrType::isKSerializer) as? IrSimpleType)?.arguments?.firstOrNull()?.typeOrNull
                    val classSymbol = kotlinType?.classOrNull
                        ?: throw AssertionError("Argument for ${SerializationAnnotations.additionalSerializersFqName} does not implement KSerializer or does not provide serializer for concrete type")
                    classSymbol to kotlinType.isNullable()
                },
                { it }
            )
    }

    fun IrBlockBodyBuilder.generateGoldenMaskCheck(
        seenVars: List<IrValueDeclaration>,
        properties: IrSerializableProperties,
        serialDescriptor: IrExpression
    ) {
        val fieldsMissedTest: IrExpression
        val throwErrorExpr: IrExpression

        val maskSlotCount = seenVars.size
        if (maskSlotCount == 1) {
            val goldenMask = properties.goldenMask


            throwErrorExpr = irInvoke(
                null,
                throwMissedFieldExceptionFunc!!,
                irGet(seenVars[0]),
                irInt(goldenMask),
                serialDescriptor,
                typeHint = compilerContext.irBuiltIns.unitType
            )

            fieldsMissedTest = irNotEquals(
                irInt(goldenMask),
                irBinOp(
                    OperatorNameConventions.AND,
                    irInt(goldenMask),
                    irGet(seenVars[0])
                )
            )
        } else {
            val goldenMaskList = properties.goldenMaskList

            var compositeExpression: IrExpression? = null
            for (i in goldenMaskList.indices) {
                val singleCheckExpr = irNotEquals(
                    irInt(goldenMaskList[i]),
                    irBinOp(
                        OperatorNameConventions.AND,
                        irInt(goldenMaskList[i]),
                        irGet(seenVars[i])
                    )
                )

                compositeExpression = if (compositeExpression == null) {
                    singleCheckExpr
                } else {
                    irBinOp(
                        OperatorNameConventions.OR,
                        compositeExpression,
                        singleCheckExpr
                    )
                }
            }

            fieldsMissedTest = compositeExpression!!

            throwErrorExpr = irBlock {
                +irInvoke(
                    null,
                    throwMissedFieldExceptionArrayFunc!!,
                    createPrimitiveArrayOfExpression(compilerContext.irBuiltIns.intType, goldenMaskList.indices.map { irGet(seenVars[it]) }),
                    createPrimitiveArrayOfExpression(compilerContext.irBuiltIns.intType, goldenMaskList.map { irInt(it) }),
                    serialDescriptor,
                    typeHint = compilerContext.irBuiltIns.unitType
                )
            }
        }

        +irIfThen(compilerContext.irBuiltIns.unitType, fieldsMissedTest, throwErrorExpr)
    }

    fun IrBlockBodyBuilder.serializeAllProperties(
        serializableProperties: List<IrSerializableProperty>,
        objectToSerialize: IrValueDeclaration,
        localOutput: IrValueDeclaration,
        localSerialDesc: IrValueDeclaration,
        kOutputClass: IrClassSymbol,
        ignoreIndexTo: Int,
        initializerAdapter: (IrExpressionBody) -> IrExpression,
        genericGetter: ((Int, IrType) -> IrExpression)?
    ) {

        fun IrSerializableProperty.irGet(): IrExpression {
            val ownerType = objectToSerialize.symbol.owner.type
            return getProperty(
                irGet(
                    type = ownerType,
                    variable = objectToSerialize.symbol
                ), ir
            )
        }

        for ((index, property) in serializableProperties.withIndex()) {
            if (index < ignoreIndexTo) continue
            // output.writeXxxElementValue(classDesc, index, value)
            val elementCall = formEncodeDecodePropertyCall(
                irGet(localOutput),
                property, { innerSerial, sti ->
                    val f =
                        kOutputClass.functionByName("${CallingConventions.encode}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}")
                    f to listOf(
                        irGet(localSerialDesc),
                        irInt(index),
                        innerSerial,
                        property.irGet()
                    )
                }, {
                    val f =
                        kOutputClass.functionByName("${CallingConventions.encode}${it.elementMethodPrefix}${CallingConventions.elementPostfix}")
                    val args: MutableList<IrExpression> = mutableListOf(irGet(localSerialDesc), irInt(index))
                    if (it.elementMethodPrefix != "Unit") args.add(property.irGet())
                    f to args
                },
                genericGetter
            )

            // check for call to .shouldEncodeElementDefault
            val encodeDefaults = property.ir.getEncodeDefaultAnnotationValue()
            val field =
                property.ir.backingField // Nullable when property from another module; can't compare it with default value on JS or Native
            if (!property.optional || encodeDefaults == true || field == null) {
                // emit call right away
                +elementCall
            } else {
                val partB = irNotEquals(property.irGet(), initializerAdapter(field.initializer!!))

                val condition = if (encodeDefaults == false) {
                    // drop default without call to .shouldEncodeElementDefault
                    partB
                } else {
                    // emit check:
                    // if (if (output.shouldEncodeElementDefault(this.descriptor, i)) true else {obj.prop != DEFAULT_VALUE} ) {
                    //    output.encodeIntElement(this.descriptor, i, obj.prop)// block {obj.prop != DEFAULT_VALUE} may contain several statements
                    val shouldEncodeFunc = kOutputClass.functionByName(CallingConventions.shouldEncodeDefault)
                    val partA = irInvoke(irGet(localOutput), shouldEncodeFunc, irGet(localSerialDesc), irInt(index))
                    // Ir infrastructure does not have dedicated symbol for ||, so
                    //  `a || b == if (a) true else b`, see org.jetbrains.kotlin.ir.builders.PrimitivesKt.oror
                    irIfThenElse(compilerContext.irBuiltIns.booleanType, partA, irTrue(), partB)
                }
                +irIfThen(condition, elementCall)
            }
        }
    }


    fun IrBlockBodyBuilder.formEncodeDecodePropertyCall(
        encoder: IrExpression,
        property: IrSerializableProperty,
        whenHaveSerializer: (serializer: IrExpression, sti: IrSerialTypeInfo) -> FunctionWithArgs,
        whenDoNot: (sti: IrSerialTypeInfo) -> FunctionWithArgs,
        genericGetter: ((Int, IrType) -> IrExpression)? = null,
        returnTypeHint: IrType? = null
    ): IrExpression {
        val sti = getIrSerialTypeInfo(property, compilerContext)
        val innerSerial = serializerInstance(
            sti.serializer,
            compilerContext,
            property.type,
            property.genericIndex,
            genericGetter
        )
        val (functionToCall, args: List<IrExpression>) = if (innerSerial != null) whenHaveSerializer(innerSerial, sti) else whenDoNot(sti)
        val typeArgs = if (functionToCall.descriptor.typeParameters.isNotEmpty()) listOf(property.type) else listOf()
        return irInvoke(encoder, functionToCall, typeArguments = typeArgs, valueArguments = args, returnTypeHint = returnTypeHint)
    }

    fun IrBuilderWithScope.callSerializerFromCompanion(
        thisIrType: IrType,
        typeArgs: List<IrType>,
        args: List<IrExpression>
    ): IrExpression? {
        val baseClass = thisIrType.getClass() ?: return null
        val companionClass = baseClass.companionObject() ?: return null
        val serializerProviderFunction = companionClass.declarations.singleOrNull {
            it is IrFunction && it.name == SerialEntityNames.SERIALIZER_PROVIDER_NAME && it.valueParameters.size == baseClass.typeParameters.size
        } ?: return null

        val adjustedArgs: List<IrExpression> =
            // if typeArgs.size == args.size then the serializer is custom - we need to use the actual serializers from the arguments
            if ((typeArgs.size != args.size) && (baseClass.descriptor.isSealed() || baseClass.descriptor.modality == Modality.ABSTRACT)) {
                val serializer = findStandardKotlinTypeSerializer(compilerContext, context.irBuiltIns.unitType)!!
                // workaround for sealed and abstract classes - the `serializer` function expects non-null serializers, but does not use them, so serializers of any type can be passed
                List(baseClass.typeParameters.size) { irGetObject(serializer) }
            } else {
                args
            }

        with(serializerProviderFunction as IrFunction) {
            // Note that [typeArgs] may be unused if we short-cut to e.g. SealedClassSerializer
            return irInvoke(
                irGetObject(companionClass),
                symbol,
                typeArgs.takeIf { it.size == typeParameters.size }.orEmpty(),
                adjustedArgs.takeIf { it.size == valueParameters.size }.orEmpty()
            )
        }
    }

    // Does not use sti and therefore does not perform encoder calls optimization
    fun IrBuilderWithScope.serializerTower(
        generator: SerializerIrGenerator,
        dispatchReceiverParameter: IrValueParameter,
        property: IrSerializableProperty
    ): IrExpression? {
        val nullableSerClass = compilerContext.referenceProperties(SerialEntityNames.wrapIntoNullableCallableId).single()
        val serializer =
            property.serializableWith(compilerContext)
                ?: if (!property.type.isTypeParameter()) generator.findTypeSerializerOrContext(
                    compilerContext,
                    property.type
                ) else null
        return serializerInstance(
            serializer,
            compilerContext,
            property.type,
            genericIndex = property.genericIndex
        ) { it, _ ->
            val (_, ir) = generator.localSerializersFieldsDescriptors[it]
            irGetField(irGet(dispatchReceiverParameter), ir.backingField!!)
        }?.let { expr -> wrapWithNullableSerializerIfNeeded(property.type, expr, nullableSerClass) }
    }

    private fun IrBuilderWithScope.wrapWithNullableSerializerIfNeeded(
        type: IrType,
        expression: IrExpression,
        nullableProp: IrPropertySymbol
    ): IrExpression = if (type.isMarkedNullable()) {
        val resultType = type.makeNotNull()
        val typeArguments = listOf(resultType)
        val callee = nullableProp.owner.getter!!

        val returnType = callee.returnType.substitute(callee.typeParameters, typeArguments)

        irInvoke(
            callee = callee.symbol,
            typeArguments = typeArguments,
            valueArguments = emptyList(),
            returnTypeHint = returnType
        ).apply { extensionReceiver = expression }
    } else {
        expression
    }

    fun wrapIrTypeIntoKSerializerIrType(
        type: IrType,
        variance: Variance = Variance.INVARIANT
    ): IrType {
        val kSerClass = compilerContext.referenceClass(ClassId(SerializationPackages.packageFqName, SerialEntityNames.KSERIALIZER_NAME))
            ?: error("Couldn't find class ${SerialEntityNames.KSERIALIZER_NAME}")
        return IrSimpleTypeImpl(
            kSerClass, hasQuestionMark = false, arguments = listOf(
                makeTypeProjection(type, variance)
            ), annotations = emptyList()
        )
    }

    fun IrBuilderWithScope.serializerInstance(
        serializerClassOriginal: IrClassSymbol?,
        pluginContext: SerializationPluginContext,
        kType: IrType,
        genericIndex: Int? = null,
        genericGetter: ((Int, IrType) -> IrExpression)? = null
    ): IrExpression? {
        val nullableSerClass = compilerContext.referenceProperties(SerialEntityNames.wrapIntoNullableCallableId).single()
        if (serializerClassOriginal == null) {
            if (genericIndex == null) return null
            return genericGetter?.invoke(genericIndex, kType)
        }
        if (serializerClassOriginal.owner.kind == ClassKind.OBJECT) {
            return irGetObject(serializerClassOriginal)
        }
        fun instantiate(serializer: IrClassSymbol?, type: IrType): IrExpression? {
            val expr = serializerInstance(
                serializer,
                pluginContext,
                type,
                type.genericIndex,
                genericGetter
            ) ?: return null
            return wrapWithNullableSerializerIfNeeded(type, expr, nullableSerClass)
        }

        var serializerClass = serializerClassOriginal
        var args: List<IrExpression>
        var typeArgs: List<IrType>
        val thisIrType = (kType as? IrSimpleType) ?: error("Don't know how to work with type ${kType::class}")
        var needToCopyAnnotations = false

        when (serializerClassOriginal.owner.classId) {
            polymorphicSerializerId -> {
                needToCopyAnnotations = true
                args = listOf(classReference(kType.classOrUpperBound()!!))
                typeArgs = listOf(thisIrType)
            }
            contextSerializerId -> {
                args = listOf(classReference(kType.classOrUpperBound()!!))
                typeArgs = listOf(thisIrType)

                val hasNewCtxSerCtor = compilerContext.referenceConstructors(contextSerializerId).any { it.owner.valueParameters.size == 3 }

                if (hasNewCtxSerCtor) {
                    // new signature of context serializer
                    args = args + mutableListOf<IrExpression>().apply {
                        val fallbackDefaultSerializer = findTypeSerializer(pluginContext, kType)
                        add(instantiate(fallbackDefaultSerializer, kType) ?: irNull())
                        add(
                            createArrayOfExpression(
                                wrapIrTypeIntoKSerializerIrType(
                                    thisIrType,
                                    variance = Variance.OUT_VARIANCE
                                ),
                                thisIrType.arguments.map {
                                    val argSer = findTypeSerializerOrContext(
                                        compilerContext,
                                        it.typeOrNull!! //todo: handle star projections here?
                                    )
                                    instantiate(argSer, it.typeOrNull!!)!!
                                })
                        )
                    }
                }
            }
            objectSerializerId -> {
                needToCopyAnnotations = true
                args = listOf(irString(kType.serialName()), irGetObject(kType.classOrUpperBound()!!))
                typeArgs = listOf(thisIrType)
            }
            sealedSerializerId -> {
                needToCopyAnnotations = true
                args = mutableListOf<IrExpression>().apply {
                    add(irString(kType.serialName()))
                    add(classReference(kType.classOrUpperBound()!!))
                    val (subclasses, subSerializers) = allSealedSerializableSubclassesFor(
                        kType.classOrUpperBound()!!.owner,
                        pluginContext
                    )
                    val projectedOutCurrentKClass =
                        compilerContext.irBuiltIns.kClassClass.typeWithArguments(
                            listOf(makeTypeProjection(thisIrType, Variance.OUT_VARIANCE))
                        )
                    add(
                        createArrayOfExpression(
                            projectedOutCurrentKClass,
                            subclasses.map { classReference(it.classOrUpperBound()!!) }
                        )
                    )
                    add(
                        createArrayOfExpression(
                            wrapIrTypeIntoKSerializerIrType(thisIrType, variance = Variance.OUT_VARIANCE),
                            subSerializers.mapIndexed { i, serializer ->
                                val type = subclasses[i]
                                val expr = serializerInstance(
                                    serializer,
                                    pluginContext,
                                    type,
                                    type.genericIndex
                                ) { _, genericType ->
                                    serializerInstance(
                                        pluginContext.referenceClass(polymorphicSerializerId),
                                        pluginContext,
                                        (genericType.classifierOrNull as IrTypeParameterSymbol).owner.representativeUpperBound
                                    )!!
                                }!!
                                wrapWithNullableSerializerIfNeeded(type, expr, nullableSerClass)
                            }
                        )
                    )
                }
                typeArgs = listOf(thisIrType)
            }
            enumSerializerId -> {
                serializerClass = pluginContext.referenceClass(enumSerializerId)
                val enumDescriptor = kType.classOrNull!!
                typeArgs = listOf(thisIrType)
                // instantiate serializer only inside enum Companion
                if (this@BaseIrGenerator !is SerializableCompanionIrGenerator) {
                    // otherwise call Companion.serializer()
                    callSerializerFromCompanion(thisIrType, typeArgs, emptyList())?.let { return it }
                }

                val enumArgs = mutableListOf(
                    irString(thisIrType.serialName()),
                    irCall(enumDescriptor.owner.findEnumValuesMethod()),
                )

                val enumSerializerFactoryFunc = enumSerializerFactoryFunc
                val markedEnumSerializerFactoryFunc = markedEnumSerializerFactoryFunc
                if (enumSerializerFactoryFunc != null && markedEnumSerializerFactoryFunc != null) {
                    // runtime contains enum serializer factory functions
                    val factoryFunc: IrSimpleFunctionSymbol = if (enumDescriptor.owner.isEnumWithSerialInfoAnnotation()) {
                        // need to store SerialInfo annotation in descriptor
                        val enumEntries = enumDescriptor.owner.enumEntries()
                        val entriesNames = enumEntries.map { it.annotations.serialNameValue?.let { n -> irString(n) } ?: irNull() }
                        val entriesAnnotations = enumEntries.map {
                            val annotationConstructors = it.annotations.map { a ->
                                a.deepCopyWithVariables()
                            }
                            val annotationsConstructors = copyAnnotationsFrom(annotationConstructors)
                            if (annotationsConstructors.isEmpty()) {
                                irNull()
                            } else {
                                createArrayOfExpression(compilerContext.irBuiltIns.annotationType, annotationsConstructors)
                            }
                        }
                        val annotationArrayType =
                            compilerContext.irBuiltIns.arrayClass.typeWith(compilerContext.irBuiltIns.annotationType.makeNullable())

                        enumArgs += createArrayOfExpression(compilerContext.irBuiltIns.stringType.makeNullable(), entriesNames)
                        enumArgs += createArrayOfExpression(annotationArrayType, entriesAnnotations)

                        markedEnumSerializerFactoryFunc
                    } else {
                        enumSerializerFactoryFunc
                    }

                    val factoryReturnType = factoryFunc.owner.returnType.substitute(factoryFunc.owner.typeParameters, typeArgs)
                    return irInvoke(null, factoryFunc, typeArgs, enumArgs, factoryReturnType)
                } else {
                    // support legacy serializer instantiation by constructor for old runtimes
                    args = enumArgs
                }
            }
            else -> {
                args = kType.arguments.map {
                    val argSer = findTypeSerializerOrContext(
                        pluginContext,
                        it.typeOrNull!! // todo: stars?
                    )
                    instantiate(argSer, it.typeOrNull!!) ?: return null
                }
                typeArgs = kType.arguments.map { it.typeOrNull!! }
            }

        }
        if (serializerClassOriginal.owner.classId == referenceArraySerializerId) {
            args = listOf(wrapperClassReference(kType.arguments.single().typeOrNull!!)) + args
            typeArgs = listOf(typeArgs[0].makeNotNull()) + typeArgs
        }

        // If KType is interface, .classSerializer always yields PolymorphicSerializer, which may be unavailable for interfaces from other modules
        if (!kType.isInterface() && serializerClassOriginal == kType.classOrUpperBound()?.owner.classSerializer(pluginContext) && this@BaseIrGenerator !is SerializableCompanionIrGenerator) {
            // This is default type serializer, we can shortcut through Companion.serializer()
            // BUT not during generation of this method itself
            callSerializerFromCompanion(thisIrType, typeArgs, args)?.let { return it }
        }


        val serializable = serializerClass?.owner?.let { getSerializableClassDescriptorBySerializer(it) }
        requireNotNull(serializerClass)
        val ctor = if (serializable?.typeParameters?.isNotEmpty() == true) {
            requireNotNull(
                findSerializerConstructorForTypeArgumentsSerializers(serializerClass.owner)
            ) { "Generated serializer does not have constructor with required number of arguments" }
        } else {
            val constructors = serializerClass.constructors
            // search for new signature of polymorphic/sealed/contextual serializer
            if (!needToCopyAnnotations) {
                constructors.single { it.owner.isPrimary }
            } else {
                constructors.find { it.owner.lastArgumentIsAnnotationArray() } ?: run {
                    // not found - we are using old serialization runtime without this feature
                    // todo: optimize allocating an empty array when no annotations defined, maybe use old constructor?
                    needToCopyAnnotations = false
                    constructors.single { it.owner.isPrimary }
                }
            }
        }
        // Return type should be correctly substituted
        assert(ctor.isBound)
        val ctorDecl = ctor.owner
        if (needToCopyAnnotations) {
            val classAnnotations = copyAnnotationsFrom(thisIrType.getClass()?.let { collectSerialInfoAnnotations(it) }.orEmpty())
            args = args + createArrayOfExpression(compilerContext.irBuiltIns.annotationType, classAnnotations)
        }

        val typeParameters = ctorDecl.parentAsClass.typeParameters
        val substitutedReturnType = ctorDecl.returnType.substitute(typeParameters, typeArgs)
        return irInvoke(
            null,
            ctor,
            // User may declare serializer with fixed type arguments, e.g. class SomeSerializer : KSerializer<ClosedRange<Float>>
            typeArguments = typeArgs.takeIf { it.size == ctorDecl.typeParameters.size }.orEmpty(),
            valueArguments = args.takeIf { it.size == ctorDecl.valueParameters.size }.orEmpty(),
            returnTypeHint = substitutedReturnType
        )
    }

}