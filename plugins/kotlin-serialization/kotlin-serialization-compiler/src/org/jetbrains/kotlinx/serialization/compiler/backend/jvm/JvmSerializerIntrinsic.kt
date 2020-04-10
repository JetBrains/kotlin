/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.jvm

import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.MethodNode

object JvmSerializerIntrinsic {
    fun applyFunction(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): StackValue? {
        val targetFunction = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return null
        val isSerializerIntr = targetFunction.fqNameSafe.asString() == "kotlinx.serialization.serializerIntr"
        if (!isSerializerIntr) return null
        val typeArgument =
            resolvedCall.typeArguments.entries.singleOrNull()?.value ?: error("serializer() function has exactly one type parameter")
        return StackValue.functionCall(kSerializerType, targetFunction.returnType) { iv ->
            generateSerializerForType(typeArgument, iv, c.typeMapper, c.codegen.typeSystem)
        }
    }

    fun applyPluginDefinedReifiedOperationMarker(
        insn: MethodInsnNode,
        instructions: InsnList,
        type: KotlinType,
        asmType: Type,
        typeMapper: KotlinTypeMapper,
        typeSystem: TypeSystemCommonBackendContext
    ): Boolean {
        val newMethodNode = MethodNode(Opcodes.API_VERSION)
        generateSerializerForType(type, InstructionAdapter(newMethodNode), typeMapper, typeSystem, asmType)

        instructions.remove(insn.next)
        instructions.insert(insn, newMethodNode.instructions)

        return true
    }

    private fun generateSerializerForType(
        type: KotlinType,
        adapter: InstructionAdapter,
        typeMapper: KotlinTypeMapper,
        typeSystem: TypeSystemCommonBackendContext,
        asmType: Type = typeMapper.mapType(type)
    ): Boolean = with(adapter) {
        val typeDescriptor = (type as SimpleType).constructor.declarationDescriptor!!
        if (typeDescriptor is TypeParameterDescriptor) { // need further reification
            ReifiedTypeInliner.putReifiedOperationMarkerIfNeeded(
                typeDescriptor,
                false,
                ReifiedTypeInliner.OperationKind.PLUGIN_DEFINED,
                this,
                typeSystem
            )
            invokestatic("kotlinx/serialization/SerializerResolvingKt", "serializerIntr", "()Lkotlinx/serialization/KSerializer;", false)
            return false
        }
        val companionType =
            (typeDescriptor as ClassDescriptor).companionObjectDescriptor ?: error("no companion")
        val asmCompanionType = typeMapper.mapType(companionType)
        val companionName = companionType.name.asString()
        getstatic(asmType.internalName, companionName, asmCompanionType.descriptor)
        val args = type.arguments.map { it.type }
        args.forEach { generateSerializerForType(it, this, typeMapper, typeSystem, typeMapper.mapType(it)) }
        val signature = kSerializerType.descriptor.repeat(args.size)
        invokevirtual(asmCompanionType.internalName, "serializer", "(${signature})${kSerializerType.descriptor}", false)
        return true
    }
}