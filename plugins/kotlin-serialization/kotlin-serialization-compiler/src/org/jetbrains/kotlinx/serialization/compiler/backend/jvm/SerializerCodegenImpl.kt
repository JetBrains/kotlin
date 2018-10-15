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

class SerializerCodegenImpl(
        private val codegen: ImplementationBodyCodegen,
        serializableClass: ClassDescriptor
) : SerializerCodegen(codegen.descriptor, codegen.bindingContext) {


    private val serialDescField = "\$\$serialDesc"

    private val serializerAsmType = codegen.typeMapper.mapClass(codegen.descriptor)
    private val serializableAsmType = codegen.typeMapper.mapClass(serializableClass)

    companion object {
        fun generateSerializerExtensions(codegen: ImplementationBodyCodegen) {
            val serializableClass = getSerializableClassDescriptorBySerializer(codegen.descriptor) ?: return
            SerializerCodegenImpl(codegen, serializableClass).generate()
        }
    }

    override fun generateGenericFieldsAndConstructor(typedConstructorDescriptor: ConstructorDescriptor) {
        serializableDescriptor.declaredTypeParameters.forEachIndexed { i, _ ->
            codegen.v.newField(
                OtherOrigin(codegen.myClass.psiOrParent), ACC_PRIVATE or ACC_SYNTHETIC,
                "$typeArgPrefix$i", kSerializerType.descriptor, null, null
            )
        }

        codegen.generateMethod(typedConstructorDescriptor) { _, _ ->
            load(0, serializerAsmType)
            invokespecial("java/lang/Object", "<init>", "()V", false)
            serializableDescriptor.declaredTypeParameters.forEachIndexed { i, _ ->
                load(0, serializerAsmType)
                load(i+1, kSerializerType)
                putfield(serializerAsmType.internalName, "$typeArgPrefix$i", kSerializerType.descriptor)
            }
            areturn(Type.VOID_TYPE)
        }

    }

    override fun generateSerialDesc() {
        codegen.v.newField(OtherOrigin(codegen.myClass.psiOrParent), ACC_PRIVATE or ACC_STATIC or ACC_FINAL or ACC_SYNTHETIC,
                           serialDescField, descType.descriptor, null, null)
        // todo: lazy initialization of $$serialDesc that is performed only when save/load is invoked first time
        val expr = codegen.createOrGetClInitCodegen()
        with(expr.v) {
            val classDescVar = 0
            anew(descImplType)
            dup()
            aconst(serialName)
            invokespecial(descImplType.internalName, "<init>", "(Ljava/lang/String;)V", false)
            store(classDescVar, descImplType)
            for (property in orderedProperties) {
                if (property.transient) continue
                load(classDescVar, descImplType)
                aconst(property.name)
                invokevirtual(descImplType.internalName, CallingConventions.addElement, "(Ljava/lang/String;)V", false)
                // pushing annotations
                for ((annotationClass, args, consParams) in property.annotationsWithArguments) {
                    if (args.size != consParams.size) throw IllegalArgumentException("Can't use arguments with defaults for serializable annotations yet")
                    load(classDescVar, descImplType)
                    expr.generateSyntheticAnnotationOnStack(annotationClass, args, consParams)
                    invokevirtual(
                        descImplType.internalName,
                        CallingConventions.addAnnotation,
                        "(Ljava/lang/annotation/Annotation;)V",
                        false
                    )
                }
            }
            // add annotations on class itself
            for ((annotationClass, args, consParams) in serializableDescriptor.annotationsWithArguments()) {
                if (args.size != consParams.size) throw IllegalArgumentException("Can't use arguments with defaults for serializable annotations yet")
                load(classDescVar, descImplType)
                expr.generateSyntheticAnnotationOnStack(annotationClass, args, consParams)
                invokevirtual(
                    descImplType.internalName,
                    CallingConventions.addClassAnnotation,
                    "(Ljava/lang/annotation/Annotation;)V",
                    false
                )
            }
            load(classDescVar, descImplType)
            putstatic(serializerAsmType.internalName, serialDescField, descType.descriptor)
        }
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

    private fun InstructionAdapter.serialCLassDescToLocalVar(classDescVar: Int) {
        getstatic(serializerAsmType.internalName, serialDescField, descType.descriptor)
        store(classDescVar, descType)
    }

    override fun generateSerializableClassProperty(property: PropertyDescriptor) {
        codegen.generateMethod(property.getter!!) { _, _ ->
            getstatic(serializerAsmType.internalName, serialDescField, descType.descriptor)
            areturn(descType)
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
            serialCLassDescToLocalVar(descVar)
            val objType = signature.valueParameters[1].asmType
            // output = output.writeBegin(classDesc, new KSerializer[0])
            load(outputVar, encoderType)
            load(descVar, descType)
            genArrayOfTypeParametersSerializers()
            invokeinterface(
                encoderType.internalName, CallingConventions.begin,
                          "(" + descType.descriptor + kSerializerArrayType.descriptor +
                          ")" + kOutputType.descriptor)
            store(outputVar, kOutputType)
            if (serializableDescriptor.isInternalSerializable) {
                val sig = StringBuilder("(${kOutputType.descriptor}${descType.descriptor}")
                // call obj.write$Self(output, classDesc)
                load(objVar, objType)
                load(outputVar, kOutputType)
                load(descVar, descType)
                serializableDescriptor.declaredTypeParameters.forEachIndexed {i, _ ->
                    load(0, kSerializerType)
                    getfield(codegen.typeMapper.mapClass(codegen.descriptor).internalName, "$typeArgPrefix$i", kSerializerType.descriptor)
                    sig.append(kSerializerType.descriptor)
                }
                sig.append(")V")
                invokevirtual(
                    objType.internalName, SerialEntityNames.WRITE_SELF_NAME.asString(),
                              sig.toString(), false)
            }
            else {
                // loop for all properties
                val labeledProperties = orderedProperties.filter { !it.transient }
                for (index in labeledProperties.indices) {
                    val property = labeledProperties[index]
                    if (property.transient) continue
                    // output.writeXxxElementValue(classDesc, index, value)
                    load(outputVar, kOutputType)
                    load(descVar, descType)
                    iconst(index)
                    genKOutputMethodCall(property, codegen, expressionCodegen, objType, objVar)
                }
            }
            // output.writeEnd(classDesc)
            load(outputVar, kOutputType)
            load(descVar, descType)
            invokeinterface(
                kOutputType.internalName, CallingConventions.end,
                          "(" + descType.descriptor + ")V")
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
            val readAllVar = 4
            val bitMaskBase = 5
            val blocksCnt = orderedProperties.size / OPT_MASK_BITS + 1
            fun bitMaskOff(i: Int) = bitMaskBase + (i / OPT_MASK_BITS) * OPT_MASK_TYPE.size
            val propsStartVar = bitMaskBase + OPT_MASK_TYPE.size * blocksCnt
            serialCLassDescToLocalVar(descVar)
            // boolean readAll = false
            iconst(0)
            store(readAllVar, Type.BOOLEAN_TYPE)
            // initialize bit mask
            for (i in 0 until blocksCnt) {
                //int bitMaskN = 0
                iconst(0)
                store(bitMaskBase + i * OPT_MASK_TYPE.size, OPT_MASK_TYPE)
            }
            // initialize all prop vars
            var propVar = propsStartVar
            for (property in orderedProperties) {
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
                          ")" + kInputType.descriptor)
            store(inputVar, kInputType)
            // readElement: int index = input.readElement(classDesc)
            val readElementLabel = Label()
            visitLabel(readElementLabel)
            load(inputVar, kInputType)
            load(descVar, descType)
            invokeinterface(
                kInputType.internalName, CallingConventions.decodeElementIndex,
                          "(" + descType.descriptor + ")I")
            store(indexVar, Type.INT_TYPE)
            // switch(index)
            val labeledProperties = orderedProperties.filter { !it.transient }
            val readAllLabel = Label()
            val readEndLabel = Label()
            val incorrectIndLabel = Label()
            val labels = arrayOfNulls<Label>(labeledProperties.size + 2)
            labels[0] = readAllLabel // READ_ALL
            labels[1] = readEndLabel // READ_DONE
            for (i in labeledProperties.indices) {
                labels[i + 2] = Label()
            }
            load(indexVar, Type.INT_TYPE)
            tableswitch(-2, labeledProperties.size - 1, incorrectIndLabel, *labels)
            // readAll: readAll := true
            visitLabel(readAllLabel)
            iconst(1)
            store(readAllVar, Type.BOOLEAN_TYPE)
            // loop for all properties
            propVar = propsStartVar
            var labelNum = 0
            for ((index, property) in orderedProperties.withIndex()) {
                val propertyType = codegen.typeMapper.mapType(property.type)
                if (!property.transient) {
                    // labelI:
                    visitLabel(labels[labelNum + 2])
                    // propX := input.readXxxValue(value)
                    load(inputVar, kInputType)
                    load(descVar, descType)
                    iconst(labelNum)

                    val sti = getSerialTypeInfo(property, propertyType)
                    val useSerializer = stackValueSerializerInstanceFromSerializer(codegen, sti)
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

                    if (useSerializer) {
                        // we can choose either it is read or update
                        val readLabel = Label()
                        val endL = Label()
                        genValidateProperty(index, ::bitMaskOff)
                        ificmpeq(readLabel)
                        load(propVar, propertyType)
                        produceCall(true)
                        goTo(endL)
                        visitLabel(readLabel)
                        produceCall(false)
                        visitLabel(endL)
                    } else {
                        // update not supported for primitive types
                        produceCall(false)
                    }

                    if (sti.unit) {
                        StackValue.putUnitInstance(this)
                    }
                    else {
                        StackValue.coerce(sti.type, propertyType, this)
                    }
                    store(propVar, propertyType)

                    // mark read bit in mask
                    // bitMask = bitMask | 1 << index
                    val addr = bitMaskOff(index)
                    load(addr, OPT_MASK_TYPE)
                    iconst(1 shl (index % OPT_MASK_BITS))
                    or(OPT_MASK_TYPE)
                    store(addr, OPT_MASK_TYPE)
                    // if (readAll == false) goto readElement
                    load(readAllVar, Type.BOOLEAN_TYPE)
                    iconst(0)
                    ificmpeq(readElementLabel)
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
            invokeinterface(kInputType.internalName, CallingConventions.end,
                          "(" + descType.descriptor + ")V")
            if (!serializableDescriptor.isInternalSerializable) {
                //validate all required (constructor) fields
                val nonThrowLabel = Label()
                val throwLabel = Label()
                for ((i, property) in properties.serializableConstructorProperties.withIndex()) {
                    if (property.optional || property.transient) {
                        // todo: Normal reporting of error
                        if (!property.isConstructorParameterWithDefault)
                            throw CompilationException("Property ${property.name} was declared as optional/transient but has no default value", null, null)
                    }
                    else {
                        genValidateProperty(i, ::bitMaskOff)
                        // todo: print name of each variable?
                        ificmpeq(throwLabel)
                    }
                }
                goTo(nonThrowLabel)
                // throwing an exception
                visitLabel(throwLabel)
                genExceptionThrow(serializationExceptionName, "Not all required constructor fields were specified")
                visitLabel(nonThrowLabel)
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
                propVar = propsStartVar + properties.serializableConstructorProperties.map { codegen.typeMapper.mapType(it.type).size }.sum()
                genSetSerializableStandaloneProperties(expressionCodegen, propVar, resultVar, ::bitMaskOff)
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
        }
        else {
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
            expressionCodegen: ExpressionCodegen, propVarStart: Int, resultVar: Int, bitMaskPos: (Int) -> Int) {
        var propVar = propVarStart
        val offset = properties.serializableConstructorProperties.size
        for ((index, property) in properties.serializableStandaloneProperties.withIndex()) {
            val i = index + offset
            //check if property has been seen and should be set
            val nextLabel = Label()
            // seen = bitMask & 1 << pos != 0
            genValidateProperty(i, bitMaskPos)
            if (property.optional) {
                // if (seen)
                //    set
                ificmpeq(nextLabel)
            }
            else {
                // if (!seen)
                //    throw
                // set
                ificmpne(nextLabel)
                genExceptionThrow(serializationExceptionMissingFieldName, property.name)
                visitLabel(nextLabel)
            }

            // generate setter call
            val propertyType = codegen.typeMapper.mapType(property.type)
            expressionCodegen.intermediateValueForProperty(property.descriptor, false, null,
                                                           StackValue.local(resultVar, serializableAsmType)).
                    store(StackValue.local(propVar, propertyType), this)
            propVar += propertyType.size
            if (property.optional)
                visitLabel(nextLabel)
        }
    }

    // todo: move to StackValue?
    private fun InstructionAdapter.stackValueDefault(type: Type) {
        when (type.sort) {
            BOOLEAN, BYTE, SHORT, CHAR, INT -> iconst(0)
            LONG -> lconst(0)
            FLOAT -> fconst(0f)
            DOUBLE -> dconst(0.0)
            else -> aconst(null)
        }
    }


}
