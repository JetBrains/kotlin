/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.common.lower.irThrow
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializerCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.getSerialTypeInfo
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.DECODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.ENCODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.KSERIALIZER_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.SERIAL_DESCRIPTOR_CLASS_IMPL
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.STRUCTURE_DECODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.STRUCTURE_ENCODER_CLASS
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.UNKNOWN_FIELD_EXC

// Is creating synthetic origin is a good idea or not?
object SERIALIZABLE_PLUGIN_ORIGIN : IrDeclarationOriginImpl("SERIALIZER")

class SerializerIrGenerator(val irClass: IrClass, override val compilerContext: BackendContext, bindingContext: BindingContext) :
    SerializerCodegen(irClass.descriptor, bindingContext), IrBuilderExtension {

    override val translator: TypeTranslator = compilerContext.createTypeTranslator(serializableDescriptor.module)
    private val _table = SymbolTable()
    override val BackendContext.localSymbolTable: SymbolTable
        get() = _table

    private val serializableIrClass = compilerContext.externalSymbols.referenceClass(serializableDescriptor).owner

    override fun generateSerialDesc() {
        val desc: PropertyDescriptor = generatedSerialDescPropertyDescriptor ?: return
        val serialDescImplClass = serializerDescriptor
            .getClassFromInternalSerializationPackage(SERIAL_DESCRIPTOR_CLASS_IMPL)
        val serialDescImplConstructor = serialDescImplClass
            .unsubstitutedPrimaryConstructor!!

        val addFuncS = serialDescImplClass.referenceMethod(CallingConventions.addElement)

        val thisAsReceiverParameter = irClass.thisReceiver!!
        lateinit var prop: IrProperty

        // how to (auto)create backing field and getter/setter?
        compilerContext.localSymbolTable.withScope(irClass.descriptor) {

            introduceValueParameter(thisAsReceiverParameter)
            prop = generateSimplePropertyWithBackingField(thisAsReceiverParameter.symbol, desc, irClass)
            irClass.addMember(prop)

            localSerializersFieldsDescriptors.forEach {
                irClass.addMember(generateSimplePropertyWithBackingField(thisAsReceiverParameter.symbol, it, irClass))
            }
        }

        compilerContext.localSymbolTable.declareAnonymousInitializer(
            irClass.startOffset, irClass.endOffset, SERIALIZABLE_PLUGIN_ORIGIN, irClass.descriptor
        ).buildWithScope { initIrBody ->
            val ctor = irClass.declarations.filterIsInstance<IrConstructor>().find { it.isPrimary }
                ?: throw AssertionError("Serializer must have primary constructor")
            val serialClassDescImplCtor = compilerContext.externalSymbols.referenceConstructor(serialDescImplConstructor)
            compilerContext.localSymbolTable.withScope(initIrBody.descriptor) {
                initIrBody.body = compilerContext.createIrBuilder(initIrBody.symbol).irBlockBody {
                    val localDesc = irTemporary(
                        irInvoke(
                            null, serialClassDescImplCtor,
                            irString(serialName), if (isGeneratedSerializer) irGet(thisAsReceiverParameter) else irNull(),
                            typeHint = serialClassDescImplCtor.owner.returnType
                        ),
                        nameHint = "serialDesc"
                    )

                    fun addFieldCall(fieldName: String) =
                        irCall(addFuncS, type = compilerContext.irBuiltIns.unitType).apply {
                            dispatchReceiver = irGet(localDesc)
                        }.mapValueParameters { irString(fieldName) }

                    for (classProp in orderedProperties) {
                        if (classProp.transient) continue
                        +addFieldCall(classProp.name)
                        // add property annotations
                        copySerialInfoAnnotationsToDescriptor(
                            classProp.irField.correspondingProperty?.annotations.orEmpty(),
                            irGet(localDesc),
                            serialDescImplClass.referenceMethod(CallingConventions.addAnnotation)
                        )
                    }
                    // add class annotations
                    copySerialInfoAnnotationsToDescriptor(
                        serializableIrClass.annotations,
                        irGet(localDesc),
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
                (ctor.body as? IrBlockBody)?.statements?.addAll(initIrBody.body.statements)
            }
        }
    }

    private fun IrBlockBodyBuilder.copySerialInfoAnnotationsToDescriptor(
        annotations: List<IrCall>,
        receiver: IrExpression,
        method: IrFunctionSymbol
    ) {
        annotations.forEach { annotationCall ->
            if ((annotationCall.descriptor as? ClassConstructorDescriptor)?.constructedClass?.isSerialInfoAnnotation == true)
                +irInvoke(receiver, method, annotationCall)
        }
    }

    override fun generateGenericFieldsAndConstructor(typedConstructorDescriptor: ClassConstructorDescriptor) =
        irClass.contributeCtor(typedConstructorDescriptor) { ctor ->
            // generate call to primary ctor to init serialClassDesc and super()
            val primaryCtor = irClass.descriptor.unsubstitutedPrimaryConstructor
                ?: throw AssertionError("Serializer class must have primary constructor")
            +IrDelegatingConstructorCallImpl(
                startOffset,
                endOffset,
                compilerContext.irBuiltIns.unitType,
                compilerContext.localSymbolTable.referenceConstructor(primaryCtor),
                primaryCtor
            )

            // store type arguments serializers in fields
            val thisAsReceiverParameter = irClass.thisReceiver!!
            ctor.valueParameters.forEachIndexed { index, param ->
                val localSerial = compilerContext.localSymbolTable.referenceField(localSerializersFieldsDescriptors[index])
                +irSetField(generateReceiverExpressionForFieldAccess(
                    thisAsReceiverParameter.symbol,
                    localSerializersFieldsDescriptors[index]
                ), localSerial.owner, irGet(param))
            }
        }

    override fun generateChildSerializersGetter(function: FunctionDescriptor) = irClass.contributeFunction(function) { irFun ->
        val allSerializers = orderedProperties.map { requireNotNull(
                serializerTower(this@SerializerIrGenerator, it)) { "Property ${it.name} must have a serializer" }
        }

        val kSer = serializableDescriptor.module.getClassFromSerializationPackage(KSERIALIZER_NAME.identifier)
        val kSerType = compilerContext.externalSymbols.referenceClass(kSer).owner.defaultType
        val array = createArrayOfExpression(kSerType, allSerializers)
        +irReturn(array)
    }

    override fun generateSerializableClassProperty(property: PropertyDescriptor) {
        /* Already implemented in .generateSerialClassDesc ? */
    }

    fun ClassDescriptor.referenceMethod(methodName: String) =
        getFuncDesc(methodName).single().let { compilerContext.externalSymbols.referenceFunction(it) }

    override fun generateSave(function: FunctionDescriptor) = irClass.contributeFunction(function) { saveFunc ->

        val fieldInitializer: (SerializableProperty) -> IrExpression? =
            buildInitializersRemapping(serializableIrClass).run { { invoke(it.irField) } }

        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, saveFunc.dispatchReceiverParameter!!.symbol)

        val kOutputClass = serializerDescriptor.getClassFromSerializationPackage(STRUCTURE_ENCODER_CLASS)
        val kOutputSmallClass = serializerDescriptor.getClassFromSerializationPackage(ENCODER_CLASS)

        val descriptorGetterSymbol = compilerContext.externalSymbols.referenceFunction(anySerialDescProperty?.getter!!) //???

        val localSerialDesc = irTemporary(irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol), "desc")

        //  fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): StructureEncoder
        val beginFunc = kOutputSmallClass.referenceMethod(CallingConventions.begin) // todo: retrieve from actual encoder instead

        val call = irCall(beginFunc).mapValueParametersIndexed { i, parameterDescriptor ->
            if (i == 0) irGet(localSerialDesc) else IrVarargImpl(
                startOffset,
                endOffset,
                parameterDescriptor.type.toIrType(),
                parameterDescriptor.varargElementType!!.toIrType()
            )
        }
        // can it be done in more concise way? e.g. additional builder function?
        call.dispatchReceiver = irGet(saveFunc.valueParameters[0])
        val serialObjectSymbol = saveFunc.valueParameters[1]
        val localOutput = irTemporary(call, "output")

        fun SerializableProperty.irGet(): IrGetField = irGetField(irGet(serialObjectSymbol), irField)

        //  internal serialization via virtual calls?
        for ((index, property) in orderedProperties.filter { !it.transient }.withIndex()) {
            // output.writeXxxElementValue(classDesc, index, value)
            val sti = getSerialTypeInfo(property)
            val innerSerial = serializerInstance(
                this@SerializerIrGenerator,
                serializableDescriptor,
                sti.serializer,
                property.module,
                property.type,
                property.genericIndex
            )
            val (writeFunc, args: List<IrExpression>) = if (innerSerial == null) {
                val f =
                    kOutputClass.referenceMethod("${CallingConventions.encode}${sti.elementMethodPrefix}${CallingConventions.elementPostfix}")
                val args = mutableListOf(irGet(localSerialDesc), irInt(index))
                if (sti.elementMethodPrefix != "Unit") args += property.irGet()
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
            val elementCall = irInvoke(irGet(localOutput), writeFunc, *args.toTypedArray())

            // check for call to .shouldEncodeElementDefault
            if (!property.optional) {
                // emit call right away
                +elementCall
            } else {
                // emit check:
                // if (obj.prop != DEFAULT_VALUE || output.shouldEncodeElementDefault(this.descriptor, i))
                //    output.encodeIntElement(this.descriptor, i, obj.prop)
                val shouldEncodeFunc = kOutputClass.referenceMethod(CallingConventions.shouldEncodeDefault)
                val partA = irNotEquals(property.irGet(), fieldInitializer(property)!!) // todo: props w/o backing fields
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
            irNull() to (compilerContext.builtIns.nullableAnyType)
        else
            defaultPrimitive to kType
    }

    override fun generateLoad(function: FunctionDescriptor) = irClass.contributeFunction(function) { loadFunc ->
        fun irThis(): IrExpression =
            IrGetValueImpl(startOffset, endOffset, loadFunc.dispatchReceiverParameter!!.symbol)

        fun IrVariable.get() = irGet(this)

        val inputClass = serializerDescriptor.getClassFromSerializationPackage(STRUCTURE_DECODER_CLASS)
        val inputSmallClass = serializerDescriptor.getClassFromSerializationPackage(DECODER_CLASS)
        val descriptorGetterSymbol = compilerContext.externalSymbols.referenceFunction(anySerialDescProperty?.getter!!) //???
        val localSerialDesc = irTemporary(irGet(descriptorGetterSymbol.owner.returnType, irThis(), descriptorGetterSymbol), "desc")

        // workaround due to unavailability of labels (KT-25386)
        val flagVar = irTemporaryVar(irBoolean(true), "flag")

        val indexVar = irTemporaryVar(irInt(0), "index")
//        val readAll = irTemporaryVar(irBoolean(false), "readAll", parent = loadFunc)
//
        // calculating bit mask vars
        val blocksCnt = orderedProperties.size / 32 + 1

        // var bitMask0 = 0, bitMask1 = 0...
        val bitMasks = (0 until blocksCnt).map { irTemporaryVar(irInt(0), "bitMask$it") }
        // var local0 = null, local1 = null ...
        val localProps = orderedProperties.mapIndexed { i, prop ->
            val (expr, type) = defaultValueAndType(prop)
            irTemporaryVar(
                expr,
                "local$i",
                typeHint = type
            )
        }

        //input = input.beginStructure(...)
        val beginFunc = inputSmallClass.referenceMethod(CallingConventions.begin)
        val call = irInvoke(
            irGet(loadFunc.valueParameters[0]),
            beginFunc,
            irGet(localSerialDesc),
            irEmptyVararg(beginFunc.descriptor.valueParameters[1])
        )
        val localInput = irTemporary(call, "input")

        +irWhile().also { loop ->
            loop.condition = flagVar.get()
            loop.body = irBlock {
                val readElementF = inputClass.referenceMethod(CallingConventions.decodeElementIndex)
                +irSetVar(indexVar.symbol, irInvoke(localInput.get(), readElementF, localSerialDesc.get()))
                +irWhen {
                    // if index == -2 (READ_ALL) todo...
                    +IrBranchImpl(irEquals(indexVar.get(), irInt(-2)), irThrowNpe())

                    // if index == -1 (READ_DONE) break loop
                    +IrBranchImpl(irEquals(indexVar.get(), irInt(-1)), irSetVar(flagVar.symbol, irBoolean(false)))

                    val branchBodies: List<Pair<Int, IrExpression>> = orderedProperties.mapIndexed { index, property ->
                        val body = irBlock {
                            val sti = getSerialTypeInfo(property)
                            val innerSerial = serializerInstance(this@SerializerIrGenerator, serializableDescriptor, sti.serializer, property.module, property.type, property.genericIndex)
                            // todo: update
                            val decodeFuncToCall =
                                (if (innerSerial != null) "${CallingConventions.decode}${sti.elementMethodPrefix}Serializable${CallingConventions.elementPostfix}"
                                else "${CallingConventions.decode}${sti.elementMethodPrefix}${CallingConventions.elementPostfix}")
                                    .let {
                                        inputClass.referenceMethod(it)
                                    }
                            val args = mutableListOf<IrExpression>(localSerialDesc.get(), irInt(index))
                            if (innerSerial != null)
                                args.add(innerSerial)
                            // local$i = localInput.decode...(...)
                            +irSetVar(localProps[index].symbol, irInvoke(localInput.get(), decodeFuncToCall, *args.toTypedArray()))
                            // bitMask[i] |= 1 << x
                            val bitPos = 1 shl (index % 32)
                            val or = irBinOp(OperatorNameConventions.OR, bitMasks[index / 32].get(), irInt(bitPos))
                            +irSetVar(bitMasks[index / 32].symbol, or)
                        }
                        index to body
                    }
                    branchBodies.forEach { (i, e) -> +IrBranchImpl(irEquals(indexVar.get(), irInt(i)), e) }

                    // throw exception on unknown field
                    val exceptionCtor =
                        serializableDescriptor.getClassFromSerializationPackage(UNKNOWN_FIELD_EXC)
                            .unsubstitutedPrimaryConstructor!!
                    val excClassRef = compilerContext.externalSymbols.referenceConstructor(exceptionCtor)
                    +elseBranch(
                        irThrow(
                            irInvoke(
                                null,
                                excClassRef,
                                indexVar.get(),
                                typeHint = excClassRef.owner.returnType
                            )
                        )
                    )
                }
            }
        }

        //input.endStructure(...)
        val endFunc = inputClass.referenceMethod(CallingConventions.end)
        +irInvoke(
            localInput.get(),
            endFunc,
            irGet(localSerialDesc)
        )

        // todo: set properties in external deserialization
        var args: List<IrExpression> = localProps.map { it.get() }
        val ctor: IrConstructorSymbol = if (serializableDescriptor.isInternalSerializable) {
            val ctorDesc = serializableIrClass
                .constructors.single { it.origin == SERIALIZABLE_PLUGIN_ORIGIN }
            args = listOf(irGet(bitMasks[0])) + args + irNull()
            ctorDesc.symbol
        } else {
            compilerContext.externalSymbols.referenceConstructor(serializableDescriptor.unsubstitutedPrimaryConstructor!!)
        }

        +irReturn(irInvoke(null, ctor, *args.toTypedArray()))
    }

    companion object {
        fun generate(
            irClass: IrClass,
            context: BackendContext,
            bindingContext: BindingContext
        ) {
            if (getSerializableClassDescriptorBySerializer(irClass.descriptor) != null)
                SerializerIrGenerator(irClass, context, bindingContext).generate()
        }
    }
}