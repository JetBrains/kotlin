/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.jvm

import org.jetbrains.kotlin.codegen.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializerCodegen
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.typeArgPrefix
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

open class SerializerCodegenImpl(
    protected val codegen: ImplementationBodyCodegen,
    serializableClass: ClassDescriptor
) : SerializerCodegen(codegen.descriptor, codegen.bindingContext) {


    private val serialDescField = "\$\$serialDesc"

    protected val serializerAsmType = codegen.typeMapper.mapClass(codegen.descriptor)
    protected val serializableAsmType = codegen.typeMapper.mapClass(serializableClass)

    // if we have type parameters, descriptor initializing must be performed in constructor
    private val staticDescriptor = serializableDescriptor.declaredTypeParameters.isEmpty()

    companion object {
        fun generateSerializerExtensions(codegen: ImplementationBodyCodegen) {
            val serializableClass = getSerializableClassDescriptorBySerializer(codegen.descriptor) ?: return
            val serializerCodegen = if (serializableClass.isSerializableEnum()) {
                SerializerForEnumsCodegen(codegen, serializableClass)
            } else {
                SerializerCodegenImpl(codegen, serializableClass)
            }
            serializerCodegen.generate()
        }
    }

    override fun generateGenericFieldsAndConstructor(typedConstructorDescriptor: ClassConstructorDescriptor) {
        serializableDescriptor.declaredTypeParameters.forEachIndexed { i, _ ->
            codegen.v.newField(
                OtherOrigin(codegen.myClass.psiOrParent), ACC_PRIVATE or ACC_SYNTHETIC,
                "$typeArgPrefix$i", kSerializerType.descriptor, null, null
            )
        }

        var locals: Int = 0
        codegen.generateMethod(typedConstructorDescriptor) { _, exprGen ->
            load(0, serializerAsmType)
            invokespecial("java/lang/Object", "<init>", "()V", false)
            serializableDescriptor.declaredTypeParameters.forEachIndexed { i, _ ->
                load(0, serializerAsmType)
                load(++locals, kSerializerType)
                putfield(serializerAsmType.internalName, "$typeArgPrefix$i", kSerializerType.descriptor)
            }
            if (!staticDescriptor) exprGen.generateSerialDescriptor(++locals, false)
            areturn(Type.VOID_TYPE)
        }

    }

    private fun ExpressionCodegen.generateSerialDescriptor(descriptorVar: Int, isStatic: Boolean) = with(v) {
        instantiateNewDescriptor(isStatic)
        store(descriptorVar, descImplType)
        // add contents
        addElementsContentToDescriptor(descriptorVar)
        // add annotations on class itself
        addSyntheticAnnotationsToDescriptor(descriptorVar, serializableDescriptor, CallingConventions.addClassAnnotation)
        if (isStatic) {
            load(descriptorVar, descImplType)
            putstatic(serializerAsmType.internalName, serialDescField, descType.descriptor)
        } else {
            load(0, serializerAsmType)
            load(descriptorVar, descImplType)
            putfield(serializerAsmType.internalName, serialDescField, descType.descriptor)
        }
    }

    protected open fun ExpressionCodegen.instantiateNewDescriptor(isStatic: Boolean) = with(v) {
        anew(descImplType)
        dup()
        aconst(serialName)
        if (isStatic) {
            assert(serializerDescriptor.kind == ClassKind.OBJECT) { "Serializer for type without type parameters must be an object" }
            // static descriptor means serializer is an object. it is safer to get it from correct field
            if (isGeneratedSerializer)
                StackValue.singleton(serializerDescriptor, codegen.typeMapper).put(generatedSerializerType, this)
            else
                aconst(null)
        } else {
            load(0, serializerAsmType)
        }
        invokespecial(descImplType.internalName, "<init>", "(Ljava/lang/String;${generatedSerializerType.descriptor})V", false)
    }

    protected open fun ExpressionCodegen.addElementsContentToDescriptor(descriptorVar: Int) = with(v) {
        for (property in serializableProperties) {
            if (property.transient) continue
            load(descriptorVar, descImplType)
            aconst(property.name)
            iconst(if (property.optional) 1 else 0)
            invokevirtual(descImplType.internalName, CallingConventions.addElement, "(Ljava/lang/String;Z)V", false)
            // pushing annotations
            addSyntheticAnnotationsToDescriptor(descriptorVar, property.descriptor, CallingConventions.addAnnotation)
        }
    }

    protected fun ExpressionCodegen.addSyntheticAnnotationsToDescriptor(descriptorVar: Int, annotated: Annotated, functionToCall: String) =
        with(v) {
            for ((annotationClass, args, consParams) in annotated.annotationsWithArguments()) {
                if (args.size != consParams.size) throw IllegalArgumentException("Can't use arguments with defaults for serializable annotations yet")
                load(descriptorVar, descImplType)
                generateSyntheticAnnotationOnStack(annotationClass, args, consParams)
                invokevirtual(
                    descImplType.internalName,
                    functionToCall,
                    "(Ljava/lang/annotation/Annotation;)V",
                    false
                )
            }
        }

    override fun generateSerialDesc() {
        var flags = ACC_PRIVATE or ACC_FINAL or ACC_SYNTHETIC
        if (staticDescriptor) flags = flags or ACC_STATIC
        codegen.v.newField(
            OtherOrigin(codegen.myClass.psiOrParent), flags,
            serialDescField, descType.descriptor, null, null
        )
        // todo: lazy initialization of $$serialDesc ?
        if (!staticDescriptor) return
        val expr = codegen.createOrGetClInitCodegen()
        expr.generateSerialDescriptor(0, true)
    }

    private fun ExpressionCodegen.generateSyntheticAnnotationOnStack(
        annotationClass: ClassDescriptor,
        args: List<ValueArgument>,
        ctorParams: List<ValueParameterDescriptor>
    ) {
        val implType =
            codegen.typeMapper.mapType(annotationClass).internalName + "\$" + SerialEntityNames.IMPL_NAME.identifier
        with(v) {
            // new Annotation$Impl(...)
            anew(Type.getObjectType(implType))
            dup()
            val sb = StringBuilder("(")
            for (i in ctorParams.indices) {
                val decl = args[i]
                val desc = ctorParams[i]
                val valAsmType = codegen.typeMapper.mapType(desc.type)
                this@generateSyntheticAnnotationOnStack.gen(decl.getArgumentExpression(), valAsmType)
                sb.append(valAsmType.descriptor)
            }
            sb.append(")V")
            invokespecial(implType, "<init>", sb.toString(), false)
        }
    }

    // use null to put value on stack, use number to store it to var
    protected fun InstructionAdapter.stackSerialClassDesc(classDescVar: Int?) {
        if (staticDescriptor)
            getstatic(serializerAsmType.internalName, serialDescField, descType.descriptor)
        else {
            load(0, serializerAsmType)
            getfield(serializerAsmType.internalName, serialDescField, descType.descriptor)
        }
        classDescVar?.let { store(it, descType) }
    }

    override fun generateSerializableClassProperty(property: PropertyDescriptor) {
        codegen.generateMethod(property.getter!!) { _, _ ->
            stackSerialClassDesc(null)
            areturn(descType)
        }
    }

    override fun generateChildSerializersGetter(function: FunctionDescriptor) {
        codegen.generateMethod(function) { _, _ ->
            val size = serializableProperties.size
            iconst(size)
            newarray(kSerializerType)
            for (i in 0 until size) {
                dup() // array
                iconst(i) // index
                val prop = serializableProperties[i]
                assert(
                    stackValueSerializerInstanceFromSerializerWithoutSti(
                        codegen,
                        prop,
                        this@SerializerCodegenImpl
                    )
                ) { "Property ${prop.name} must have serializer" }
                astore(kSerializerType)
            }
            areturn(kSerializerArrayType)
        }
    }

    override fun generateSave(
        function: FunctionDescriptor
    ) {
        codegen.generateMethod(function) { signature, expressionCodegen ->
            // fun save(output: KOutput, obj : T)
            val outputVar = 1
            val objVar = 2
            val descVar = 3
            stackSerialClassDesc(descVar)
            val objType = signature.valueParameters[1].asmType
            // output = output.writeBegin(classDesc, new KSerializer[0])
            load(outputVar, encoderType)
            load(descVar, descType)
            genArrayOfTypeParametersSerializers()
            invokeinterface(
                encoderType.internalName, CallingConventions.begin,
                "(" + descType.descriptor + kSerializerArrayType.descriptor +
                        ")" + kOutputType.descriptor
            )
            store(outputVar, kOutputType)
            if (serializableDescriptor.isInternalSerializable) {
                val sig = StringBuilder("(${objType.descriptor}${kOutputType.descriptor}${descType.descriptor}")
                // call obj.write$Self(output, classDesc)
                load(objVar, objType)
                load(outputVar, kOutputType)
                load(descVar, descType)
                serializableDescriptor.declaredTypeParameters.forEachIndexed { i, _ ->
                    load(0, kSerializerType)
                    getfield(codegen.typeMapper.mapClass(codegen.descriptor).internalName, "$typeArgPrefix$i", kSerializerType.descriptor)
                    sig.append(kSerializerType.descriptor)
                }
                sig.append(")V")
                invokestatic(
                    objType.internalName, SerialEntityNames.WRITE_SELF_NAME.asString(),
                    sig.toString(), false
                )
            } else {
                // loop for all properties
                val labeledProperties = serializableProperties.filter { !it.transient }
                for (index in labeledProperties.indices) {
                    val property = labeledProperties[index]
                    if (property.transient) continue
                    // output.writeXxxElementValue(classDesc, index, value)
                    load(outputVar, kOutputType)
                    load(descVar, descType)
                    iconst(index)
                    genKOutputMethodCall(property, codegen, expressionCodegen, objType, objVar, generator = this@SerializerCodegenImpl)
                }
            }
            // output.writeEnd(classDesc)
            load(outputVar, kOutputType)
            load(descVar, descType)
            invokeinterface(
                kOutputType.internalName, CallingConventions.end,
                "(" + descType.descriptor + ")V"
            )
            // return
            areturn(Type.VOID_TYPE)
        }
    }

    internal fun InstructionAdapter.genArrayOfTypeParametersSerializers() {
        val size = serializableDescriptor.declaredTypeParameters.size
        iconst(size)
        newarray(kSerializerType) // todo: use some predefined empty array, if size is 0
        for (i in 0 until size) {
            dup() // array
            iconst(i) // index
            load(0, kSerializerType) // this.serialTypeI
            getfield(codegen.typeMapper.mapClass(codegen.descriptor).internalName, "$typeArgPrefix$i", kSerializerType.descriptor)
            astore(kSerializerType)
        }
    }

    override fun generateLoad(
        function: FunctionDescriptor
    ) {
        codegen.generateMethod(function) { _, expressionCodegen ->
            // fun load(input: KInput): T
            val inputVar = 1
            val descVar = 2
            val indexVar = 3
            val bitMaskBase = 4
            val blocksCnt = serializableProperties.bitMaskSlotCount()
            val bitMaskOff = fun(it: Int): Int { return bitMaskBase + bitMaskSlotAt(it) }
            val propsStartVar = bitMaskBase + blocksCnt
            stackSerialClassDesc(descVar)
            // initialize bit mask
            for (i in 0 until blocksCnt) {
                //int bitMaskN = 0
                iconst(0)
                store(bitMaskBase + i * OPT_MASK_TYPE.size, OPT_MASK_TYPE)
            }
            // initialize all prop vars
            var propVar = propsStartVar
            for (property in serializableProperties) {
                val propertyType = codegen.typeMapper.mapType(property.type)
                stackValueDefault(propertyType)
                store(propVar, propertyType)
                propVar += propertyType.size
            }
            // input = input.readBegin(classDesc, new KSerializer[0])
            load(inputVar, decoderType)
            load(descVar, descType)
            genArrayOfTypeParametersSerializers()
            invokeinterface(
                decoderType.internalName, CallingConventions.begin,
                "(" + descType.descriptor + kSerializerArrayType.descriptor +
                        ")" + kInputType.descriptor
            )
            store(inputVar, kInputType)
            val readElementLabel = Label()
            val readEndLabel = Label()
            // if (decoder.decodeSequentially)
            load(inputVar, kInputType)
            invokeinterface(
                kInputType.internalName, CallingConventions.decodeSequentially,
                "()Z"
            )
            ifeq(readElementLabel)
            // decodeSequentially = true
            propVar = propsStartVar
            for ((index, property) in serializableProperties.withIndex()) {
                val propertyType = codegen.typeMapper.mapType(property.type)
                callReadProperty(property, propertyType, index, inputVar, descVar, -1, propVar)
                propVar += propertyType.size
            }
            // set all bit masks to true
            for (maskVar in bitMaskBase until propsStartVar) {
                iconst(Int.MAX_VALUE)
                store(maskVar, OPT_MASK_TYPE)
            }
            // go to end
            goTo(readEndLabel)
            // branch with decodeSequentially = false
            // readElement: int index = input.readElement(classDesc)
            visitLabel(readElementLabel)
            load(inputVar, kInputType)
            load(descVar, descType)
            invokeinterface(
                kInputType.internalName, CallingConventions.decodeElementIndex,
                "(" + descType.descriptor + ")I"
            )
            store(indexVar, Type.INT_TYPE)
            // switch(index)
            val labeledProperties = serializableProperties.filter { !it.transient }
            val incorrectIndLabel = Label()
            val labels = arrayOfNulls<Label>(labeledProperties.size + 1)
            labels[0] = readEndLabel // READ_DONE
            for (i in labeledProperties.indices) {
                labels[i + 1] = Label()
            }
            load(indexVar, Type.INT_TYPE)
            tableswitch(-1, labeledProperties.size - 1, incorrectIndLabel, *labels)
            // loop for all properties
            propVar = propsStartVar
            var labelNum = 0
            for ((index, property) in serializableProperties.withIndex()) {
                val propertyType = codegen.typeMapper.mapType(property.type)
                if (!property.transient) {
                    val propertyAddressInBitMask = bitMaskOff(index)
                    // labelI:
                    visitLabel(labels[labelNum + 1])
                    callReadProperty(property, propertyType, index, inputVar, descVar, propertyAddressInBitMask, propVar)

                    // mark read bit in mask
                    // bitMask = bitMask | 1 << index
                    val addr = bitMaskOff(index)
                    load(addr, OPT_MASK_TYPE)
                    iconst(1 shl (index % OPT_MASK_BITS))
                    or(OPT_MASK_TYPE)
                    store(addr, OPT_MASK_TYPE)
                    goTo(readElementLabel)
                    labelNum++
                }
                // next
                propVar += propertyType.size
            }
            val resultVar = propVar
            // readEnd: input.readEnd(classDesc)
            visitLabel(readEndLabel)
            load(inputVar, kInputType)
            load(descVar, descType)
            invokeinterface(
                kInputType.internalName, CallingConventions.end,
                "(" + descType.descriptor + ")V"
            )
            if (!serializableDescriptor.isInternalSerializable) {
                //validate all required (constructor) fields
                for ((i, property) in properties.serializableConstructorProperties.withIndex()) {
                    if (property.optional || property.transient) {
                        if (!property.isConstructorParameterWithDefault)
                            throw CompilationException(
                                "Property ${property.name} was declared as optional/transient but has no default value",
                                null,
                                null
                            )
                    } else {
                        genValidateProperty(i, bitMaskOff(i))
                        val nonThrowLabel = Label()
                        ificmpne(nonThrowLabel)
                        genMissingFieldExceptionThrow(property.name)
                        visitLabel(nonThrowLabel)
                    }
                }
            }
            // create object with constructor
            anew(serializableAsmType)
            dup()
            val constructorDesc = if (serializableDescriptor.isInternalSerializable)
                buildInternalConstructorDesc(propsStartVar, bitMaskBase, codegen, properties.serializableProperties)
            else buildExternalConstructorDesc(propsStartVar, bitMaskBase)
            invokespecial(serializableAsmType.internalName, "<init>", constructorDesc, false)
            if (!serializableDescriptor.isInternalSerializable && !properties.serializableStandaloneProperties.isEmpty()) {
                // result := ... <created object>
                store(resultVar, serializableAsmType)
                // set other properties
                propVar = propsStartVar +
                        properties.serializableConstructorProperties.map { codegen.typeMapper.mapType(it.type).size }.sum()
                genSetSerializableStandaloneProperties(expressionCodegen, propVar, resultVar, bitMaskOff)
                // load result
                load(resultVar, serializableAsmType)
                // will return result
            }
            // return
            areturn(serializableAsmType)

            // throwing an exception in default branch (if no index matched)
            visitLabel(incorrectIndLabel)
            anew(Type.getObjectType(serializationExceptionUnknownIndexName))
            dup()
            load(indexVar, Type.INT_TYPE)
            invokespecial(serializationExceptionUnknownIndexName, "<init>", "(I)V", false)
            checkcast(Type.getObjectType("java/lang/Throwable"))
            athrow()
        }
    }

    private fun InstructionAdapter.callReadProperty(
        property: SerializableProperty,
        propertyType: Type,
        index: Int,
        inputVar: Int,
        descriptorVar: Int,
        propertyAddressInBitMask: Int,
        propertyVar: Int
    ) {
        // propX := input.readXxxValue(value)
        load(inputVar, kInputType)
        load(descriptorVar, descType)
        iconst(index)

        val sti = getSerialTypeInfo(property, propertyType)
        val useSerializer = stackValueSerializerInstanceFromSerializer(codegen, sti, this@SerializerCodegenImpl)
        val unknownSer = (!useSerializer && sti.elementMethodPrefix.isEmpty())
        if (unknownSer) {
            aconst(codegen.typeMapper.mapType(property.type))
            AsmUtil.wrapJavaClassIntoKClass(this)
        }

        fun produceCall(update: Boolean) {
            invokeinterface(
                kInputType.internalName,
                (if (update) CallingConventions.update else CallingConventions.decode) + sti.elementMethodPrefix + (if (useSerializer) "Serializable" else "") + CallingConventions.elementPostfix,
                "(" + descType.descriptor + "I" +
                        (if (useSerializer) kSerialLoaderType.descriptor else "")
                        + (if (unknownSer) AsmTypes.K_CLASS_TYPE.descriptor else "")
                        + (if (update) sti.type.descriptor else "")
                        + ")" + (if (sti.unit) "V" else sti.type.descriptor)
            )
        }

        if (useSerializer && propertyAddressInBitMask != -1) {
            // we can choose either it is read or update
            val readLabel = Label()
            val endL = Label()
            genValidateProperty(index, propertyAddressInBitMask)
            ificmpeq(readLabel)
            load(propertyVar, propertyType)
            StackValue.coerce(propertyType, sti.type, this)
            produceCall(true)
            goTo(endL)
            visitLabel(readLabel)
            produceCall(false)
            visitLabel(endL)
        } else {
            // update not supported for primitive types or decodeSequentially
            produceCall(false)
        }

        if (sti.unit) {
            StackValue.putUnitInstance(this)
        } else {
            StackValue.coerce(sti.type, propertyType, this)
        }
        store(propertyVar, propertyType)
    }

    private fun InstructionAdapter.buildExternalConstructorDesc(propsStartVar: Int, bitMaskBase: Int): String {
        val constructorDesc = StringBuilder("(")
        var propVar = propsStartVar
        for (property in properties.serializableConstructorProperties) {
            val propertyType = codegen.typeMapper.mapType(property.type)
            constructorDesc.append(propertyType.descriptor)
            load(propVar, propertyType)
            propVar += propertyType.size
        }
        if (!properties.primaryConstructorWithDefaults) {
            constructorDesc.append(")V")
        } else {
            val cnt = properties.serializableConstructorProperties.size.coerceAtMost(32) //only 32 default values are supported
            val mask = if (cnt == 32) -1 else ((1 shl cnt) - 1)
            load(bitMaskBase, OPT_MASK_TYPE)
            iconst(mask)
            xor(Type.INT_TYPE)
            aconst(null)
            constructorDesc.append("ILkotlin/jvm/internal/DefaultConstructorMarker;)V")
        }
        return constructorDesc.toString()
    }

    private fun InstructionAdapter.genSetSerializableStandaloneProperties(
        expressionCodegen: ExpressionCodegen, propVarStart: Int, resultVar: Int, bitMaskPos: (Int) -> Int
    ) {
        var propVar = propVarStart
        val offset = properties.serializableConstructorProperties.size
        for ((index, property) in properties.serializableStandaloneProperties.withIndex()) {
            val i = index + offset
            //check if property has been seen and should be set
            val nextLabel = Label()
            // seen = bitMask & 1 << pos != 0
            genValidateProperty(i, bitMaskPos(i))
            if (property.optional) {
                // if (seen)
                //    set
                ificmpeq(nextLabel)
            } else {
                // if (!seen)
                //    throw
                // set
                ificmpne(nextLabel)
                genMissingFieldExceptionThrow(property.name)
                visitLabel(nextLabel)
            }

            // generate setter call
            val propertyType = codegen.typeMapper.mapType(property.type)
            expressionCodegen.intermediateValueForProperty(
                property.descriptor, false, null,
                StackValue.local(resultVar, serializableAsmType)
            ).store(StackValue.local(propVar, propertyType), this)
            propVar += propertyType.size
            if (property.optional)
                visitLabel(nextLabel)
        }
    }
}
