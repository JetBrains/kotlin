/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.jvm

import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.codegen.extensions.ExpressionCodegenExtension
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.inline.newMethodNodeWithCorrectStackSize
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.expressions.typeParametersCount
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.TypeSystemCommonBackendContext
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlinx.serialization.compiler.backend.common.AbstractSerialGenerator
import org.jetbrains.kotlinx.serialization.compiler.backend.common.SerializableCompanionCodegen
import org.jetbrains.kotlinx.serialization.compiler.backend.common.findTypeSerializerOrContextUnchecked
import org.jetbrains.kotlinx.serialization.compiler.resolve.isSerializableObject
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode

object JvmSerializerIntrinsic {
    fun applyFunction(resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): StackValue? {
        val targetFunction = resolvedCall.resultingDescriptor as? FunctionDescriptor ?: return null
        if (!isSerializerReifiedFunction(targetFunction)) return null
        val typeArgument =
            resolvedCall.typeArguments.entries.singleOrNull()?.value ?: error("serializer() function has exactly one type parameter")
        return StackValue.functionCall(kSerializerType, targetFunction.returnType) { iv ->
            generateSerializerForType(typeArgument, iv, c.typeMapper, c.codegen.typeSystem, module = c.codegen.state.module)
        }
    }

    fun isSerializerReifiedFunction(targetFunction: FunctionDescriptor): Boolean =
        targetFunction.fqNameSafe.asString() == "kotlinx.serialization.serializer"
                && targetFunction.valueParameters.isEmpty()
                && targetFunction.typeParametersCount == 1
                && targetFunction.dispatchReceiverParameter == null
                && targetFunction.extensionReceiverParameter == null

    fun applyPluginDefinedReifiedOperationMarker(
        insn: MethodInsnNode,
        instructions: InsnList,
        type: KotlinType,
        typeMapper: KotlinTypeMapper,
        typeSystem: TypeSystemCommonBackendContext,
        module: ModuleDescriptor
    ): Int {
        val newMethodNode = newMethodNodeWithCorrectStackSize {
            generateSerializerForType(type, it, typeMapper, typeSystem, module)
        }

        instructions.remove(insn.next)
        instructions.insert(insn, newMethodNode.instructions)

        return newMethodNode.maxStack
    }

    private fun InstructionAdapter.putReifyMarkerIfNeeded(type: KotlinTypeMarker, typeSystem: TypeSystemCommonBackendContext): Boolean =
        with(typeSystem) {
            val typeDescriptor = type.typeConstructor().getTypeParameterClassifier()
            if (typeDescriptor != null) { // need further reification
                ReifiedTypeInliner.putReifiedOperationMarkerIfNeeded(
                    typeDescriptor,
                    false,
                    ReifiedTypeInliner.OperationKind.PLUGIN_DEFINED,
                    this@putReifyMarkerIfNeeded,
                    typeSystem
                )
                invokestatic("kotlinx/serialization/SerializersKt", "serializer", "()Lkotlinx/serialization/KSerializer;", false)
                return true
            }
            return false
        }

    private fun KotlinTypeMarker.toClassDescriptorOrFail(typeSystem: TypeSystemCommonBackendContext): ClassDescriptor = with(typeSystem) {
        val typeDescriptor: ClassDescriptor = when (val c = this@toClassDescriptorOrFail.typeConstructor()) {
            is IrClassSymbol -> c.descriptor
            is TypeConstructor -> c.declarationDescriptor!!
            else -> error("Don't know how to extract type descriptor for $c")
        } as ClassDescriptor
        return typeDescriptor
    }

    internal fun generateSerializerForType(
        type: KotlinType,
        adapter: InstructionAdapter,
        typeMapper: KotlinTypeMapper,
        typeSystem: TypeSystemCommonBackendContext,
        module: ModuleDescriptor,
    ): Unit = with(adapter) {
        if (putReifyMarkerIfNeeded(type, typeSystem)) return
        val typeDescriptor: ClassDescriptor = type.toClassDescriptorOrFail(typeSystem)

        // TODO: This works only with ClassDescriptor/KotlinType, so we convert all the types
        // This may cause problems with KotlinTypeMapper
        val serializerMethod = SerializableCompanionCodegen.findSerializerGetterOnCompanion(typeDescriptor)
        if (serializerMethod != null) {
            // fast path
            val companionType = if (typeDescriptor.isSerializableObject) typeDescriptor else typeDescriptor.companionObjectDescriptor!!
            StackValue.singleton(companionType, typeMapper).put(this)
            val args = type.arguments.map { it.type }
            args.forEach { generateSerializerForType(it, this, typeMapper, typeSystem, module) }
            val signature = kSerializerType.descriptor.repeat(args.size)
            invokevirtual(
                typeMapper.mapType(companionType).internalName,
                "serializer",
                "(${signature})${kSerializerType.descriptor}",
                false
            )
        } else {
            // More general path, including special ol built-in serializers for e.g. List
            val emptyGenerator: AbstractSerialGenerator? = null // stub indicating we do not look into @UseSerializers to avoid confusion
            val serializer = emptyGenerator.findTypeSerializerOrContextUnchecked(module, type)
            emptyGenerator.stackValueSerializerInstance(
                typeMapper,
                module,
                type,
                serializer,
                this,
                insertExceptionOnNoSerializer = true
            ) { _, genericArg ->
                assert(putReifyMarkerIfNeeded(genericArg, typeSystem))
            }
            if (type.isMarkedNullable) wrapStackValueIntoNullableSerializer()
        }
    }
}