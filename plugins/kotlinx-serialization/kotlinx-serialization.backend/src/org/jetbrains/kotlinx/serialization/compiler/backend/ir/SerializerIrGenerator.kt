/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.jvm.functionByName
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.v2.irVariable
import org.jetbrains.kotlin.ir.builders.expressions.v2.irBranch
import org.jetbrains.kotlin.ir.builders.expressions.v2.irCall
import org.jetbrains.kotlin.ir.builders.expressions.v2.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.expressions.v2.irElseBranch
import org.jetbrains.kotlin.ir.builders.expressions.v2.irGetField
import org.jetbrains.kotlin.ir.builders.expressions.v2.irGetValue
import org.jetbrains.kotlin.ir.builders.expressions.v2.irReturn
import org.jetbrains.kotlin.ir.builders.expressions.v2.irSetField
import org.jetbrains.kotlin.ir.builders.expressions.v2.irSetValue
import org.jetbrains.kotlin.ir.builders.expressions.v2.irThrow
import org.jetbrains.kotlin.ir.builders.expressions.v2.irWhen
import org.jetbrains.kotlin.ir.builders.expressions.v2.irWhileLoop
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrAnnotation
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrExpressionBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.memoryOptimizedMap
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
) : BaseIrGenerator(irClass, compilerContext) {
    protected val serializableIrClass = getSerializableClassDescriptorBySerializer(irClass)!!

    protected val serialName: String = serializableIrClass.serialName()
    protected val properties = serializablePropertiesForIrBackend(serializableIrClass)
    protected val serializableProperties = properties.serializableProperties
    protected val isGeneratedSerializer = irClass.superTypes.any(IrType::isGeneratedKSerializer)

    protected val generatedSerialDescPropertyDescriptor = getProperty(
        SerialEntityNames.SERIAL_DESC_FIELD
    ) { true }?.takeIf { it.isFromPlugin() }

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
        @OptIn(ObsoleteDescriptorBasedAPI::class)
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
            val localDesc = irVariable {
                initializer = instantiateNewDescriptor(serialDescImplClass, irGetValue(thisAsReceiverParameter.symbol))
                name = Name.identifier(scope.inventNameForTemporary(nameHint = "serialDesc"))
                origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            }.also { +it }

            addElementsContentToDescriptor(serialDescImplClass, localDesc, addFuncS)
            // add class annotations
            copySerialInfoAnnotationsToDescriptor(
                collectSerialInfoAnnotations(serializableIrClass),
                localDesc,
                serialDescImplClass.functionByName(CallingConventions.addClassAnnotation)
            )

            // save local descriptor to field
            +irSetField(prop.backingField!!.symbol) {
                receiver = irGetValue(thisAsReceiverParameter.symbol)
                value = irGetValue(localDesc.symbol)
            }
        }
    }

    protected open fun IrBlockBodyBuilder.instantiateNewDescriptor(
        serialDescImplClass: IrClassSymbol,
        correctThis: IrExpression
    ): IrExpression {
//        val classConstructors = compilerContext.referenceConstructors(serialDescImplClass.fqNameSafe)
        val serialClassDescImplCtor = serialDescImplClass.constructors.single { it.owner.isPrimary }
        return irInvoke(
            serialClassDescImplCtor,
            irString(serialName), if (isGeneratedSerializer) correctThis else irNull(), irInt(serializableProperties.size)
        )
    }

    protected open fun IrBlockBodyBuilder.addElementsContentToDescriptor(
        serialDescImplClass: IrClassSymbol,
        localDescriptor: IrVariable,
        addFunction: IrFunctionSymbol
    ) {
        fun addFieldCall(prop: IrSerializableProperty) = irInvoke(
            addFunction,
            irGetValue(localDescriptor.symbol),
            irString(prop.name),
            irBoolean(prop.optional),
            returnTypeHint = compilerContext.irBuiltIns.unitType
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
        annotations: List<IrAnnotation>,
        receiver: IrVariable,
        method: IrFunctionSymbol
    ) {
        copyAnnotationsFrom(annotations).forEach {
            +irInvoke(method, irGetValue(receiver.symbol), it)
        }
    }

    fun generateGenericFieldsAndConstructor(typedConstructorDescriptor: IrConstructor) =
        addFunctionBody(typedConstructorDescriptor) { ctor ->
            // generate call to primary ctor to init serialClassDesc and super()
            val primaryCtor = irClass.primaryConstructorOrFail
            +irDelegatingConstructorCall(primaryCtor.symbol) {
                irClass.typeParameters.forEachIndexed { index, irTypeParameter ->
                    typeArguments[index] = irTypeParameter.defaultType
                }
            }

            // store type arguments serializers in fields
            val thisAsReceiverParameter = irClass.thisReceiver!!
            ctor.parameters.forEachIndexed { index, param ->
                val localSerial = localSerializersFieldsDescriptors[index].backingField!!
                +irSetField(localSerial.symbol) {
                    receiver = irGetValue(thisAsReceiverParameter.symbol)
                    value = irGetValue(param.symbol)
                }
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
                    cachedChildSerializerByIndex(index),
                    property.getTypeInScope()
                )
            ) { "Property ${property.name} must have a serializer" }
        }

        val kSerType = ((irFun.returnType as IrSimpleType).arguments.first() as IrTypeProjection).type
        val array = createArrayOfExpression(kSerType, allSerializers)
        +irReturn { value = array }
    }

    open fun generateTypeParamsSerializersGetter(function: IrSimpleFunction) = addFunctionBody(function) { irFun ->
        val typeParams = serializableIrClass.typeParameters.mapIndexed { idx, _ ->
            irGetField(localSerializersFieldsDescriptors[idx].backingField!!.symbol) {
                receiver = irGetValue(irFun.dispatchReceiverParameter!!.symbol)
            }
        }
        val kSerType = ((irFun.returnType as IrSimpleType).arguments.first() as IrTypeProjection).type
        val array = createArrayOfExpression(kSerType, typeParams)
        +irReturn { value = array }
    }

    open fun generateSerializableClassProperty(property: IrProperty) {
        /* Already implemented in .generateSerialClassDesc ? */
    }

    open fun generateSave(function: IrSimpleFunction) = addFunctionBody(function) { saveFunc ->

        fun irThis(): IrExpression =
            irGetValue(saveFunc.dispatchReceiverParameter!!.symbol)

        val kOutputClass = compilerContext.getClassFromRuntime(STRUCTURE_ENCODER_CLASS)
        val encoderClass = compilerContext.getClassFromRuntime(ENCODER_CLASS)

        val descriptorGetterSymbol = irAnySerialDescProperty?.getter!!.symbol

        val localSerialDesc = irVariable {
            initializer = irCall(descriptorGetterSymbol) {
            origin = IrStatementOrigin.GET_PROPERTY
            arguments[0] = irThis()
        }
            name = Name.identifier(scope.inventNameForTemporary(nameHint = "desc"))
            origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
        }.also { +it }

        //  public fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder
        val beginFunc =
            encoderClass.functions.single { it.owner.name.asString() == CallingConventions.begin && it.owner.hasShape(dispatchReceiver = true, regularParameters = 1) }

        val call = irInvoke(beginFunc, irGetValue(saveFunc.parameters[1].symbol), irGetValue(localSerialDesc.symbol), returnTypeHint = kOutputClass.defaultType)
        val objectToSerialize = saveFunc.parameters[2]
        val localOutput = irVariable {
            initializer = call
            name = Name.identifier(scope.inventNameForTemporary(nameHint = "output"))
            origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
        }.also { +it }

        val writeSelfFunction = serializableIrClass.findWriteSelfMethod()

        if (writeSelfFunction != null) {
            // extract Tx from KSerializer<Tx> list
            val typeArgs =
                localSerializersFieldsDescriptors.map { ir -> (ir.backingField!!.type as IrSimpleType).arguments.single().typeOrNull }
            val args = mutableListOf<IrExpression>(irGetValue(objectToSerialize.symbol), irGetValue(localOutput.symbol), irGetValue(localSerialDesc.symbol))
            args.addAll(localSerializersFieldsDescriptors.map { ir ->
                irGetField(ir.backingField!!.symbol) {
                    receiver = irGetValue(saveFunc.dispatchReceiverParameter!!.symbol)
                }
            })
            +irInvoke(writeSelfFunction.symbol, args, typeArgs)
        } else {
            val propertyByParamReplacer: (IrValueParameterSymbol) -> IrExpression? =
                createPropertyByParamReplacer(serializableIrClass, serializableProperties, objectToSerialize)

            val thisSymbol = serializableIrClass.thisReceiver!!.symbol
            val initializerAdapter: (IrExpressionBody) -> IrExpression =
                createInitializerAdapter(serializableIrClass, propertyByParamReplacer, thisSymbol to { irGetValue(objectToSerialize.symbol) })

            val cachedChildSerializerByIndex =
                createCacheableChildSerializersFactory(cachedChildSerializersProperty, cacheableChildSerializers) {
                    serializableIrClass.companionObject()!!
                }

            serializeAllProperties(
                serializableProperties, objectToSerialize, localOutput,
                localSerialDesc, kOutputClass, ignoreIndexTo = -1, initializerAdapter, cachedChildSerializerByIndex
            ) { it, _ ->
                val ir = localSerializersFieldsDescriptors[it]
                irGetField(ir.backingField!!.symbol) {
                    receiver = irGetValue(saveFunc.dispatchReceiverParameter!!.symbol)
                }
            }
        }

        // output.writeEnd(serialClassDesc)
        val wEndFunc = kOutputClass.functionByName(CallingConventions.end)
        +irInvoke(wEndFunc, irGetValue(localOutput.symbol), irGetValue(localSerialDesc.symbol))
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
            irGetField(ir.backingField!!.symbol) { receiver = irGetValue(dispatchReceiver.symbol) }
        },
        returnTypeHint,
        substitutedPropertyType = property.getTypeInScope(),
    )

    // returns null: Any? for boxed types and 0: <number type> for primitives
    private fun IrBuilderWithScope.defaultValueAndType(
        property: IrProperty,
        expectedPropertyType: IrType? = null,
    ): Pair<IrExpression, IrType> {
        val T = expectedPropertyType ?: property.getter!!.returnType
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
            irGetValue(loadFunc.dispatchReceiverParameter!!.symbol)

        fun IrVariable.get() = irGetValue(this.symbol)

        val inputClass = compilerContext.getClassFromRuntime(STRUCTURE_DECODER_CLASS)
        val decoderClass = compilerContext.getClassFromRuntime(DECODER_CLASS)
        val descriptorGetterSymbol = irAnySerialDescProperty?.getter!!.symbol
        val localSerialDesc = irVariable {
            initializer = irCall(descriptorGetterSymbol) {
            origin = IrStatementOrigin.GET_PROPERTY
            arguments[0] = irThis()
        }
            name = Name.identifier(scope.inventNameForTemporary(nameHint = "desc"))
            origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
        }.also { +it }

        // workaround due to unavailability of labels (KT-25386)
        val flagVar = irVariable {
            initializer = irBoolean(true)
            name = Name.identifier(scope.inventNameForTemporary(nameHint = "flag"))
            origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            isVar = true
        }.also { +it }

        val indexVar = irVariable {
            initializer = irInt(0)
            name = Name.identifier(scope.inventNameForTemporary(nameHint = "index"))
            origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            isVar = true
        }.also { +it }

        // calculating bit mask vars
        val blocksCnt = serializableProperties.bitMaskSlotCount()

        val serialPropertiesIndexes = serializableProperties
            .mapIndexed { i, property -> property to i }
            .associate { [p, i] -> p.ir to i }

        val transients = serializableIrClass.declarations.asSequence()
            .filterIsInstance<IrProperty>()
            .filter { !serialPropertiesIndexes.contains(it) }
            .filter { it.isNonStaticWithField }

        // var bitMask0 = 0, bitMask1 = 0...
        val bitMasks = (0 until blocksCnt).map { irVariable {
            initializer = irInt(0)
            name = Name.identifier(scope.inventNameForTemporary(nameHint = "bitMask$it"))
            origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            isVar = true
        }.also { +it } }
        // var local0 = null, local1 = null ...
        val serialPropertiesMap = serializableProperties.mapIndexed { i, prop -> i to prop }.associate { [i, serializableProp] ->
            val ir = serializableProp.ir
            val [expr, type] = defaultValueAndType(ir, serializableProp.getTypeInScope())
            ir to irVariable {
                initializer = expr
                name = Name.identifier(scope.inventNameForTemporary(nameHint = "local$i"))
                this.type = type
                origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
                isVar = true
            }.also { +it }
        }
        // var transient0 = null, transient1 = null ...
        val transientsPropertiesMap = transients.mapIndexed { i, prop -> i to prop }.associate { [i, irProperty] ->
            val [expr, type] = defaultValueAndType(irProperty)
            irProperty to irVariable {
                initializer = expr
                name = Name.identifier(scope.inventNameForTemporary(nameHint = "transient$i"))
                this.type = type
                origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
                isVar = true
            }.also { +it }
        }

        //input = input.beginStructure(...)
        val beginFunc =
            decoderClass.functions.single { it.owner.name.asString() == CallingConventions.begin && it.owner.hasShape(dispatchReceiver = true, regularParameters = 1) }
        val call = irInvoke(
            beginFunc,
            irGetValue(loadFunc.parameters[1].symbol),
            irGetValue(localSerialDesc.symbol),
            returnTypeHint = inputClass.defaultType
        )
        val localInput = irVariable {
            initializer = call
            name = Name.identifier(scope.inventNameForTemporary(nameHint = "input"))
            origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
        }.also { +it }

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
                                        it.owner.hasShape(dispatchReceiver = true, regularParameters = 4)
                            } to listOf(
                                localSerialDesc.get(), irInt(index), innerSerial, serialPropertiesMap.getValue(property.ir).get()
                            )
                        }, { sti ->
                                                         inputClass.functions.single {
                                                             it.owner.name.asString() == "${CallingConventions.decode}${sti.elementMethodPrefix}${CallingConventions.elementPostfix}" &&
                                                                     it.owner.hasShape(dispatchReceiver = true, regularParameters = 2)
                                                         } to listOf(localSerialDesc.get(), irInt(index))
                                                     }, cachedChildSerializerByIndex(index), returnTypeHint = property.getTypeInScope())
                    // local$i = localInput.decode...(...)
                    +irSetValue(serialPropertiesMap.getValue(property.ir).symbol) {
                        origin = IrStatementOrigin.EQ
                        value = decodeFuncToCall
                    }
                    // bitMask[i] |= 1 << x
                    val bitPos = 1 shl (index % 32)
                    val or = irBinOp(OperatorNameConventions.OR, bitMasks[index / 32].get(), irInt(bitPos))
                    +irSetValue(bitMasks[index / 32].symbol) {
                        origin = IrStatementOrigin.EQ
                        value = or
                    }
                }
                index to body
            }

        // if (decoder.decodeSequentially())
        val decodeSequentiallyCall = irInvoke(inputClass.functionByName(CallingConventions.decodeSequentially), localInput.get())

        val sequentialPart = irBlock {
            decoderCalls.forEach { [_, expr] -> +expr.deepCopyWithoutPatchingParents() }
        }

        fun indexEquals(index: Int): IrExpression =
            irCall(context.irBuiltIns.eqeqSymbol) {
                origin = IrStatementOrigin.EQEQ
                arguments[0] = indexVar.get()
                arguments[1] = irInt(index)
            }

        // throw exception on unknown field
        val excClassRef = compilerContext.finderForBuiltins().findConstructors(
            ClassId(
                SerializationPackages.packageFqName,
                Name.identifier(UNKNOWN_FIELD_EXC)
            )
        )
            .single { it.owner.hasShape(regularParameters = 1, parameterTypes = listOf(context.irBuiltIns.intType)) }

        val byIndexPart: IrExpression = irWhileLoop {
            condition = flagVar.get()
            body = irBlock {
                val readElementF = inputClass.functionByName(CallingConventions.decodeElementIndex)
                +irSetValue(indexVar.symbol) {
                    origin = IrStatementOrigin.EQ
                    value = irInvoke(readElementF, localInput.get(), localSerialDesc.get())
                }
                +irWhen {
                    type = compilerContext.irBuiltIns.unitType
                    // if index == -1 (READ_DONE) break loop
                    branches += irBranch {
                        condition = indexEquals(-1)
                        result = irSetValue(flagVar.symbol) {
                            origin = IrStatementOrigin.EQ
                            value = irBoolean(false)
                        }
                    }
                    decoderCalls.forEach { [i, e] ->
                        branches += irBranch {
                            condition = indexEquals(i)
                            result = e
                        }
                    }
                    branches += irElseBranch {
                        result = irThrow { value = irInvoke(excClassRef, indexVar.get()) }
                    }
                }
            }
        }

        +irWhen {
            type = compilerContext.irBuiltIns.unitType
            branches += irBranch {
                condition = decodeSequentiallyCall
                result = sequentialPart
            }
            branches += irElseBranch {
                result = byIndexPart
            }
        }

        //input.endStructure(...)
        val endFunc = inputClass.functionByName(CallingConventions.end)
        +irInvoke(
            endFunc,
            localInput.get(),
            irGetValue(localSerialDesc.symbol)
        )

        val typeArgs = (loadFunc.returnType as IrSimpleType).arguments.map { (it as IrTypeProjection).type }
        val deserCtor: IrConstructorSymbol? = serializableIrClass.findSerializableSyntheticConstructor()
        if (serializableIrClass.shouldHaveGeneratedMethods() && deserCtor != null) {
            var args: List<IrExpression> = serializableProperties.map { serialPropertiesMap.getValue(it.ir).get() }
            args = bitMasks.map { irGetValue(it.symbol) } + args + irNull()
            +irReturn { value = irInvoke(deserCtor, args, typeArgs, loadFunc.returnType) }
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

            val ctorDeclaration = serializableIrClass.primaryConstructorOrFail
            val ctor: IrConstructorSymbol = ctorDeclaration.symbol

            val variableByParamReplacer: (IrValueParameterSymbol) -> IrExpression? = { vpd ->
                val propertyDescriptor = serializableIrClass.properties.find { it.name == vpd.owner.name }
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
            val ctorArgs = ctor.owner.parameters.map { parameter ->
                val propertyDescriptor = serializableIrClass.properties.find { it.name == parameter.name }!!
                val serialProperty = serialPropertiesMap[propertyDescriptor]

                // null if transient
                if (serialProperty != null) {
                    val index = serialPropertiesIndexes.getValue(propertyDescriptor)
                    if (parameter.hasDefaultValue()) {
                        val propNotSeenTest =
                            irCall(context.irBuiltIns.eqeqSymbol) {
                                origin = IrStatementOrigin.EQEQ
                                arguments[0] = irInt(0)
                                arguments[1] = irBinOp(
                                    OperatorNameConventions.AND,
                                    bitMasks[index / 32].get(),
                                    irInt(1 shl (index % 32))
                                )
                            }

                        // if(mask$j && propertyMask == 0) local$i = <initializer>
                        val defaultValueExp = parameter.defaultValue!!
                        val expr = initializerAdapter(defaultValueExp)
                        +irWhen {
                            type = context.irBuiltIns.unitType
                            branches += irBranch {
                                condition = propNotSeenTest
                                result = irSetValue(serialProperty.symbol) {
                                    origin = IrStatementOrigin.EQ
                                    value = expr
                                }
                            }
                        }
                    }
                    serialProperty.get()
                } else {
                    val transientVar = transientsPropertiesMap.getValue(propertyDescriptor)
                    if (parameter.hasDefaultValue()) {
                        val defaultValueExp = parameter.defaultValue!!
                        val expr = initializerAdapter(defaultValueExp)
                        +irSetValue(transientVar.symbol) {
                            origin = IrStatementOrigin.EQ
                            value = expr
                        }
                    }
                    transientVar.get()
                }
            }

            val serializerVar = irVariable {
                initializer = irInvoke(ctor, ctorArgs, typeArgs)
                name = Name.identifier(scope.inventNameForTemporary(nameHint = "serializable"))
                origin = IrDeclarationOrigin.IR_TEMPORARY_VARIABLE
            }.also { +it }
            generateSetStandaloneProperties(serializerVar, serialPropertiesMap::getValue, serialPropertiesIndexes::getValue, bitMasks)
            +irReturn { value = irGetValue(serializerVar.symbol) }
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
                irCall(context.irBuiltIns.booleanNotSymbol) {
                    origin = IrStatementOrigin.EXCLEQ
                    arguments[0] = irCall(context.irBuiltIns.eqeqSymbol) {
                        origin = IrStatementOrigin.EXCLEQ
                        arguments[0] = irInt(0)
                        arguments[1] = irBinOp(
                            OperatorNameConventions.AND,
                            irGetValue(bitMasks[localPropIndex / 32].symbol),
                            irInt(1 shl (localPropIndex % 32))
                        )
                    }
                }

            val setterInvokeExpr = irCall(setter.symbol) {
                type = setter.returnType
                origin = IrStatementOrigin.EQ
                arguments[0] = irGetValue(serializableVar.symbol)
                arguments[1] = irGetValue(propVars(property.ir).symbol)
            }

            +irWhen {
                type = context.irBuiltIns.unitType
                branches += irBranch {
                    condition = propSeenTest
                    result = setterInvokeExpr
                }
            }
        }
    }

    fun generate() {
        val prop = generatedSerialDescPropertyDescriptor?.let { generateSerializableClassProperty(it); true } ?: false
        if (prop)
            generateSerialDesc()
        val save = irClass.findPluginGeneratedMethod(SAVE)?.let { generateSave(it); true } ?: false
        val load = irClass.findPluginGeneratedMethod(LOAD)?.let { generateLoad(it); true } ?: false
        irClass.findPluginGeneratedMethod(SerialEntityNames.CHILD_SERIALIZERS_GETTER.identifier)
            ?.let { generateChildSerializersGetter(it) }
        irClass.findPluginGeneratedMethod(SerialEntityNames.TYPE_PARAMS_SERIALIZERS_GETTER.identifier)
            ?.let { generateTypeParamsSerializersGetter(it) }
        if (!prop && (save || load))
            generateSerialDesc()
        if (serializableIrClass.typeParameters.isNotEmpty()) {
            findSerializerConstructorForTypeArgumentsSerializers(irClass)?.takeIf { it.owner.isFromPlugin() }?.let {
                generateGenericFieldsAndConstructor(it.owner)
            }
        }
    }

    protected fun IrSerializableProperty.getTypeInScope() =
        getSubstitutedType(serializableIrClass, irClass)

    companion object {
        fun generate(
            irClass: IrClass,
            context: SerializationPluginContext,
        ) {
            val serializableDesc = getSerializableClassDescriptorBySerializer(irClass) ?: return
            if (irClass.isFromPlugin()) {
                irClass.copyTypeParameterBoundsFrom(serializableDesc)
            }
            val generator = when {
                serializableDesc.isEnumWithLegacyGeneratedSerializer() -> SerializerForEnumsGenerator(irClass, context)
                serializableDesc.isSerializedAsInlineClass(isJvm = context.platform.isJvm()) ->
                    SerializerForInlineClassGenerator(irClass, context)
                else -> SerializerIrGenerator(irClass, context)
            }
            generator.generate()
            if (irClass.isFromPlugin()) {
                // replace origin only for plugin generated serializers
                irClass.origin = SERIALIZATION_PLUGIN_ORIGIN
            }
            irClass.addDefaultConstructorBodyIfAbsent(context)
            irClass.patchDeclarationParents(irClass.parent)
        }

        /**
         * The frontend can't copy the bounds when it generates the serializer class, because they are not
         * resolved yet at that point. Wasm erases type parameters to their bounds, so without them it emits
         * an invalid call to the serializable class's constructor (KT-89275).
         */
        private fun IrClass.copyTypeParameterBoundsFrom(serializableIrClass: IrClass) {
            for ([typeParameter, serializableTypeParameter] in typeParameters.zip(serializableIrClass.typeParameters)) {
                typeParameter.superTypes = serializableTypeParameter.superTypes.memoryOptimizedMap {
                    it.remapTypeParameters(serializableIrClass, this)
                }
            }
        }
    }
}
