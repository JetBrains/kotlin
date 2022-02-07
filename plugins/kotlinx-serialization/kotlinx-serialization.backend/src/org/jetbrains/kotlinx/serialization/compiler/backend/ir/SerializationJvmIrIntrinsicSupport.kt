/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.BlockInfo
import org.jetbrains.kotlin.backend.jvm.codegen.ExpressionCodegen
import org.jetbrains.kotlin.backend.jvm.codegen.MaterialValue
import org.jetbrains.kotlin.backend.jvm.codegen.PromisedValue
import org.jetbrains.kotlin.backend.jvm.intrinsics.IntrinsicMethod
import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.backend.jvm.mapping.mapClass
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.inline.newMethodNodeWithCorrectStackSize
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.AsmTypes
import org.jetbrains.kotlin.types.model.KotlinTypeMarker
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.*
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.annotationArrayType
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.doubleAnnotationArrayType
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.enumFactoriesType
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.kSerializerArrayType
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.kSerializerType
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.stringArrayType
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.stringType
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.ENUM_SERIALIZER_FACTORY_FUNC_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.MARKED_ENUM_SERIALIZER_FACTORY_FUNC_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.contextSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.objectSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.polymorphicSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.referenceArraySerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.sealedSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SpecialBuiltins
import org.jetbrains.kotlinx.serialization.compiler.resolve.getClassFromSerializationPackage
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode

class SerializationJvmIrIntrinsicSupport(val jvmBackendContext: JvmBackendContext) : SerializationBaseContext {
    companion object {
        fun isSerializerReifiedFunction(targetFunction: IrFunction): Boolean =
            targetFunction.fqNameWhenAvailable?.asString() == "kotlinx.serialization.SerializersKt.serializer"
                    && targetFunction.valueParameters.isEmpty()
                    && targetFunction.typeParameters.size == 1
                    && targetFunction.dispatchReceiverParameter == null
                    && targetFunction.extensionReceiverParameter == null
    }

    object ReifiedSerializerMethod : IntrinsicMethod() {
        override fun invoke(
            expression: IrFunctionAccessExpression,
            codegen: ExpressionCodegen,
            data: BlockInfo
        ): PromisedValue {
            with(codegen) {
                val argument = expression.getTypeArgument(0)!!
                SerializationJvmIrIntrinsicSupport(codegen.context).generateSerializerForType(
                    argument,
                    mv
                )
                return MaterialValue(codegen, kSerializerType, expression.type)
            }
        }
    }


    private val emptyGenerator: BaseIrGenerator? = null
    private val module = jvmBackendContext.state.module
    private val typeSystemContext = jvmBackendContext.typeSystem
    private val typeMapper = jvmBackendContext.defaultTypeMapper

    /**
     * This likely won't work in FIR because module is empty there and can't reference dependencies
     * Proper referencing can be done via FirPluginContext, but it's not available in the intrinsics.
     */
    @FirIncompatiblePluginAPI
    override fun referenceClassId(classId: ClassId): IrClassSymbol? {
        return module.findClassAcrossModuleDependencies(classId)?.let { jvmBackendContext.referenceClass(it) }
    }

    override val runtimeHasEnumSerializerFactoryFunctions: Boolean
        get() = false // TODO

    private fun findTypeSerializerOrContext(argType: IrType): IrClassSymbol? =
        emptyGenerator.findTypeSerializerOrContextUnchecked(this, argType)

    private fun instantiateObject(iv: InstructionAdapter, objectSymbol: IrClassSymbol) {
        val originalIrClass = objectSymbol.owner
        require(originalIrClass.isObject)
        val targetField = jvmBackendContext.cachedDeclarations.getFieldForObjectInstance(originalIrClass)
        val ownerType = typeMapper.mapClass(targetField.parentAsClass)
        val fieldType = typeMapper.mapType(targetField.type)
        iv.visitFieldInsn(Opcodes.GETSTATIC, ownerType.internalName, targetField.name.asString(), fieldType.descriptor)
    }

    fun applyPluginDefinedReifiedOperationMarker(
        insn: MethodInsnNode,
        instructions: InsnList,
        type: IrType,
    ): Int {
        val newMethodNode = newMethodNodeWithCorrectStackSize {
            generateSerializerForType(type, it)
        }

        instructions.remove(insn.next)
        instructions.insert(insn, newMethodNode.instructions)

        return newMethodNode.maxStack
    }

    private fun InstructionAdapter.putReifyMarkerIfNeeded(type: KotlinTypeMarker): Boolean =
        with(typeSystemContext) {
            val typeDescriptor = type.typeConstructor().getTypeParameterClassifier()
            if (typeDescriptor != null) { // need further reification
                ReifiedTypeInliner.putReifiedOperationMarkerIfNeeded(
                    typeDescriptor,
                    false,
                    ReifiedTypeInliner.OperationKind.PLUGIN_DEFINED,
                    this@putReifyMarkerIfNeeded,
                    typeSystemContext
                )
                invokestatic("kotlinx/serialization/SerializersKt", "serializer", "()Lkotlinx/serialization/KSerializer;", false)
                return true
            }
            return false
        }

    fun generateSerializerForType(
        type: IrType,
        adapter: InstructionAdapter
    ) {
        with(typeSystemContext) {
            if (adapter.putReifyMarkerIfNeeded(type)) return
            val typeDescriptor: IrClass = type.classOrNull!!.owner

            val support = this@SerializationJvmIrIntrinsicSupport

            val serializerMethod = SerializableCompanionIrGenerator.getSerializerGetterFunction(typeDescriptor)
            if (serializerMethod != null) {
                // fast path
                val companionType = if (typeDescriptor.isSerializableObject) typeDescriptor else typeDescriptor.companionObject()!!
                support.instantiateObject(adapter, companionType.symbol)
                val args = type.getArguments().map { it.getType() }
                args.forEach { generateSerializerForType(it, adapter) }
                val signature = kSerializerType.descriptor.repeat(args.size)
                adapter.invokevirtual(
                    typeMapper.mapClass(companionType).internalName,
                    "serializer",
                    "(${signature})${kSerializerType.descriptor}",
                    false
                )
            } else {
                // More general path, including special ol built-in serializers for e.g. List
                val serializer = support.findTypeSerializerOrContext(type)
                support.stackValueSerializerInstance(
                    type,
                    serializer,
                    adapter
                ) { genericArg ->
                    assert(putReifyMarkerIfNeeded(genericArg))
                }
                if (type.isMarkedNullable()) adapter.wrapStackValueIntoNullableSerializer()
            }
        }
    }

    private fun stackValueSerializerInstance(
        kType: IrType, maybeSerializer: IrClassSymbol?,
        iv: InstructionAdapter?,
        genericIndex: Int? = null,
        genericSerializerFieldGetter: (InstructionAdapter.(IrType) -> Unit)? = null,
    ): Boolean = with(typeSystemContext) {
        if (maybeSerializer == null && genericIndex != null) {
            // get field from serializer object
            iv?.run { genericSerializerFieldGetter?.invoke(this, kType) }
            return true
        }
        val serializer = maybeSerializer ?: run {
            iv?.apply {
                aconst(kType.classFqName!!.asString())
                invokestatic(
                    "kotlinx/serialization/SerializersKt",
                    "noCompiledSerializer",
                    "(Ljava/lang/String;)Lkotlinx/serialization/KSerializer;",
                    false
                )
            }
            return false
        }
        if (serializer.owner.isObject) {
            // singleton serializer -- just get it
            iv?.let { instantiateObject(it, serializer) }
            return true
        }
        // serializer is not singleton object and shall be instantiated
        val argSerializers = (kType as IrSimpleType).arguments.map { projection ->
            // check if any type argument is not serializable
            val argType = projection.typeOrNull!!
            val argSerializer =
                if (argType.isTypeParameter()) null else findTypeSerializerOrContext(argType)
            // check if it can be properly serialized with its args recursively
            Pair(argType, argSerializer)
        }
        // new serializer if needed
        iv?.apply {
            val serializerType = typeMapper.mapClass(serializer.owner)
            if (serializer.owner.classId == enumSerializerId && runtimeHasEnumSerializerFactoryFunctions) {
                val enumIrClass = kType.classOrNull!!.owner
                // runtime contains enum serializer factory functions
                val javaEnumArray = Type.getType("[Ljava/lang/Enum;")
                val enumJavaType = typeMapper.mapType(kType, TypeMappingMode.GENERIC_ARGUMENT)
                val serialName = enumIrClass.serialName()

                if (enumIrClass.isEnumWithSerialInfoAnnotation()) {
                    aconst(serialName)
                    invokestatic(enumJavaType.internalName, "values", "()[${enumJavaType.descriptor}", false)
                    checkcast(javaEnumArray)

                    val entries = enumIrClass.enumEntries()
                    fillArray(stringType, entries) { _, entry ->
                        entry.annotations.serialNameValue.let {
                            if (it == null) {
                                aconst(null)
                            } else {
                                aconst(it)
                            }
                        }
                    }
                    checkcast(stringArrayType)

                    fillArray(annotationArrayType, entries) { _, _ ->
                        // FIXME: no org.jetbrains.kotlin.codegen.ExpressionCodegen available here to generate instances from descriptors
                        //  val annotations = entry.descriptor.annotationsWithArguments()
                        aconst(null)
                    }
                    checkcast(doubleAnnotationArrayType)

                    invokestatic(
                        enumFactoriesType.internalName,
                        MARKED_ENUM_SERIALIZER_FACTORY_FUNC_NAME.asString(),
                        "(${stringType.descriptor}${javaEnumArray.descriptor}${stringArrayType.descriptor}${doubleAnnotationArrayType.descriptor})${kSerializerType.descriptor}",
                        false
                    )
                } else {
                    aconst(serialName)
                    invokestatic(enumJavaType.internalName, "values", "()[${enumJavaType.descriptor}", false)
                    checkcast(javaEnumArray)

                    invokestatic(
                        enumFactoriesType.internalName,
                        ENUM_SERIALIZER_FACTORY_FUNC_NAME.asString(),
                        "(${stringType.descriptor}${javaEnumArray.descriptor})${kSerializerType.descriptor}",
                        false
                    )
                }
                return true
            }

            anew(serializerType)
            dup()
            // instantiate all arg serializers on stack
            val signature = StringBuilder("(")

            fun instantiate(typeArgument: Pair<IrType, IrClassSymbol?>, writeSignature: Boolean = true) {
                val (argType, argSerializer) = typeArgument
                stackValueSerializerInstance(
                    argType,
                    argSerializer,
                    this,
                    argType.genericIndex,
                    genericSerializerFieldGetter
                )
                // wrap into nullable serializer if argType is nullable
                if (argType.isMarkedNullable()) wrapStackValueIntoNullableSerializer()
                if (writeSignature) signature.append(kSerializerType.descriptor)
            }

            val serialName = kType.serialName()
            when (serializer.owner.classId) {
                enumSerializerId -> {
                    // support legacy serializer instantiation by constructor for old runtimes
                    aconst(serialName)
                    signature.append("Ljava/lang/String;")
                    val enumJavaType = typeMapper.mapTypeCommon(kType, TypeMappingMode.GENERIC_ARGUMENT)
                    val javaEnumArray = Type.getType("[Ljava/lang/Enum;")
                    invokestatic(enumJavaType.internalName, "values", "()[${enumJavaType.descriptor}", false)
                    checkcast(javaEnumArray)
                    signature.append(javaEnumArray.descriptor)
                }
                contextSerializerId, polymorphicSerializerId -> {
                    // a special way to instantiate enum -- need a enum KClass reference
                    // GENERIC_ARGUMENT forces boxing in order to obtain KClass
                    aconst(typeMapper.mapTypeCommon(kType, TypeMappingMode.GENERIC_ARGUMENT))
                    AsmUtil.wrapJavaClassIntoKClass(this)
                    signature.append(AsmTypes.K_CLASS_TYPE.descriptor)
                    if (serializer.owner.classId == contextSerializerId && serializer.constructors.any { it.owner.valueParameters.size == 3 }) { // TODO: this isn't working with LAZY IR CLASS
                        // append new additional arguments
                        val fallbackDefaultSerializer = findTypeSerializer(this@SerializationJvmIrIntrinsicSupport, kType)
                        if (fallbackDefaultSerializer != null && fallbackDefaultSerializer != serializer) {
                            instantiate(kType to fallbackDefaultSerializer, writeSignature = false)
                        } else {
                            aconst(null)
                        }
                        signature.append(kSerializerType.descriptor)
                        fillArray(kSerializerType, argSerializers) { _, serializer ->
                            instantiate(serializer, writeSignature = false)
                        }
                        signature.append(kSerializerArrayType.descriptor)
                    }
                }
                referenceArraySerializerId -> {
                    // a special way to instantiate reference array serializer -- need an element KClass reference
                    aconst(typeMapper.mapTypeCommon(kType.getArguments().first().getType(), TypeMappingMode.GENERIC_ARGUMENT))
                    AsmUtil.wrapJavaClassIntoKClass(this)
                    signature.append(AsmTypes.K_CLASS_TYPE.descriptor)
                    // Reference array serializer still needs serializer for its argument type
                    instantiate(argSerializers[0])
                }
                sealedSerializerId -> {
                    aconst(serialName)
                    signature.append("Ljava/lang/String;")
                    aconst(typeMapper.mapTypeCommon(kType, TypeMappingMode.GENERIC_ARGUMENT))
                    AsmUtil.wrapJavaClassIntoKClass(this)
                    signature.append(AsmTypes.K_CLASS_TYPE.descriptor)
                    val (subClasses, subSerializers) = emptyGenerator.allSealedSerializableSubclassesFor(
                        kType.classOrUpperBound()?.owner!!,
                        this@SerializationJvmIrIntrinsicSupport
                    )
                    // KClasses vararg
                    fillArray(AsmTypes.K_CLASS_TYPE, subClasses) { _, type ->
                        aconst(typeMapper.mapTypeCommon(type, TypeMappingMode.GENERIC_ARGUMENT))
                        AsmUtil.wrapJavaClassIntoKClass(this)
                    }
                    signature.append(AsmTypes.K_CLASS_ARRAY_TYPE.descriptor)
                    // Serializers vararg
                    fillArray(kSerializerType, subSerializers) { i, serializer ->
                        val (argType, argSerializer) = subClasses[i] to serializer
                        assert(
                            stackValueSerializerInstance(
                                argType,
                                argSerializer,
                                this,
                                argType.genericIndex
                            ) { genericType ->
                                // if we encountered generic type parameter in one of subclasses of sealed class, use polymorphism from upper bound
                                assert(
                                    stackValueSerializerInstance(
                                        (genericType.classifierOrNull as IrTypeParameterSymbol).owner.representativeUpperBound,
                                        jvmBackendContext.referenceClass(module.getClassFromSerializationPackage(SpecialBuiltins.polymorphicSerializer)),
                                        this
                                    )
                                )
                            }
                        )
                        if (argType.isMarkedNullable()) wrapStackValueIntoNullableSerializer()
                    }
                    signature.append(kSerializerArrayType.descriptor)
                }
                objectSerializerId -> {
                    aconst(serialName)
                    signature.append("Ljava/lang/String;")
                    instantiateObject(iv, kType.classOrNull!!)
                    signature.append("Ljava/lang/Object;")
                }
                // all serializers get arguments with serializers of their generic types
                else -> argSerializers.forEach { instantiate(it) }
            }
            signature.append(")V")
            // invoke constructor
            invokespecial(serializerType.internalName, "<init>", signature.toString(), false)
        }
        return true
    }
}