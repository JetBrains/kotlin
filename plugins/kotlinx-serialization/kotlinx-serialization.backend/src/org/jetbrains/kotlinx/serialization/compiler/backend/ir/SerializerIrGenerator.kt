/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.deepCopyWithVariables
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationDescriptorSerializerPlugin
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.CallingConventions
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.CACHED_CHILD_SERIALIZERS_PROPERTY_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.DECODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.ENCODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.KSERIALIZER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.LOAD
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SAVE
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.STRUCTURE_DECODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.STRUCTURE_ENCODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.UNKNOWN_FIELD_EXC
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages
import org.jetbrains.kotlinx.serialization.compiler.resolve.bitMaskSlotCount

val SERIALIZATION_PLUGIN_ORIGIN = IrDeclarationOriginImpl("KOTLINX_SERIALIZATION", true)

internal typealias FunctionWithArgs = Pair<IrFunctionSymbol, List<IrExpression>>

open class SerializerIrGenerator(
    val irClass: IrClass,
    compilerContext: SerializationPluginContext,
    metadataPlugin: SerializationDescriptorSerializerPlugin?,
) : BaseIrGenerator(irClass, compilerContext) {
    protected val serializableIrClass = compilerContext.getSerializableClassDescriptorBySerializer(irClass)!!

    protected val serialName: String = serializableIrClass.serialName()
    protected val properties = serializablePropertiesForIrBackend(serializableIrClass, metadataPlugin)
    protected val serializableProperties = properties.serializableProperties
    protected val isGeneratedSerializer = irClass.superTypes.any(IrType::isGeneratedKSerializer)

    protected val generatedSerialDescPropertyDescriptor = getProperty(
        SerialEntityNames.SERIAL_DESC_FIELD
    ) { true }?.takeIf { it.isFromPlugin(compilerContext.afterK2) }

    protected val irAnySerialDescProperty = getProperty(
        SerialEntityNames.SERIAL_DESC_FIELD,
    ) { true }

    fun getProperty(
        name: String,
        isReturnTypeOk: (IrProperty) -> Boolean
    ): IrProperty? {
        return irClass.properties.singleOrNull { it.name.asString() == name && isReturnTypeOk(it) }
    }

    var localSerializersFieldsDescriptors: List<IrProperty> = emptyList()
        private set

    // child serializers cached if serializable class is internal
    private val cachedChildSerializersProperty by lazy {
        if (isGeneratedSerializer)
            serializableIrClass.companionObject()?.properties?.singleOrNull { it.name == CACHED_CHILD_SERIALIZERS_PROPERTY_NAME }
        else null
    }

    // non-object serializers which can be cached
    private val cacheableChildSerializers by lazy {
        serializableIrClass.createCachedChildSerializers(serializableIrClass, properties.serializableProperties).map { it != null }
    }

    // null if was not found — we're in FIR
    private fun findLocalSerializersFieldDescriptors(): List<IrProperty?> {
        val count = serializableIrClass.typeParameters.size
        if (count == 0) return emptyList()
        val propNames = (0 until count).map { "${SerialEntityNames.typeArgPrefix}$it" }
        return propNames.map { name ->
            getProperty(name) { it.getter!!.returnType.isKSerializer() }
        }
    }

    protected open val serialDescImplClass: IrClassSymbol =
        compilerContext.getClassFromInternalSerializationPackage(SerialEntityNames.SERIAL_DESCRIPTOR_CLASS_IMPL)

    fun generateSerialDesc() {
        val desc = generatedSerialDescPropertyDescriptor ?: return
        val addFuncS = serialDescImplClass.functionByName(CallingConventions.addElement)

        val thisAsReceiverParameter = irClass.thisReceiver!!
        lateinit var prop: IrProperty

        // how to (auto)create backing field and getter/setter?
        compilerContext.symbolTable.withReferenceScope(irClass) {
            prop = generatePropertyMissingParts(desc, desc.name, serialDescImplClass.starProjectedType, irClass, desc.visibility)

            localSerializersFieldsDescriptors = findLocalSerializersFieldDescriptors().mapIndexed { i, prop ->
                generatePropertyMissingParts(
                    prop, Name.identifier("${SerialEntityNames.typeArgPrefix}$i"),
                    compilerContext.getClassFromRuntime(KSERIALIZER_CLASS).starProjectedType, irClass
                )
            }
        }

        irClass.addAnonymousInit {
            val localDesc = irTemporary(
                instantiateNewDescriptor(serialDescImplClass, irGet(thisAsReceiverParameter)),
                nameHint = "serialDesc"
            )

            addElementsContentToDescriptor(serialDescImplClass, localDesc, addFuncS)
            // add class annotations
            copySerialInfoAnnotationsToDescriptor(
                collectSerialInfoAnnotations(serializableIrClass),
                localDesc,
                serialDescImplClass.functionByName(CallingConventions.addClassAnnotation)
            )

            // save local descriptor to field
            +irSetField(
                IrGetValueImpl(
                    startOffset, endOffset,
                    thisAsReceiverParameter.symbol
                ),
                prop.backingField!!,
                irGet(localDesc)
            )
        }
    }

    protected open fun IrBlockBodyBuilder.instantiateNewDescriptor(
        serialDescImplClass: IrClassSymbol,
        correctThis: IrExpression
    ): IrExpression {
//        val classConstructors = compilerContext.referenceConstructors(serialDescImplClass.fqNameSafe)
        val serialClassDescImplCtor = serialDescImplClass.constructors.single { it.owner.isPrimary }
        return irInvoke(
            null, serialClassDescImplCtor,
            irString(serialName), if (isGeneratedSerializer) correctThis else irNull(), irInt(serializableProperties.size)
        )
    }

    protected open fun IrBlockBodyBuilder.addElementsContentToDescriptor(
        serialDescImplClass: IrClassSymbol,
        localDescriptor: IrVariable,
        addFunction: IrFunctionSymbol
    ) {
        fun addFieldCall(prop: IrSerializableProperty) = irInvoke(
            irGet(localDescriptor),
            addFunction,
            irString(prop.name),
            irBoolean(prop.optional),
            typeHint = compilerContext.irBuiltIns.unitType
        )

        for (classProp in serializableProperties) {
            if (classProp.transient) continue
            +addFieldCall(classProp)
            // add property annotations
            val property = classProp.ir//.getIrPropertyFrom(serializableIrClass)
            copySerialInfoAnnotationsToDescriptor(
                property.annotations,
                localDescriptor,
                serialDescImplClass.functionByName(CallingConventions.addAnnotation)
            )
        }
    }

    protected fun IrBlockBodyBuilder.copySerialInfoAnnotationsToDescriptor(
        annotations: List<IrConstructorCall>,
        receiver: IrVariable,
        method: IrFunctionSymbol
    ) {
        copyAnnotationsFrom(annotations).forEach {
            +irInvoke(irGet(receiver), method, it)
        }
    }

    fun generateGenericFieldsAndConstructor(typedConstructorDescriptor: IrConstructor) =
        addFunctionBody(typedConstructorDescriptor) { ctor ->
            // generate call to primary ctor to init serialClassDesc and super()
            val primaryCtor = irClass.primaryConstructorOrFail
            +IrDelegatingConstructorCallImpl.fromSymbolOwner(
                startOffset,
                endOffset,
                compilerContext.irBuiltIns.unitType,
                primaryCtor.symbol
            ).apply {
                irClass.typeParameters.forEachIndexed { index, irTypeParameter ->
                    putTypeArgument(index, irTypeParameter.defaultType)
                }
            }

            // store type arguments serializers in fields
            val thisAsReceiverParameter = irClass.thisReceiver!!
            ctor.valueParameters.forEachIndexed { index, param ->
                val localSerial = localSerializersFieldsDescriptors[index].backingField!!
                +irSetField(
                    IrGetValueImpl(startOffset, endOffset, thisAsReceiverParameter.symbol), localSerial, irGet(param)
                )
            }
        }

    open fun generateChildSerializersGetter(function: IrSimpleFunction) = addFunctionBody(function) { irFun ->
        val cachedChildSerializerByIndex = createCacheableChildSerializersFactory(
            cachedChildSerializersProperty,
            cacheableChildSerializers
        ) { serializableIrClass.companionObject()!! }

        val allSerializers = serializableProperties.mapIndexed { index, property ->
            requireNotNull(
                serializerTower(
                    this@SerializerIrGenerator,
                    irFun.dispatchReceiverParameter!!,
                    property,
                    cachedChildSerializerByIndex(index)
                )
            ) { "Property ${property.name} must have a serializer" }
        }

        val kSerType = ((irFun.returnType as IrSimpleType).arguments.first() as IrTypeProjection).type
        val array = createArrayOfExpression(kSerType, allSerializers)
        +irReturn(array)
    }

    open fun generateTypeParamsSerializersGetter(function: IrSimpleFunction) = addFunctionBody(function) { irFun ->
        val typeParams = serializableIrClass.typeParameters.mapIndexed { idx, _ ->
            irGetField(
                irGet(irFun.dispatchReceiverParameter!!),
                localSerializersFieldsDescriptors[idx].backingField!!
            )
        }
        val kSerType = ((irFun.returnType as IrSimpleType).arguments.first() as IrTypeProjection).type
        val array = createArrayOfExpression(kSerType, typeParams)
        +irReturn(array)
    }

    open fun generateSerializableClassProperty(property: IrProperty) {
        /* Already implemented in .generateSerialClassDesc ? */
    }

    open fun generateSave(function: IrSimpleFunction) = addFunctionBody(function) { saveFunc ->

        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, saveFunc.dispatchReceiverParameter!!.symbol)

        val kOutputClass = compilerContext.getClassFromRuntime(STRUCTURE_ENCODER_CLASS)
        val encoderClass = compilerContext.getClassFromRuntime(ENCODER_CLASS)

        val descriptorGetterSymbol = irAnySerialDescProperty?.getter!!.symbol

        val localSerialDesc = irTemporary(irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol), "desc")

        //  public fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder
        val beginFunc =
            encoderClass.functions.single { it.owner.name.asString() == CallingConventions.begin && it.owner.valueParameters.size == 1 }

        val call = irInvoke(irGet(saveFunc.valueParameters[0]), beginFunc, irGet(localSerialDesc), typeHint = kOutputClass.defaultType)
        val objectToSerialize = saveFunc.valueParameters[1]
        val localOutput = irTemporary(call, "output")

        val writeSelfFunction = serializableIrClass.findWriteSelfMethod()

        if (writeSelfFunction != null) {
            // extract Tx from KSerializer<Tx> list
            val typeArgs =
                localSerializersFieldsDescriptors.map { ir -> (ir.backingField!!.type as IrSimpleType).arguments.single().typeOrNull }
            val args = mutableListOf<IrExpression>(irGet(objectToSerialize), irGet(localOutput), irGet(localSerialDesc))
            args.addAll(localSerializersFieldsDescriptors.map { ir ->
                irGetField(
                    irGet(saveFunc.dispatchReceiverParameter!!),
                    ir.backingField!!
                )
            })
            +irInvoke(null, writeSelfFunction.symbol, typeArgs, args)
        } else {
            val propertyByParamReplacer: (ValueParameterDescriptor) -> IrExpression? =
                createPropertyByParamReplacer(serializableIrClass, serializableProperties, objectToSerialize)

            val thisSymbol = serializableIrClass.thisReceiver!!.symbol
            val initializerAdapter: (IrExpressionBody) -> IrExpression =
                createInitializerAdapter(serializableIrClass, propertyByParamReplacer, thisSymbol to { irGet(objectToSerialize) })

            val cachedChildSerializerByIndex =
                createCacheableChildSerializersFactory(cachedChildSerializersProperty, cacheableChildSerializers) {
                    serializableIrClass.companionObject()!!
                }

            serializeAllProperties(
                serializableProperties, objectToSerialize, localOutput,
                localSerialDesc, kOutputClass, ignoreIndexTo = -1, initializerAdapter, cachedChildSerializerByIndex
            ) { it, _ ->
                val ir = localSerializersFieldsDescriptors[it]
                irGetField(irGet(saveFunc.dispatchReceiverParameter!!), ir.backingField!!)
            }
        }

        // output.writeEnd(serialClassDesc)
        val wEndFunc = kOutputClass.functionByName(CallingConventions.end)
        +irInvoke(irGet(localOutput), wEndFunc, irGet(localSerialDesc))
    }

    protected fun IrBlockBodyBuilder.formEncodeDecodePropertyCall(
        encoder: IrExpression,
        dispatchReceiver: IrValueParameter,
        property: IrSerializableProperty,
        whenHaveSerializer: (serializer: IrExpression, sti: IrSerialTypeInfo) -> FunctionWithArgs,
        whenDoNot: (sti: IrSerialTypeInfo) -> FunctionWithArgs,
        cachedSerializer: IrExpression?,
        returnTypeHint: IrType? = null
    ): IrExpression = formEncodeDecodePropertyCall(
        encoder,
        property,
        whenHaveSerializer,
        whenDoNot,
        cachedSerializer,
        { it, _ ->
            val ir = localSerializersFieldsDescriptors[it]
            irGetField(irGet(dispatchReceiver), ir.backingField!!)
        },
        returnTypeHint
    )

    // returns null: Any? for boxed types and 0: <number type> for primitives
    private fun IrBuilderWithScope.defaultValueAndType(descriptor: IrProperty): Pair<IrExpression, IrType> {
        val T = descriptor.getter!!.returnType
        val defaultPrimitive: IrExpression? =
            if (T.isMarkedNullable()) null
            else when (T.getPrimitiveType()) {
                PrimitiveType.BOOLEAN -> IrConstImpl.boolean(startOffset, endOffset, T, false)
                PrimitiveType.CHAR -> IrConstImpl.char(startOffset, endOffset, T, 0.toChar())
                PrimitiveType.BYTE -> IrConstImpl.byte(startOffset, endOffset, T, 0)
                PrimitiveType.SHORT -> IrConstImpl.short(startOffset, endOffset, T, 0)
                PrimitiveType.INT -> IrConstImpl.int(startOffset, endOffset, T, 0)
                PrimitiveType.FLOAT -> IrConstImpl.float(startOffset, endOffset, T, 0.0f)
                PrimitiveType.LONG -> IrConstImpl.long(startOffset, endOffset, T, 0)
                PrimitiveType.DOUBLE -> IrConstImpl.double(startOffset, endOffset, T, 0.0)
                else -> null
            }
        return if (defaultPrimitive == null)
            T.makeNullable().let { irNull(it) to it }
        else
            defaultPrimitive to T
    }

    open fun generateLoad(function: IrSimpleFunction) = addFunctionBody(function) { loadFunc ->
        if (serializableIrClass.modality == Modality.ABSTRACT || serializableIrClass.modality == Modality.SEALED) {
            return@addFunctionBody
        }

        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, loadFunc.dispatchReceiverParameter!!.symbol)

        fun IrVariable.get() = irGet(this)

        val inputClass = compilerContext.getClassFromRuntime(STRUCTURE_DECODER_CLASS)
        val decoderClass = compilerContext.getClassFromRuntime(DECODER_CLASS)
        val descriptorGetterSymbol = irAnySerialDescProperty?.getter!!.symbol
        val localSerialDesc = irTemporary(irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol), "desc")

        // workaround due to unavailability of labels (KT-25386)
        val flagVar = irTemporary(irBoolean(true), "flag", isMutable = true)

        val indexVar = irTemporary(irInt(0), "index", isMutable = true)

        // calculating bit mask vars
        val blocksCnt = serializableProperties.bitMaskSlotCount()

        val serialPropertiesIndexes = serializableProperties
            .mapIndexed { i, property -> property to i }
            .associate { (p, i) -> p.ir to i }

        val transients = serializableIrClass.declarations.asSequence()
            .filterIsInstance<IrProperty>()
            .filter { !serialPropertiesIndexes.contains(it) }
            .filter { it.backingField != null }

        // var bitMask0 = 0, bitMask1 = 0...
        val bitMasks = (0 until blocksCnt).map { irTemporary(irInt(0), "bitMask$it", isMutable = true) }
        // var local0 = null, local1 = null ...
        val serialPropertiesMap = serializableProperties.mapIndexed { i, prop -> i to prop.ir }.associate { (i, descriptor) ->
            val (expr, type) = defaultValueAndType(descriptor)
            descriptor to irTemporary(expr, "local$i", type, isMutable = true)
        }
        // var transient0 = null, transient0 = null ...
        val transientsPropertiesMap = transients.mapIndexed { i, prop -> i to prop }.associate { (i, descriptor) ->
            val (expr, type) = defaultValueAndType(descriptor)
            descriptor to irTemporary(expr, "transient$i", type, isMutable = true)
        }

        //input = input.beginStructure(...)
        val beginFunc =
            decoderClass.functions.single { it.owner.name.asString() == CallingConventions.begin && it.owner.valueParameters.size == 1 }
        val call = irInvoke(
            irGet(loadFunc.valueParameters[0]),
            beginFunc,
            irGet(localSerialDesc),
            typeHint = inputClass.defaultType
        )
        val localInput = irTemporary(call, "input")

        val cachedChildSerializerByIndex = createCacheableChildSerializersFactory(
            cachedChildSerializersProperty,
            cacheableChildSerializers
        ) { serializableIrClass.companionObject()!! }

        // prepare all .decodeXxxElement calls
        val decoderCalls: List<Pair<Int, IrExpression>> =
            serializableProperties.mapIndexed { index, property ->
                val body = irBlock {
                    val decodeFuncToCall =
                        formEncodeDecodePropertyCall(localInput.get(), loadFunc.dispatchReceiverParameter!!, property, { innerSerial, sti ->
                            inputClass.functions.single {
                                it.owner.name.asString() == "${CallingConventions.decode}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}" &&
                                        it.owner.valueParameters.size == 4
                            } to listOf(
                                localSerialDesc.get(), irInt(index), innerSerial, serialPropertiesMap.getValue(property.ir).get()
                            )
                        }, { sti ->
                                                         inputClass.functions.single {
                                                             it.owner.name.asString() == "${CallingConventions.decode}${sti.elementMethodPrefix}${CallingConventions.elementPostfix}" &&
                                                                     it.owner.valueParameters.size == 2
                                                         } to listOf(localSerialDesc.get(), irInt(index))
                                                     }, cachedChildSerializerByIndex(index), returnTypeHint = property.type)
                    // local$i = localInput.decode...(...)
                    +irSet(
                        serialPropertiesMap.getValue(property.ir).symbol,
                        decodeFuncToCall
                    )
                    // bitMask[i] |= 1 << x
                    val bitPos = 1 shl (index % 32)
                    val or = irBinOp(OperatorNameConventions.OR, bitMasks[index / 32].get(), irInt(bitPos))
                    +irSet(bitMasks[index / 32].symbol, or)
                }
                index to body
            }

        // if (decoder.decodeSequentially())
        val decodeSequentiallyCall = irInvoke(localInput.get(), inputClass.functionByName(CallingConventions.decodeSequentially))

        val sequentialPart = irBlock {
            decoderCalls.forEach { (_, expr) -> +expr.deepCopyWithVariables() }
        }

        val byIndexPart: IrExpression = irWhile().also { loop ->
            loop.condition = flagVar.get()
            loop.body = irBlock {
                val readElementF = inputClass.functionByName(CallingConventions.decodeElementIndex)
                +irSet(indexVar.symbol, irInvoke(localInput.get(), readElementF, localSerialDesc.get()))
                +irWhen {
                    // if index == -1 (READ_DONE) break loop
                    +IrBranchImpl(irEquals(indexVar.get(), irInt(-1)), irSet(flagVar.symbol, irBoolean(false)))

                    decoderCalls.forEach { (i, e) -> +IrBranchImpl(irEquals(indexVar.get(), irInt(i)), e) }

                    // throw exception on unknown field

                    val excClassRef = compilerContext.referenceConstructors(
                        ClassId(
                            SerializationPackages.packageFqName,
                            Name.identifier(UNKNOWN_FIELD_EXC)
                        )
                    )
                        .single { it.owner.valueParameters.singleOrNull()?.type?.isInt() == true }
                    +elseBranch(
                        irThrow(
                            irInvoke(
                                null,
                                excClassRef,
                                indexVar.get()
                            )
                        )
                    )
                }
            }
        }

        +irIfThenElse(compilerContext.irBuiltIns.unitType, decodeSequentiallyCall, sequentialPart, byIndexPart)

        //input.endStructure(...)
        val endFunc = inputClass.functionByName(CallingConventions.end)
        +irInvoke(
            localInput.get(),
            endFunc,
            irGet(localSerialDesc)
        )

        val typeArgs = (loadFunc.returnType as IrSimpleType).arguments.map { (it as IrTypeProjection).type }
        val deserCtor: IrConstructorSymbol? = serializableIrClass.findSerializableSyntheticConstructor()
        if (serializableIrClass.isInternalSerializable && deserCtor != null) {
            var args: List<IrExpression> = serializableProperties.map { serialPropertiesMap.getValue(it.ir).get() }
            args = bitMasks.map { irGet(it) } + args + irNull()
            +irReturn(irInvoke(null, deserCtor, typeArgs, args))
        } else {
            if (irClass.isLocal) {
                // if the serializer is local, then the serializable class too, since they must be in the same scope
                throw CompilationException(
                    "External serializer class `${irClass.fqNameWhenAvailable}` is local. Local external serializers are not supported yet.",
                    null,
                    null
                )
            }

            generateGoldenMaskCheck(bitMasks, properties, localSerialDesc.get())

            val ctor: IrConstructorSymbol = serializableIrClass.primaryConstructorOrFail.symbol
            val params = ctor.owner.valueParameters

            val variableByParamReplacer: (ValueParameterDescriptor) -> IrExpression? = { vpd ->
                val propertyDescriptor = serializableIrClass.properties.find { it.name == vpd.name }
                if (propertyDescriptor != null) {
                    val serializable = serialPropertiesMap[propertyDescriptor]
                    (serializable ?: transientsPropertiesMap[propertyDescriptor])?.get()
                } else {
                    null
                }
            }
            val initializerAdapter: (IrExpressionBody) -> IrExpression =
                createInitializerAdapter(serializableIrClass, variableByParamReplacer)

            // constructor args:
            val ctorArgs = params.map { parameter ->
                val propertyDescriptor = serializableIrClass.properties.find { it.name == parameter.name }!!
                val serialProperty = serialPropertiesMap[propertyDescriptor]

                // null if transient
                if (serialProperty != null) {
                    val index = serialPropertiesIndexes.getValue(propertyDescriptor)
                    if (parameter.hasDefaultValue()) {
                        val propNotSeenTest =
                            irEquals(
                                irInt(0),
                                irBinOp(
                                    OperatorNameConventions.AND,
                                    bitMasks[index / 32].get(),
                                    irInt(1 shl (index % 32))
                                )
                            )

                        // if(mask$j && propertyMask == 0) local$i = <initializer>
                        val defaultValueExp = parameter.defaultValue!!
                        val expr = initializerAdapter(defaultValueExp)
                        +irIfThen(propNotSeenTest, irSet(serialProperty.symbol, expr))
                    }
                    serialProperty.get()
                } else {
                    val transientVar = transientsPropertiesMap.getValue(propertyDescriptor)
                    if (parameter.hasDefaultValue()) {
                        val defaultValueExp = parameter.defaultValue!!
                        val expr = initializerAdapter(defaultValueExp)
                        +irSet(transientVar.symbol, expr)
                    }
                    transientVar.get()
                }
            }

            val serializerVar = irTemporary(irInvoke(null, ctor, typeArgs, ctorArgs), "serializable")
            generateSetStandaloneProperties(serializerVar, serialPropertiesMap::getValue, serialPropertiesIndexes::getValue, bitMasks)
            +irReturn(irGet(serializerVar))
        }
    }

    private fun IrBlockBodyBuilder.generateSetStandaloneProperties(
        serializableVar: IrVariable,
        propVars: (IrProperty) -> IrVariable,
        propIndexes: (IrProperty) -> Int,
        bitMasks: List<IrVariable>
    ) {
        for (property in properties.serializableStandaloneProperties) {
            val localPropIndex = propIndexes(property.ir)
            // generate setter call
            val setter = property.ir.setter!!
            val propSeenTest =
                irNotEquals(
                    irInt(0),
                    irBinOp(
                        OperatorNameConventions.AND,
                        irGet(bitMasks[localPropIndex / 32]),
                        irInt(1 shl (localPropIndex % 32))
                    )
                )

            val setterInvokeExpr = irSet(setter.returnType, irGet(serializableVar), setter.symbol, irGet(propVars(property.ir)))

            +irIfThen(propSeenTest, setterInvokeExpr)
        }
    }

    fun generate() {
        val prop = generatedSerialDescPropertyDescriptor?.let { generateSerializableClassProperty(it); true } ?: false
        if (prop)
            generateSerialDesc()
        val withFir = compilerContext.afterK2
        val save = irClass.findPluginGeneratedMethod(SAVE, withFir)?.let { generateSave(it); true } ?: false
        val load = irClass.findPluginGeneratedMethod(LOAD, withFir)?.let { generateLoad(it); true } ?: false
        irClass.findPluginGeneratedMethod(SerialEntityNames.CHILD_SERIALIZERS_GETTER.identifier, withFir)
            ?.let { generateChildSerializersGetter(it) }
        irClass.findPluginGeneratedMethod(SerialEntityNames.TYPE_PARAMS_SERIALIZERS_GETTER.identifier, withFir)
            ?.let { generateTypeParamsSerializersGetter(it) }
        if (!prop && (save || load))
            generateSerialDesc()
        if (serializableIrClass.typeParameters.isNotEmpty()) {
            findSerializerConstructorForTypeArgumentsSerializers(irClass)?.takeIf { it.owner.isFromPlugin(withFir) }?.let {
                generateGenericFieldsAndConstructor(it.owner)
            }
        }
    }


    companion object {
        fun generate(
            irClass: IrClass,
            context: SerializationPluginContext,
            metadataPlugin: SerializationDescriptorSerializerPlugin?,
        ) {
            val serializableDesc = context.getSerializableClassDescriptorBySerializer(irClass) ?: return
            val generator = when {
                serializableDesc.isEnumWithLegacyGeneratedSerializer() -> SerializerForEnumsGenerator(
                    irClass,
                    context
                )
                serializableDesc.isSingleFieldValueClass -> SerializerForInlineClassGenerator(irClass, context)
                else -> SerializerIrGenerator(irClass, context, metadataPlugin)
            }
            generator.generate()
            if (irClass.isFromPlugin(context.afterK2)) {
                // replace origin only for plugin generated serializers
                irClass.origin = SERIALIZATION_PLUGIN_ORIGIN
            }
            irClass.addDefaultConstructorBodyIfAbsent(context)
            irClass.patchDeclarationParents(irClass.parent)
        }
    }
}
