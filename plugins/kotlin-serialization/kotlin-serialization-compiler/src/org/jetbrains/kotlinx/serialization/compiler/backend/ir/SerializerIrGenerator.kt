/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.builtins.PrimitiveType
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.platform.jvm.isJvm
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.components.hasDefaultValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerialTypeInfo
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializerCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.getSerialTypeInfo
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationDescriptorSerializerPlugin
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.DECODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.ENCODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESCRIPTOR_CLASS_IMPL
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.STRUCTURE_DECODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.STRUCTURE_ENCODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.UNKNOWN_FIELD_EXC

object SERIALIZABLE_PLUGIN_ORIGIN : IrDeclarationOriginImpl("SERIALIZER", true)

internal typealias FunctionWithArgs = Pair<IrFunctionSymbol, List<IrExpression>>

open class SerializerIrGenerator(
    val irClass: IrClass,
    final override val compilerContext: SerializationPluginContext,
    bindingContext: BindingContext,
    metadataPlugin: SerializationDescriptorSerializerPlugin?,
    private val serialInfoJvmGenerator: SerialInfoImplJvmIrGenerator,
) : SerializerCodegen(irClass.descriptor, bindingContext, metadataPlugin), IrBuilderExtension {
    protected val serializableIrClass = compilerContext.symbolTable.referenceClass(serializableDescriptor).owner
    protected val irAnySerialDescProperty = anySerialDescProperty?.let { compilerContext.symbolTable.referenceProperty(it) }


    protected open val serialDescImplClass: ClassDescriptor = serializerDescriptor
        .getClassFromInternalSerializationPackage(SERIAL_DESCRIPTOR_CLASS_IMPL)

    override fun generateSerialDesc() {
        val desc: PropertyDescriptor = generatedSerialDescPropertyDescriptor ?: return
        val addFuncS = serialDescImplClass.referenceFunctionSymbol(CallingConventions.addElement)

        val thisAsReceiverParameter = irClass.thisReceiver!!
        lateinit var prop: IrProperty

        // how to (auto)create backing field and getter/setter?
        compilerContext.symbolTable.withReferenceScope(irClass) {
            prop = generateSimplePropertyWithBackingField(desc, irClass)

            // TODO: Do not use descriptors here
            localSerializersFieldsDescriptors = findLocalSerializersFieldDescriptors().map { descriptor ->
                descriptor to generateSimplePropertyWithBackingField(descriptor, irClass)
            }
        }

        val anonymousInit = irClass.run {
            val symbol = IrAnonymousInitializerSymbolImpl(descriptor)
            irClass.factory.createAnonymousInitializer(startOffset, endOffset, SERIALIZABLE_PLUGIN_ORIGIN, symbol).also {
                it.parent = this
                declarations.add(it)
            }
        }

        anonymousInit.buildWithScope { initIrBody ->
            compilerContext.symbolTable.withReferenceScope(initIrBody) {
                initIrBody.body =
                    DeclarationIrBuilder(compilerContext, initIrBody.symbol, initIrBody.startOffset, initIrBody.endOffset).irBlockBody {
                        val localDesc = irTemporary(
                            instantiateNewDescriptor(serialDescImplClass, irGet(thisAsReceiverParameter)),
                            nameHint = "serialDesc"
                        )

                        addElementsContentToDescriptor(serialDescImplClass, localDesc, addFuncS)
                        // add class annotations
                        copySerialInfoAnnotationsToDescriptor(
                            serializableIrClass.annotations,
                            localDesc,
                            serialDescImplClass.referenceFunctionSymbol(CallingConventions.addClassAnnotation)
                        )

                        // save local descriptor to field
                        +irSetField(
                            generateReceiverExpressionForFieldAccess(
                                thisAsReceiverParameter.symbol,
                                generatedSerialDescPropertyDescriptor
                            ),
                            prop.backingField!!,
                            irGet(localDesc)
                        )
                    }
            }
        }
    }

    protected open fun IrBlockBodyBuilder.instantiateNewDescriptor(
        serialDescImplClass: ClassDescriptor,
        correctThis: IrExpression
    ): IrExpression {
        val classConstructors = compilerContext.referenceConstructors(serialDescImplClass.fqNameSafe)
        val serialClassDescImplCtor = classConstructors.single { it.owner.isPrimary }
        return irInvoke(
            null, serialClassDescImplCtor,
            irString(serialName), if (isGeneratedSerializer) correctThis else irNull(), irInt(serializableProperties.size)
        )
    }

    protected open fun IrBlockBodyBuilder.addElementsContentToDescriptor(
        serialDescImplClass: ClassDescriptor,
        localDescriptor: IrVariable,
        addFunction: IrFunctionSymbol
    ) {
        fun addFieldCall(prop: SerializableProperty) = irInvoke(
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
            val property = classProp.getIrPropertyFrom(serializableIrClass)
            copySerialInfoAnnotationsToDescriptor(
                property.annotations,
                localDescriptor,
                serialDescImplClass.referenceFunctionSymbol(CallingConventions.addAnnotation)
            )
        }
    }

    protected fun IrBlockBodyBuilder.copySerialInfoAnnotationsToDescriptor(
        annotations: List<IrConstructorCall>,
        receiver: IrVariable,
        method: IrFunctionSymbol
    ) {
        for (annotationCall in annotations) {
            val annotationClass = annotationCall.symbol.owner.parentAsClass
            if (!annotationClass.descriptor.isSerialInfoAnnotation) continue

            val createAnnotation = if (compilerContext.platform.isJvm()) {
                val implClass = serialInfoJvmGenerator.getImplClass(annotationClass)
                val ctor = implClass.constructors.singleOrNull { it.valueParameters.size == annotationCall.valueArgumentsCount }
                    ?: error("No constructor args found for SerialInfo annotation Impl class: ${implClass.render()}")
                irCall(ctor).apply {
                    for (i in 0 until annotationCall.valueArgumentsCount) {
                        putValueArgument(i, annotationCall.getValueArgument(i)!!.deepCopyWithVariables())
                    }
                }
            } else {
                annotationCall.deepCopyWithVariables()
            }

            +irInvoke(irGet(receiver), method, createAnnotation)
        }
    }

    override fun generateGenericFieldsAndConstructor(typedConstructorDescriptor: ClassConstructorDescriptor) =
        irClass.contributeConstructor(typedConstructorDescriptor) { ctor ->
            // generate call to primary ctor to init serialClassDesc and super()
            val primaryCtor = irClass.constructors.find { it.isPrimary }
                ?: throw AssertionError("Serializer class must have primary constructor")
            +IrDelegatingConstructorCallImpl.fromSymbolDescriptor(
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
                val localSerial = localSerializersFieldsDescriptors[index].second.backingField!!
                +irSetField(generateReceiverExpressionForFieldAccess(
                    thisAsReceiverParameter.symbol,
                    localSerializersFieldsDescriptors[index].first
                ), localSerial, irGet(param))
            }


        }

    override fun generateChildSerializersGetter(function: FunctionDescriptor) = irClass.contributeFunction(function) { irFun ->
        val allSerializers = serializableProperties.map {
            requireNotNull(
                serializerTower(this@SerializerIrGenerator, irFun.dispatchReceiverParameter!!, it)
            ) { "Property ${it.name} must have a serializer" }
        }

        val kSerType = ((irFun.returnType as IrSimpleType).arguments.first() as IrTypeProjection).type
        val array = createArrayOfExpression(kSerType, allSerializers)
        +irReturn(array)
    }

    override fun generateTypeParamsSerializersGetter(function: FunctionDescriptor) = irClass.contributeFunction(function) { irFun ->
        val typeParams = serializableDescriptor.declaredTypeParameters.mapIndexed { idx, _ ->
            irGetField(
                irGet(irFun.dispatchReceiverParameter!!),
                localSerializersFieldsDescriptors[idx].second.backingField!!
            )
        }
        val kSerType = ((irFun.returnType as IrSimpleType).arguments.first() as IrTypeProjection).type
        val array = createArrayOfExpression(kSerType, typeParams)
        +irReturn(array)
    }

    override fun generateSerializableClassProperty(property: PropertyDescriptor) {
        /* Already implemented in .generateSerialClassDesc ? */
    }

    override fun generateSave(function: FunctionDescriptor) = irClass.contributeFunction(function) { saveFunc ->

        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, saveFunc.dispatchReceiverParameter!!.symbol)

        val kOutputClass = serializerDescriptor.getClassFromSerializationPackage(STRUCTURE_ENCODER_CLASS)
        val encoderClass = serializerDescriptor.getClassFromSerializationPackage(ENCODER_CLASS)

        val descriptorGetterSymbol = irAnySerialDescProperty?.owner?.getter!!.symbol

        val localSerialDesc = irTemporary(irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol), "desc")

        //  fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): StructureEncoder
        val beginFunc = encoderClass.referenceFunctionSymbol(CallingConventions.begin) { it.valueParameters.size == 1 }

        val call = irCall(beginFunc, type = kOutputClass.defaultType.toIrType()).mapValueParametersIndexed { _, _ ->
            irGet(localSerialDesc)
        }
        // can it be done in more concise way? e.g. additional builder function?
        call.dispatchReceiver = irGet(saveFunc.valueParameters[0])
        val objectToSerialize = saveFunc.valueParameters[1]
        val localOutput = irTemporary(call, "output")

        fun SerializableProperty.irGet(): IrExpression {
            val ownerType = objectToSerialize.symbol.owner.type
            return getProperty(
                irGet(
                    type = ownerType,
                    variable = objectToSerialize.symbol
                ), getIrPropertyFrom(serializableIrClass)
            )
        }

        // Ignore comparing to default values of properties from superclass,
        // because we do not have access to their fields (and initializers), if superclass is in another module.
        // In future, IR analogue of JVM's write$Self should be implemented.
        val superClass = serializableIrClass.getSuperClassOrAny().descriptor
        val ignoreIndexTo = if (superClass.isInternalSerializable) {
            bindingContext.serializablePropertiesFor(superClass).size
        } else {
            -1
        }

        val propertyByParamReplacer: (ValueParameterDescriptor) -> IrExpression? =
            createPropertyByParamReplacer(serializableIrClass, serializableProperties, objectToSerialize, bindingContext)

        val thisSymbol = serializableIrClass.thisReceiver!!.symbol
        val initializerAdapter: (IrExpressionBody) -> IrExpression =
            createInitializerAdapter(serializableIrClass, propertyByParamReplacer, thisSymbol to { irGet(objectToSerialize) })

        //  internal serialization via virtual calls?
        for ((index, property) in serializableProperties.withIndex()) {
            // output.writeXxxElementValue(classDesc, index, value)
            val elementCall = formEncodeDecodePropertyCall(irGet(localOutput), saveFunc.dispatchReceiverParameter!!, property, {innerSerial, sti ->
                val f =
                    kOutputClass.referenceFunctionSymbol("${CallingConventions.encode}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}")
                f to listOf(
                    irGet(localSerialDesc),
                    irInt(index),
                    innerSerial,
                    property.irGet()
                )
            }, {
                val f =
                    kOutputClass.referenceFunctionSymbol("${CallingConventions.encode}${it.elementMethodPrefix}${CallingConventions.elementPostfix}")
                val args: MutableList<IrExpression> = mutableListOf(irGet(localSerialDesc), irInt(index))
                if (it.elementMethodPrefix != "Unit") args.add(property.irGet())
                f to args
            })

            // check for call to .shouldEncodeElementDefault
            if (!property.optional || index < ignoreIndexTo) {
                // emit call right away
                +elementCall
            } else {
                // emit check:
                // if ( if (output.shouldEncodeElementDefault(this.descriptor, i)) true else {obj.prop != DEFAULT_VALUE} ) {
                //    output.encodeIntElement(this.descriptor, i, obj.prop)
                // block {obj.prop != DEFAULT_VALUE} may contain several statements
                val shouldEncodeFunc = kOutputClass.referenceFunctionSymbol(CallingConventions.shouldEncodeDefault)
                val partA = irInvoke(irGet(localOutput), shouldEncodeFunc, irGet(localSerialDesc), irInt(index))
                val partB = irNotEquals(property.irGet(), initializerAdapter(property.irField.initializer!!))
                // Ir infrastructure does not have dedicated symbol for ||, so
                //  `a || b == if (a) true else b`, see org.jetbrains.kotlin.ir.builders.PrimitivesKt.oror
                val condition = irIfThenElse(compilerContext.irBuiltIns.booleanType, partA, irTrue(), partB)
                +irIfThen(condition, elementCall)
            }
        }

        // output.writeEnd(serialClassDesc)
        val wEndFunc = kOutputClass.referenceFunctionSymbol(CallingConventions.end)
        +irInvoke(irGet(localOutput), wEndFunc, irGet(localSerialDesc))
    }

    protected inline fun IrBlockBodyBuilder.formEncodeDecodePropertyCall(
        encoder: IrExpression,
        dispatchReceiver: IrValueParameter,
        property: SerializableProperty,
        whenHaveSerializer: (serializer: IrExpression, sti: SerialTypeInfo) -> FunctionWithArgs,
        whenDoNot: (sti: SerialTypeInfo) -> FunctionWithArgs,
        returnTypeHint: IrType? = null
    ): IrExpression {
        val sti = getSerialTypeInfo(property)
        val innerSerial = serializerInstance(
            this@SerializerIrGenerator,
            dispatchReceiver,
            sti.serializer,
            property.module,
            property.type,
            genericIndex = property.genericIndex
        )
        val (functionToCall, args: List<IrExpression>) = if (innerSerial != null) whenHaveSerializer(innerSerial, sti) else whenDoNot(sti)
        val typeArgs = if (functionToCall.descriptor.typeParameters.isNotEmpty()) listOf(property.type.toIrType()) else listOf()
        return irInvoke(encoder, functionToCall, typeArguments = typeArgs, valueArguments = args, returnTypeHint = returnTypeHint)
    }

    // returns null: Any? for boxed types and 0: <number type> for primitives
    private fun IrBuilderWithScope.defaultValueAndType(descriptor: PropertyDescriptor): Pair<IrExpression, IrType> {
        val kType = descriptor.returnType!!
        val T = kType.toIrType()
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
            irNull(compilerContext.irBuiltIns.anyNType) to (compilerContext.irBuiltIns.anyNType)
        else
            defaultPrimitive to T
    }

    override fun generateLoad(function: FunctionDescriptor) = irClass.contributeFunction(function) { loadFunc ->
        if (serializableDescriptor.modality == Modality.ABSTRACT || serializableDescriptor.modality == Modality.SEALED) {
            return@contributeFunction
        }

        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, loadFunc.dispatchReceiverParameter!!.symbol)

        fun IrVariable.get() = irGet(this)

        val inputClass = serializerDescriptor.getClassFromSerializationPackage(STRUCTURE_DECODER_CLASS)
        val decoderClass = serializerDescriptor.getClassFromSerializationPackage(DECODER_CLASS)
        val descriptorGetterSymbol = irAnySerialDescProperty?.owner?.getter!!.symbol
        val localSerialDesc = irTemporary(irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol), "desc")

        // workaround due to unavailability of labels (KT-25386)
        val flagVar = irTemporary(irBoolean(true), "flag", isMutable = true)

        val indexVar = irTemporary(irInt(0), "index", isMutable = true)

        // calculating bit mask vars
        val blocksCnt = serializableProperties.bitMaskSlotCount()

        val serialPropertiesIndexes = serializableProperties
            .mapIndexed { i, property -> property to i }
            .associate { (p, i) -> p.descriptor to i }

        val transients = serializableIrClass.declarations.asSequence()
            .filterIsInstance<IrProperty>()
            .filter { !serialPropertiesIndexes.containsKey(it.descriptor) }
            .filter { it.backingField != null }

        // var bitMask0 = 0, bitMask1 = 0...
        val bitMasks = (0 until blocksCnt).map { irTemporary(irInt(0), "bitMask$it", isMutable = true) }
        // var local0 = null, local1 = null ...
        val serialPropertiesMap = serializableProperties.mapIndexed { i, prop -> i to prop.descriptor }.associate { (i, descriptor) ->
            val (expr, type) = defaultValueAndType(descriptor)
            descriptor to irTemporary(expr, "local$i", type, isMutable = true)
        }
        // var transient0 = null, transient0 = null ...
        val transientsPropertiesMap = transients.mapIndexed { i, prop -> i to prop.descriptor }.associate { (i, descriptor) ->
            val (expr, type) = defaultValueAndType(descriptor)
            descriptor to irTemporary(expr, "transient$i", type, isMutable = true)
        }

        //input = input.beginStructure(...)
        val beginFunc = decoderClass.referenceFunctionSymbol(CallingConventions.begin) { it.valueParameters.size == 1 }
        val call = irInvoke(
            irGet(loadFunc.valueParameters[0]),
            beginFunc,
            irGet(localSerialDesc),
            typeHint = inputClass.defaultType.toIrType()
        )
        val localInput = irTemporary(call, "input")

        // prepare all .decodeXxxElement calls
        val decoderCalls: List<Pair<Int, IrExpression>> =
            serializableProperties.mapIndexed { index, property ->
                val body = irBlock {
                    val decodeFuncToCall = formEncodeDecodePropertyCall(localInput.get(), loadFunc.dispatchReceiverParameter!!, property, {innerSerial, sti ->
                        inputClass.referenceFunctionSymbol(
                            "${CallingConventions.decode}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}", {it.valueParameters.size == 4}
                        ) to listOf(
                            localSerialDesc.get(), irInt(index), innerSerial, serialPropertiesMap.getValue(property.descriptor).get()
                        )
                    }, {
                        inputClass.referenceFunctionSymbol(
                            "${CallingConventions.decode}${it.elementMethodPrefix}${CallingConventions.elementPostfix}", {it.valueParameters.size == 2}
                        ) to listOf(
                            localSerialDesc.get(), irInt(index)
                        )
                    }, returnTypeHint = property.type.toIrType())
                    // local$i = localInput.decode...(...)
                    +irSet(
                        serialPropertiesMap.getValue(property.descriptor).symbol,
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
        val decodeSequentiallyCall = irInvoke(localInput.get(), inputClass.referenceFunctionSymbol(CallingConventions.decodeSequentially))

        val sequentialPart = irBlock {
            decoderCalls.forEach { (_, expr) -> +expr.deepCopyWithVariables() }
        }

        val byIndexPart: IrExpression = irWhile().also { loop ->
            loop.condition = flagVar.get()
            loop.body = irBlock {
                val readElementF = inputClass.referenceFunctionSymbol(CallingConventions.decodeElementIndex)
                +irSet(indexVar.symbol, irInvoke(localInput.get(), readElementF, localSerialDesc.get()))
                +irWhen {
                    // if index == -1 (READ_DONE) break loop
                    +IrBranchImpl(irEquals(indexVar.get(), irInt(-1)), irSet(flagVar.symbol, irBoolean(false)))

                    decoderCalls.forEach { (i, e) -> +IrBranchImpl(irEquals(indexVar.get(), irInt(i)), e) }

                    // throw exception on unknown field

                    val exceptionFqn = getSerializationPackageFqn(UNKNOWN_FIELD_EXC)
                    val excClassRef = compilerContext.referenceConstructors(exceptionFqn)
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
        val endFunc = inputClass.referenceFunctionSymbol(CallingConventions.end)
        +irInvoke(
            localInput.get(),
            endFunc,
            irGet(localSerialDesc)
        )

        val typeArgs = (loadFunc.returnType as IrSimpleType).arguments.map { (it as IrTypeProjection).type }
        if (serializableDescriptor.isInternalSerializable) {
            var args: List<IrExpression> = serializableProperties.map { serialPropertiesMap.getValue(it.descriptor).get() }
            args = bitMasks.map { irGet(it) } + args + irNull()
            val ctor: IrConstructorSymbol = serializableSyntheticConstructor(serializableIrClass)
            +irReturn(irInvoke(null, ctor, typeArgs, args))
        } else {
            // check properties presence
            val serialDescIrProperty = irClass.searchForProperty(generatedSerialDescPropertyDescriptor!!)
            val serialDescriptorExpr = irGetField(null, serialDescIrProperty.backingField!!)
            generateGoldenMaskCheck(bitMasks, properties, serialDescriptorExpr)


            val ctor: IrConstructorSymbol =
                compilerContext.referenceConstructors(serializableDescriptor.fqNameSafe).single { it.owner.isPrimary }
            val params = ctor.owner.valueParameters


            val variableByParamReplacer: (ValueParameterDescriptor) -> IrExpression? = {
                val propertyDescriptor = bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, it]
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
                val parameterDescriptor = parameter.descriptor as ValueParameterDescriptor
                val propertyDescriptor = bindingContext[BindingContext.VALUE_PARAMETER_AS_PROPERTY, parameterDescriptor]!!
                val serialProperty = serialPropertiesMap[propertyDescriptor]

                // null if transient
                if (serialProperty != null) {
                    val index = serialPropertiesIndexes.getValue(propertyDescriptor)
                    if (parameterDescriptor.hasDefaultValue()) {
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
                    if (parameterDescriptor.hasDefaultValue()) {
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
        propVars: (PropertyDescriptor) -> IrVariable,
        propIndexes: (PropertyDescriptor) -> Int,
        bitMasks: List<IrVariable>
    ) {
        for (property in properties.serializableStandaloneProperties) {
            val localPropIndex = propIndexes(property.descriptor)
            // generate setter call
            val setter = property.getIrPropertyFrom(serializableIrClass).setter!!
            val propSeenTest =
                irNotEquals(
                    irInt(0),
                    irBinOp(
                        OperatorNameConventions.AND,
                        irGet(bitMasks[localPropIndex / 32]),
                        irInt(1 shl (localPropIndex % 32))
                    )
                )

            val setterInvokeExpr = irSet(setter.returnType, irGet(serializableVar), setter.symbol, irGet(propVars(property.descriptor)))

            +irIfThen(propSeenTest, setterInvokeExpr)
        }
    }

    companion object {
        fun generate(
            irClass: IrClass,
            context: SerializationPluginContext,
            bindingContext: BindingContext,
            metadataPlugin: SerializationDescriptorSerializerPlugin?,
            serialInfoJvmGenerator: SerialInfoImplJvmIrGenerator,
        ) {
            val serializableDesc = getSerializableClassDescriptorBySerializer(irClass.symbol.descriptor) ?: return
            val generator = when {
                serializableDesc.isSerializableEnum() -> SerializerForEnumsGenerator(irClass, context, bindingContext, serialInfoJvmGenerator)
                serializableDesc.isInline -> SerializerForInlineClassGenerator(irClass, context, bindingContext, serialInfoJvmGenerator)
                else -> SerializerIrGenerator(irClass, context, bindingContext, metadataPlugin, serialInfoJvmGenerator)
            }
            generator.generate()
            irClass.patchDeclarationParents(irClass.parent)
        }
    }
}
