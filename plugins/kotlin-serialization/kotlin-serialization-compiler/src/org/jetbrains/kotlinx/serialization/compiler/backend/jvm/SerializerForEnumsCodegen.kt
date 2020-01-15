package org.jetbrains.kotlinx.serialization.compiler.backend.jvm

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlinx.serialization.compiler.resolve.CallingConventions
import org.jetbrains.kotlinx.serialization.compiler.resolve.enumEntries
import org.jetbrains.kotlinx.serialization.compiler.resolve.serialNameValue
import org.jetbrains.org.objectweb.asm.Type

class SerializerForEnumsCodegen(
    codegen: ImplementationBodyCodegen,
    serializableClass: ClassDescriptor
) : SerializerCodegenImpl(codegen, serializableClass) {
    override fun generateSave(function: FunctionDescriptor) = codegen.generateMethod(function) { _, _ ->
        // fun save(output: KOutput, obj : T)
        val outputVar = 1
        val objVar = 2
        // output.encodeEnum(descriptor, ordinal)
        load(outputVar, encoderType)
        stackSerialClassDesc(null)
        load(objVar, serializableAsmType)
        invokevirtual(serializableAsmType.internalName, "ordinal", "()I", false)
        invokeinterface(encoderType.internalName, CallingConventions.encodeEnum, "(${descType.descriptor}I)V")
        // return
        areturn(Type.VOID_TYPE)
    }

    override fun generateLoad(function: FunctionDescriptor) = codegen.generateMethod(function) { _, _ ->
        // fun load(input: KInput): T
        val inputVar = 1
        val serializableArrayType = Type.getType("[L${serializableAsmType.internalName};")
        // T.values()
        invokestatic(serializableAsmType.internalName, "values", "()${serializableArrayType.descriptor}", false)
        // input.decodeEnum(descriptor)
        load(inputVar, decoderType)
        stackSerialClassDesc(null)
        invokeinterface(decoderType.internalName, CallingConventions.decodeEnum, "(${descType.descriptor})I")
        // return
        aload(serializableAsmType)
        areturn(serializableAsmType)
    }

    override fun ExpressionCodegen.instantiateNewDescriptor(isStatic: Boolean) = with(v) {
        anew(descriptorForEnumsType)
        dup()
        aconst(serialName)
        invokespecial(descriptorForEnumsType.internalName, "<init>", "(Ljava/lang/String;)V", false)
        checkcast(descImplType)
    }

    override fun ExpressionCodegen.addElementsContentToDescriptor(descriptorVar: Int) = with(v) {
        val enumEntries = serializableDescriptor.enumEntries()
        for (entry in enumEntries) {
            load(descriptorVar, descImplType)
            // regular .serialName() produces fqName here, which is kinda inconvenient for enum entry
            val serialName = entry.annotations.serialNameValue ?: entry.name.toString()
            aconst(serialName)
            iconst(0)
            invokevirtual(descImplType.internalName, CallingConventions.addElement, "(Ljava/lang/String;Z)V", false)
            // pushing annotations
            addSyntheticAnnotationsToDescriptor(descriptorVar, entry, CallingConventions.addAnnotation)
        }
    }
}