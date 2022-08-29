/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.extensions.FirIncompatiblePluginAPI
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.intrinsics.IntrinsicMethod
import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.backend.jvm.mapping.mapClass
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.inline.newMethodNodeWithCorrectStackSize
import org.jetbrains.kotlin.config.ApiVersion
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
import org.jetbrains.kotlinx.serialization.compiler.diagnostic.VersionReader
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
import org.jetbrains.org.objectweb.asm.tree.AbstractInsnNode
import org.jetbrains.org.objectweb.asm.tree.InsnList
import org.jetbrains.org.objectweb.asm.tree.MethodInsnNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode

class SerializationJvmIrIntrinsicSupport(val jvmBackendContext: JvmBackendContext) : SerializationBaseContext {
    sealed class IntrinsicType(val methodDescriptor: String) {
        object Simple : IntrinsicType(stubCallDescriptor)

        class WithModule(val storedIndex: Int) :
            IntrinsicType(stubCallDescriptorWithModule)
    }

    companion object {
        fun intrinsicForMethod(method: IrFunction): IntrinsicMethod? {
            if (method.fqNameWhenAvailable?.asString() != "kotlinx.serialization.SerializersKt.serializer"
                || method.dispatchReceiverParameter != null
                || method.typeParameters.size != 1
                || method.valueParameters.isNotEmpty()
            ) return null
            val receiver = method.extensionReceiverParameter
            return if (receiver == null)
                ReifiedSerializerMethod(withModule = false)
            else if (receiver.type.classFqName?.asString() == "kotlinx.serialization.modules.SerializersModule")
                ReifiedSerializerMethod(withModule = true)
            else null
        }

        val serializersModuleType: Type = Type.getObjectType("kotlinx/serialization/modules/SerializersModule")

        val stubCallDescriptorWithModule = "(${serializersModuleType.descriptor})${kSerializerType.descriptor}"
        val stubCallDescriptor = "()${kSerializerType.descriptor}"
        const val serializersKtInternalName = "kotlinx/serialization/SerializersKt"
        const val callMethodName = "serializer"
        const val noCompiledSerializerMethodName = "noCompiledSerializer"

    }

    class ReifiedSerializerMethod(private val withModule: Boolean) : IntrinsicMethod() {
        override fun invoke(
            expression: IrFunctionAccessExpression,
            codegen: ExpressionCodegen,
            data: BlockInfo
        ): PromisedValue {
            with(codegen) {
                val argument = expression.getTypeArgument(0)!!
                val intrinsicType = if (withModule) {
                    val moduleReceiver = expression.extensionReceiver!!
                    val materialVal = moduleReceiver.accept(codegen, data).materializedAt(moduleReceiver.type)
                    val storedIndex = frameMap.enterTemp(materialVal.type)
                    mv.store(storedIndex, materialVal.type)
                    IntrinsicType.WithModule(storedIndex)
                } else {
                    codegen.markLineNumber(expression)
                    IntrinsicType.Simple
                }
                SerializationJvmIrIntrinsicSupport(codegen.context).generateSerializerForType(
                    argument,
                    mv,
                    intrinsicType
                )
                if (withModule) {
                    frameMap.leaveTemp(serializersModuleType)
                }
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

    private val currentVersion = VersionReader.getVersionsForCurrentModuleFromTrace(module, jvmBackendContext.state.bindingTrace)
        ?.implementationVersion

    override val runtimeHasEnumSerializerFactoryFunctions: Boolean
        get() = currentVersion != null && currentVersion > ApiVersion.parse("1.4.0")!!

    private val hasNewContextSerializerSignature: Boolean
        get() = currentVersion != null && currentVersion >= ApiVersion.parse("1.2.0")!!

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
        val intrinsicType = getOperationTypeFromInsn(insn) ?: return -1
        val newMethodNode = newMethodNodeWithCorrectStackSize {
            generateSerializerForType(type, it, intrinsicType)
        }

        when (intrinsicType) {
            is IntrinsicType.Simple -> instructions.remove(insn.next)
            is IntrinsicType.WithModule -> {
                instructions.remove(insn.next.next)
                instructions.remove(insn.next)
            }
        }
        instructions.insert(insn, newMethodNode.instructions)

        return newMethodNode.maxStack
    }

    private fun getOperationTypeFromInsn(insn: MethodInsnNode): IntrinsicType? {
        // insn is reification marker
        // insn.next is serializer() OR load(module)
        // insn.next.next is serializer(module) if insn.next was load(module)
        val mayBeSerializerModuleCall: AbstractInsnNode? = insn.next?.next
        val next = insn.next ?: error("Reification marker cannot be the last instruction in method")
        if (
            mayBeSerializerModuleCall is MethodInsnNode
            && mayBeSerializerModuleCall.opcode == Opcodes.INVOKESTATIC
            && mayBeSerializerModuleCall.owner == serializersKtInternalName
            && mayBeSerializerModuleCall.name == callMethodName
            && mayBeSerializerModuleCall.desc == stubCallDescriptorWithModule
        ) {
            val loadIns = next as? VarInsnNode ?: error("Expected load(SerializersModule) instruction")
            // It's possible to also check opcode, but that doesn't seem necessary
            return IntrinsicType.WithModule(loadIns.`var`)
        } else if (next is MethodInsnNode
            && next.opcode == Opcodes.INVOKESTATIC
            && next.owner == serializersKtInternalName
            && next.name == callMethodName
            && next.desc == stubCallDescriptor
        ) {
            return IntrinsicType.Simple
        } else return null // May be reification marker from other plugin
    }

    private fun InstructionAdapter.putReifyMarkerIfNeeded(type: KotlinTypeMarker, intrinsicType: IntrinsicType): Boolean =
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
                if (intrinsicType is IntrinsicType.WithModule) {
                    load(intrinsicType.storedIndex, serializersModuleType)
                }
                invokestatic(serializersKtInternalName, callMethodName, intrinsicType.methodDescriptor, false)
                return true
            }
            return false
        }

    fun generateSerializerForType(
        type: IrType,
        adapter: InstructionAdapter,
        intrinsicType: IntrinsicType
    ) {
        with(typeSystemContext) {
            if (adapter.putReifyMarkerIfNeeded(type, intrinsicType)) return
            val typeDescriptor: IrClass = type.classOrNull!!.owner

            val support = this@SerializationJvmIrIntrinsicSupport

            val serializerMethod = SerializableCompanionIrGenerator.getSerializerGetterFunction(typeDescriptor)
            if (serializerMethod != null) {
                // fast path
                val companionType = if (typeDescriptor.isSerializableObject) typeDescriptor else typeDescriptor.companionObject()!!
                support.instantiateObject(adapter, companionType.symbol)
                val args = type.getArguments().map { it.getType() }
                args.forEach { generateSerializerForType(it, adapter, intrinsicType) }
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
                    adapter,
                    intrinsicType
                ) { genericArg ->
                    assert(putReifyMarkerIfNeeded(genericArg, intrinsicType))
                }
            }
            if (type.isMarkedNullable()) adapter.wrapStackValueIntoNullableSerializer()
        }
    }

    private fun stackValueSerializerInstance(
        kType: IrType, maybeSerializer: IrClassSymbol?,
        iv: InstructionAdapter,
        intrinsicType: IntrinsicType,
        genericIndex: Int? = null,
        genericSerializerFieldGetter: (InstructionAdapter.(IrType) -> Unit)? = null,
    ): Boolean = with(typeSystemContext) {
        if (maybeSerializer == null && genericIndex != null) {
            // get field from serializer object
            genericSerializerFieldGetter?.invoke(iv, kType)
            return true
        }
        if (maybeSerializer == null && intrinsicType == IntrinsicType.Simple) {
            iv.apply {
                aconst(kType.classFqName!!.asString())
                invokestatic(
                    serializersKtInternalName,
                    noCompiledSerializerMethodName,
                    "(Ljava/lang/String;)${kSerializerType.descriptor}",
                    false
                )
            }
            return false
        }
        if (maybeSerializer != null && maybeSerializer.owner.isObject) {
            // singleton serializer -- just get it
            instantiateObject(iv, maybeSerializer)
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

        fun instantiate(typeArgument: Pair<IrType, IrClassSymbol?>, signature: StringBuilder?) {
            val (argType, argSerializer) = typeArgument
            stackValueSerializerInstance(
                argType,
                argSerializer,
                iv,
                intrinsicType,
                argType.genericIndex,
                genericSerializerFieldGetter
            )
            // wrap into nullable serializer if argType is nullable
            if (argType.isMarkedNullable()) iv.wrapStackValueIntoNullableSerializer()
            signature?.append(kSerializerType.descriptor)
        }

        val serializer = maybeSerializer ?: iv.run {
            require(intrinsicType is IntrinsicType.WithModule) // SIMPLE is covered in previous if
            // SerializersModule
            load(intrinsicType.storedIndex, serializersModuleType)
            // KClass
            aconst(typeMapper.mapTypeCommon(kType, TypeMappingMode.GENERIC_ARGUMENT))
            AsmUtil.wrapJavaClassIntoKClass(this)

            val descriptor = StringBuilder("(${serializersModuleType.descriptor}${AsmTypes.K_CLASS_TYPE.descriptor}")
            // Generic args (if present)
            if (argSerializers.isNotEmpty()) {
                fillArray(kSerializerType, argSerializers) { _, serializer ->
                    instantiate(serializer, null)
                }
                descriptor.append(kSerializerArrayType.descriptor)
            }
            descriptor.append(")${kSerializerType.descriptor}")
            invokestatic(
                serializersKtInternalName,
                noCompiledSerializerMethodName,
                descriptor.toString(),
                false
            )
            return false
        }

        // new serializer if needed
        iv.apply {
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
                    if (serializer.owner.classId == contextSerializerId && hasNewContextSerializerSignature) {
                        // append new additional arguments
                        val fallbackDefaultSerializer = findTypeSerializer(this@SerializationJvmIrIntrinsicSupport, kType)
                        if (fallbackDefaultSerializer != null && fallbackDefaultSerializer != serializer) {
                            instantiate(kType to fallbackDefaultSerializer, null)
                        } else {
                            aconst(null)
                        }
                        signature.append(kSerializerType.descriptor)
                        fillArray(kSerializerType, argSerializers) { _, serializer ->
                            instantiate(serializer, null)
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
                    instantiate(argSerializers[0], signature)
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
                                intrinsicType,
                                argType.genericIndex
                            ) { genericType ->
                                // if we encountered generic type parameter in one of subclasses of sealed class, use polymorphism from upper bound
                                assert(
                                    stackValueSerializerInstance(
                                        (genericType.classifierOrNull as IrTypeParameterSymbol).owner.representativeUpperBound,
                                        jvmBackendContext.referenceClass(module.getClassFromSerializationPackage(SpecialBuiltins.polymorphicSerializer)),
                                        this, intrinsicType
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
                else -> argSerializers.forEach { instantiate(it, signature) }
            }
            signature.append(")V")
            // invoke constructor
            invokespecial(serializerType.internalName, "<init>", signature.toString(), false)
        }
        return true
    }
}