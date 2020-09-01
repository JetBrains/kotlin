/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrBranchImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.expressions.mapValueParametersIndexed
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.withReferenceScope
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializerCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.getSerialTypeInfo
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.DECODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.ENCODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESCRIPTOR_CLASS_IMPL
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.STRUCTURE_DECODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.STRUCTURE_ENCODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.UNKNOWN_FIELD_EXC

// Is creating synthetic origin is a good idea or not?
object SERIALIZABLE_PLUGIN_ORIGIN : IrDeclarationOriginImpl("SERIALIZER")

open class SerializerIrGenerator(val irClass: IrClass, final override val compilerContext: SerializationPluginContext, bindingContext: BindingContext) :
    SerializerCodegen(irClass.descriptor, bindingContext), IrBuilderExtension {

    protected val serializableIrClass = compilerContext.symbolTable.referenceClass(serializableDescriptor).owner
    protected val irAnySerialDescProperty = anySerialDescProperty?.let { compilerContext.symbolTable.referenceProperty(it) }


    protected open val serialDescImplClass: ClassDescriptor = serializerDescriptor
            .getClassFromInternalSerializationPackage(SERIAL_DESCRIPTOR_CLASS_IMPL)

    override fun generateSerialDesc() {
        val desc: PropertyDescriptor = generatedSerialDescPropertyDescriptor ?: return
        val addFuncS = serialDescImplClass.referenceMethod(CallingConventions.addElement)

        val thisAsReceiverParameter = irClass.thisReceiver!!
        lateinit var prop: IrProperty

        // how to (auto)create backing field and getter/setter?
        compilerContext.symbolTable.withReferenceScope(irClass.descriptor) {

            prop = generateSimplePropertyWithBackingField(thisAsReceiverParameter.symbol, desc, irClass, false)

            // TODO: Do not use descriptors here
            localSerializersFieldsDescriptors.forEach {
                generateSimplePropertyWithBackingField(thisAsReceiverParameter.symbol, it, irClass, true)
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
            compilerContext.symbolTable.withReferenceScope(initIrBody.descriptor) {
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
                            serialDescImplClass.referenceMethod(CallingConventions.addClassAnnotation)
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
            val property = classProp.irProp
            copySerialInfoAnnotationsToDescriptor(
                property.annotations,
                localDescriptor,
                serialDescImplClass.referenceMethod(CallingConventions.addAnnotation)
            )
        }
    }

    protected fun IrBlockBodyBuilder.copySerialInfoAnnotationsToDescriptor(
        annotations: List<IrConstructorCall>,
        receiver: IrVariable,
        method: IrFunctionSymbol
    ) {
        annotations.forEach { annotationCall ->
            if ((annotationCall.symbol.descriptor as? ClassConstructorDescriptor)?.constructedClass?.isSerialInfoAnnotation == true)
                +irInvoke(irGet(receiver), method, annotationCall.deepCopyWithVariables())
        }
    }

    override fun generateGenericFieldsAndConstructor(typedConstructorDescriptor: ClassConstructorDescriptor) =
        irClass.contributeConstructor(typedConstructorDescriptor) { ctor ->
            // generate call to primary ctor to init serialClassDesc and super()
            val primaryCtor = irClass.constructors.find { it.isPrimary }
                ?: throw AssertionError("Serializer class must have primary constructor")
            +IrDelegatingConstructorCallImpl(
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
                val localSerial = compilerContext.symbolTable.referenceField(localSerializersFieldsDescriptors[index])
                +irSetField(generateReceiverExpressionForFieldAccess(
                    thisAsReceiverParameter.symbol,
                    localSerializersFieldsDescriptors[index]
                ), localSerial.owner, irGet(param))
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
                compilerContext.symbolTable.referenceField(localSerializersFieldsDescriptors[idx]).owner
            )
        }
        val kSerType = ((irFun.returnType as IrSimpleType).arguments.first() as IrTypeProjection).type
        val array = createArrayOfExpression(kSerType, typeParams)
        +irReturn(array)
    }

    override fun generateSerializableClassProperty(property: PropertyDescriptor) {
        /* Already implemented in .generateSerialClassDesc ? */
    }

    protected inline fun ClassDescriptor.referenceMethod(methodName: String, predicate: (IrSimpleFunction) -> Boolean = { true }): IrFunctionSymbol {
        val irClass = compilerContext.referenceClass(fqNameSafe)?.owner ?: error("Couldn't load class $this")
        val simpleFunctions = irClass.declarations.filterIsInstance<IrSimpleFunction>()
        return simpleFunctions.filter { it.name.asString() == methodName }.single { predicate(it) }.symbol
    }

    override fun generateSave(function: FunctionDescriptor) = irClass.contributeFunction(function) { saveFunc ->

        val fieldInitializer: (SerializableProperty) -> IrExpression? =
            buildInitializersRemapping(serializableIrClass).run { { invoke(it.irField) } }

        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, saveFunc.dispatchReceiverParameter!!.symbol)

        val kOutputClass = serializerDescriptor.getClassFromSerializationPackage(STRUCTURE_ENCODER_CLASS)
        val encoderClass = serializerDescriptor.getClassFromSerializationPackage(ENCODER_CLASS)

        val descriptorGetterSymbol = irAnySerialDescProperty?.owner?.getter!!.symbol

        val localSerialDesc = irTemporary(irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol), "desc")

        //  fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): StructureEncoder
        val beginFunc = encoderClass.referenceMethod(CallingConventions.begin) { it.valueParameters.size == 1 }

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
                ), irProp
            )
        }

        // Ignore comparing to default values of properties from superclass,
        // because we do not have access to their fields (and initializers), if superclass is in another module.
        // In future, IR analogue of JVM's write$Self should be implemented.
        val superClass = irClass.getSuperClassOrAny().descriptor
        val ignoreIndexTo = if (superClass.isInternalSerializable) {
            bindingContext.serializablePropertiesFor(superClass).size
        } else {
            -1
        }


        //  internal serialization via virtual calls?
        for ((index, property) in serializableProperties.filter { !it.transient }.withIndex()) {
            // output.writeXxxElementValue(classDesc, index, value)
            val sti = getSerialTypeInfo(property)
            val innerSerial = serializerInstance(
                this@SerializerIrGenerator,
                saveFunc.dispatchReceiverParameter!!,
                sti.serializer,
                property.module,
                property.type,
                genericIndex = property.genericIndex
            )
            val (writeFunc, args: List<IrExpression>) = if (innerSerial == null) {
                val f =
                    kOutputClass.referenceMethod("${CallingConventions.encode}${sti.elementMethodPrefix}${CallingConventions.elementPostfix}")
                val args: MutableList<IrExpression> = mutableListOf(irGet(localSerialDesc), irInt(index))
                if (sti.elementMethodPrefix != "Unit") args.add(property.irGet())
                f to args
            } else {
                val f =
                    kOutputClass.referenceMethod("${CallingConventions.encode}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}")
                f to listOf(
                    irGet(localSerialDesc),
                    irInt(index),
                    innerSerial,
                    property.irGet()
                )
            }
            val typeArgs  = if (writeFunc.descriptor.typeParameters.isNotEmpty()) listOf(property.type.toIrType()) else listOf()
            val elementCall = irInvoke(irGet(localOutput), writeFunc, typeArguments = typeArgs, valueArguments = args)

            // check for call to .shouldEncodeElementDefault
            if (!property.optional || index < ignoreIndexTo) {
                // emit call right away
                +elementCall
            } else {
                // emit check:
                // if (obj.prop != DEFAULT_VALUE || output.shouldEncodeElementDefault(this.descriptor, i))
                //    output.encodeIntElement(this.descriptor, i, obj.prop)
                val shouldEncodeFunc = kOutputClass.referenceMethod(CallingConventions.shouldEncodeDefault)
                val partA = irNotEquals(property.irGet(), fieldInitializer(property)!!)
                val partB = irInvoke(irGet(localOutput), shouldEncodeFunc, irGet(localSerialDesc), irInt(index))
                // Ir infrastructure does not have dedicated symbol for ||, so
                //  `a || b == if (a) true else b`, see org.jetbrains.kotlin.ir.builders.PrimitivesKt.oror
                val condition = irIfThenElse(compilerContext.irBuiltIns.booleanType, partA, irTrue(), partB)
                +irIfThen(condition, elementCall)
            }
        }

        // output.writeEnd(serialClassDesc)
        val wEndFunc = kOutputClass.referenceMethod(CallingConventions.end)
        +irInvoke(irGet(localOutput), wEndFunc, irGet(localSerialDesc))
    }

    // returns null: Any? for boxed types and 0: <number type> for primitives
    private fun IrBuilderWithScope.defaultValueAndType(prop: SerializableProperty): Pair<IrExpression, KotlinType> {
        val kType = prop.descriptor.returnType!!
        val T = kType.toIrType()
        val defaultPrimitive: IrExpression? = when {
            T.isInt() -> IrConstImpl.int(startOffset, endOffset, T, 0)
            T.isBoolean() -> IrConstImpl.boolean(startOffset, endOffset, T, false)
            T.isLong() -> IrConstImpl.long(startOffset, endOffset, T, 0)
            T.isDouble() -> IrConstImpl.double(startOffset, endOffset, T, 0.0)
            T.isFloat() -> IrConstImpl.float(startOffset, endOffset, T, 0.0f)
            T.isChar() -> IrConstImpl.char(startOffset, endOffset, T, 0.toChar())
            T.isByte() -> IrConstImpl.byte(startOffset, endOffset, T, 0)
            T.isShort() -> IrConstImpl.short(startOffset, endOffset, T, 0)
            else -> null
        }
        return if (defaultPrimitive == null)
            irNull(compilerContext.irBuiltIns.anyNType) to (compilerContext.builtIns.nullableAnyType)
        else
            defaultPrimitive to kType
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
        val flagVar = irTemporaryVar(irBoolean(true), "flag")

        val indexVar = irTemporaryVar(irInt(0), "index")

        // calculating bit mask vars
        val blocksCnt = serializableProperties.bitMaskSlotCount()

        // var bitMask0 = 0, bitMask1 = 0...
        val bitMasks = (0 until blocksCnt).map { irTemporaryVar(irInt(0), "bitMask$it") }
        // var local0 = null, local1 = null ...
        val localProps = serializableProperties.mapIndexed { i, prop ->
            val (expr, type) = defaultValueAndType(prop)
            irTemporaryVar(
                expr,
                "local$i",
                typeHint = type
            )
        }

        //input = input.beginStructure(...)
        val beginFunc = decoderClass.referenceMethod(CallingConventions.begin) { it.valueParameters.size == 1 }
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
                    val sti = getSerialTypeInfo(property)
                    val innerSerial = serializerInstance(
                        this@SerializerIrGenerator,
                        loadFunc.dispatchReceiverParameter!!,
                        sti.serializer,
                        property.module,
                        property.type,
                        genericIndex = property.genericIndex
                    )
                    val isSerializable = innerSerial != null
                    // todo: update
                    val decodeFuncToCall =
                        (if (isSerializable) "${CallingConventions.decode}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}"
                        else "${CallingConventions.decode}${sti.elementMethodPrefix}${CallingConventions.elementPostfix}")
                            .let {
                                inputClass.referenceMethod(it) { it.valueParameters.size == if (isSerializable) 4 else 2 }
                            }
                    val typeArgs =
                        if (decodeFuncToCall.descriptor.typeParameters.isNotEmpty()) listOf(property.type.toIrType()) else listOf()
                    val args = mutableListOf<IrExpression>(localSerialDesc.get(), irInt(index))
                    if (innerSerial != null) {
                        args.add(innerSerial)
                        args.add(localProps[index].get())
                    }
                    // local$i = localInput.decode...(...)
                    +irSet(
                        localProps[index].symbol,
                        irInvoke(localInput.get(), decodeFuncToCall, typeArgs, args, returnTypeHint = property.type.toIrType())
                    )
                    // bitMask[i] |= 1 << x
                    val bitPos = 1 shl (index % 32)
                    val or = irBinOp(OperatorNameConventions.OR, bitMasks[index / 32].get(), irInt(bitPos))
                    +irSet(bitMasks[index / 32].symbol, or)
                }
                index to body
            }

        // if (decoder.decodeSequentially())
        val decodeSequentiallyCall = irInvoke(localInput.get(), inputClass.referenceMethod(CallingConventions.decodeSequentially))

        val sequentialPart = irBlock {
            decoderCalls.forEach { (_, expr) -> +expr.deepCopyWithVariables() }
        }

        val byIndexPart: IrExpression = irWhile().also { loop ->
            loop.condition = flagVar.get()
            loop.body = irBlock {
                val readElementF = inputClass.referenceMethod(CallingConventions.decodeElementIndex)
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
        val endFunc = inputClass.referenceMethod(CallingConventions.end)
        +irInvoke(
            localInput.get(),
            endFunc,
            irGet(localSerialDesc)
        )

        // todo: set properties in external deserialization
        var args: List<IrExpression> = localProps.map { it.get() }
        val typeArgs = (loadFunc.returnType as IrSimpleType).arguments.map { (it as IrTypeProjection).type }
        val ctor: IrConstructorSymbol = if (serializableDescriptor.isInternalSerializable) {
            args = bitMasks.map { irGet(it) } + args + irNull()
            serializableSyntheticConstructor(serializableIrClass)
        } else {
            compilerContext.referenceConstructors(serializableDescriptor.fqNameSafe).single { it.owner.isPrimary }
        }

        +irReturn(irInvoke(null, ctor, typeArgs, args))
    }

    companion object {
        fun generate(
            irClass: IrClass,
            context: SerializationPluginContext,
            bindingContext: BindingContext
        ) {
            val serializableDesc = getSerializableClassDescriptorBySerializer(irClass.symbol.descriptor) ?: return
            if (serializableDesc.isSerializableEnum()) {
                SerializerForEnumsGenerator(irClass, context, bindingContext).generate()
            } else {
                SerializerIrGenerator(irClass, context, bindingContext).generate()
            }
            irClass.patchDeclarationParents(irClass.parent)
        }
    }
}
