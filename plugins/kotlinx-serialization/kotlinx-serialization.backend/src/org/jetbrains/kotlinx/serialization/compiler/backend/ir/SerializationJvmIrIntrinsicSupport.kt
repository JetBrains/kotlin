/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.codegen.*
import org.jetbrains.kotlin.backend.jvm.intrinsics.IntrinsicMethod
import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.backend.jvm.mapping.mapClass
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.codegen.extractUsedReifiedParameters
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.Companion.pluginIntrinsicsMarkerMethod
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.Companion.pluginIntrinsicsMarkerOwner
import org.jetbrains.kotlin.codegen.inline.ReifiedTypeInliner.Companion.pluginIntrinsicsMarkerSignature
import org.jetbrains.kotlin.config.ApiVersion
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
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
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames.ENUM_SERIALIZER_FACTORY_FUNC_NAME
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
import org.jetbrains.org.objectweb.asm.tree.LdcInsnNode
import org.jetbrains.org.objectweb.asm.tree.VarInsnNode

class SerializationJvmIrIntrinsicSupport(
    private val jvmBackendContext: JvmBackendContext,
    private val irPluginContext: IrPluginContext
) : SerializationBaseContext, JvmIrIntrinsicExtension {
    sealed class IntrinsicType(val methodDescriptor: String) {
        object Simple : IntrinsicType(stubCallDescriptor)

        class WithModule(val storedIndex: Int) :
            IntrinsicType(stubCallDescriptorWithModule)

        fun magicMarkerString(): String = magicMarkerStringPrefix + when (this) {
            is Simple -> "simple"
            is WithModule -> "withModule"
        }
    }

    inner class ReifiedSerializerMethod(private val withModule: Boolean) : IntrinsicMethod() {
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
                    expression.markLineNumber(startOffset = true)
                    IntrinsicType.Simple
                }
                generateSerializerForType(
                    argument,
                    mv,
                    intrinsicType
                )
                codegen.propagateChildReifiedTypeParametersUsages(codegen.typeMapper.typeSystem.extractUsedReifiedParameters(argument))
                if (withModule) {
                    frameMap.leaveTemp(serializersModuleType)
                }
                return MaterialValue(codegen, kSerializerType, expression.type)
            }
        }
    }

    companion object {
        val serializersModuleType: Type = Type.getObjectType("kotlinx/serialization/modules/SerializersModule")
        val kTypeType: Type = AsmTypes.K_TYPE

        val stubCallDescriptorWithModule = "(${serializersModuleType.descriptor}${kTypeType.descriptor})${kSerializerType.descriptor}"
        val stubCallDescriptor = "(${kTypeType.descriptor})${kSerializerType.descriptor}"
        const val serializersKtInternalName = "kotlinx/serialization/SerializersKt"
        const val callMethodName = "serializer"
        const val noCompiledSerializerMethodName = "noCompiledSerializer"

        const val magicMarkerStringPrefix = "kotlinx.serialization.serializer."

    }

    /**
     * Method for intrinsification `kotlinx.serialization.serializer` is a top-level function.
     * For the rest of the world, it is located in the facade `kotlinx.serialization.SerializersKt`.
     * However, when we compile `kotlinx-serialization-core` itself, facade contains only synthetic bridges.
     * Real function is contained in IR class with `SerializersKt__SerializersKt` name.
     * (as we have `@file:JvmMultifileClass @file:JvmName("SerializersKt")` on both common Serializers.kt and a platform-specific SerializersJvm.kt files)
     */
    private fun IrFunction.isTargetMethod(): Boolean {
        val fqName = fqNameWhenAvailable?.asString() ?: return false
        return fqName == "kotlinx.serialization.SerializersKt.serializer" || fqName == "kotlinx.serialization.SerializersKt__SerializersKt.serializer"
    }

    override fun getIntrinsic(symbol: IrFunctionSymbol): IntrinsicMethod? {
        val method = symbol.owner
        if (!method.isTargetMethod()
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


    private val emptyGenerator: BaseIrGenerator? = null
    private val module = jvmBackendContext.state.module
    private val typeSystemContext = jvmBackendContext.typeSystem
    private val typeMapper = jvmBackendContext.defaultTypeMapper

    override fun referenceClassId(classId: ClassId): IrClassSymbol? = irPluginContext.referenceClass(classId)

    private val currentVersion by lazy {
        VersionReader.getVersionsForCurrentModuleFromTrace(module, jvmBackendContext.state.bindingTrace)
            ?.implementationVersion
    }

    override val runtimeHasEnumSerializerFactoryFunctions: Boolean
        get() = currentVersion != null && currentVersion!! >= ApiVersion.parse("1.5.0")!!

    private val hasNewContextSerializerSignature: Boolean
        get() = currentVersion != null && currentVersion!! >= ApiVersion.parse("1.2.0")!!

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

    /**
     * Instructions at the moment of call:
     *
     * -3: iconst(6) // TYPE_OF
     * -2: aconst(typeParamName) // TYPE_OF
     * -1: invokestatic(reifiedOperationMarker)
     * < instructions from instructionAdapter will be inserted here by inliner >
     *  0 (stubConstNull): aconst(null)
     * 1: aconst(kotlinx.serialization.serializer.<operationType>)
     * 2: invokestatic(voidMagicApiCall)
     * 3: aload(moduleVar) // if withModule
     * 4: swap // if withModule
     * 5: invokestatic(kotlinx.serialization.serializer(module?, kType)
     *
     * We need to remove instructions from 0 to 5
     * Instructions -1, -2 and -3 would be removed by inliner.
     */
    override fun rewritePluginDefinedOperationMarker(
        v: InstructionAdapter,
        reifiedInsn: AbstractInsnNode,
        instructions: InsnList,
        type: IrType
    ): Boolean {
        val operationTypeStr = (reifiedInsn.next as LdcInsnNode).cst as String
        if (!operationTypeStr.startsWith(magicMarkerStringPrefix)) return false
        val operationType = if (operationTypeStr.endsWith("withModule")) {
            val aload = reifiedInsn.next.next.next as VarInsnNode
            val storedVar = aload.`var`
            instructions.remove(aload.next)
            instructions.remove(aload)
            IntrinsicType.WithModule(storedVar)
        } else IntrinsicType.Simple
        // Remove other instructions
        instructions.remove(reifiedInsn.next.next.next)
        instructions.remove(reifiedInsn.next.next)
        instructions.remove(reifiedInsn.next)
        instructions.remove(reifiedInsn)
        // generate serializer
        generateSerializerForType(type, v, operationType)
        return true
    }

    /**
     * This function produces identical to TYPE_OF reification marker. This is needed for compatibility reasons:
     * old compiler should be able to inline and run newer versions of kotlinx-serialization or other libraries.
     *
     * Operation detection in new compilers performed by voidMagicApiCall.
     */
    private fun InstructionAdapter.putReifyMarkerIfNeeded(type: IrType, intrinsicType: IntrinsicType): Boolean =
        with(typeSystemContext) {
            val typeDescriptor = type.typeConstructor().getTypeParameterClassifier()
            if (typeDescriptor != null) { // need further reification
                ReifiedTypeInliner.putReifiedOperationMarkerIfNeeded(
                    typeDescriptor,
                    type.isMarkedNullable(),
                    ReifiedTypeInliner.OperationKind.TYPE_OF,
                    this@putReifyMarkerIfNeeded,
                    typeSystemContext
                )
                aconst(null)
                aconst(intrinsicType.magicMarkerString())
                invokestatic(pluginIntrinsicsMarkerOwner, pluginIntrinsicsMarkerMethod, pluginIntrinsicsMarkerSignature, false)
                if (intrinsicType is IntrinsicType.WithModule) {
                    // Force emit load instruction so we can retrieve var index later
                    load(intrinsicType.storedIndex, serializersModuleType)
                    swap()
                }
                invokestatic(serializersKtInternalName, callMethodName, intrinsicType.methodDescriptor, false)
                return true
            }
            return false
        }

    private fun generateThrowOnStarProjection(parentType: IrSimpleType, adapter: InstructionAdapter) {
        val iaeName = "java/lang/IllegalArgumentException"
        with(adapter) {
            anew(Type.getObjectType(iaeName))
            dup()
            aconst("Star projections in type arguments are not allowed, but had ${parentType.render()}")
            invokespecial(iaeName, "<init>", "(Ljava/lang/String;)V", false)
            checkcast(Type.getObjectType("java/lang/Throwable"))
            athrow()
        }
    }

    fun generateSerializerForType(
        type: IrType,
        adapter: InstructionAdapter,
        intrinsicType: IntrinsicType
    ) {
        if (adapter.putReifyMarkerIfNeeded(type, intrinsicType)) return
        val typeDescriptor: IrClass = type.classOrNull!!.owner

        val support = this@SerializationJvmIrIntrinsicSupport

        val serializerMethod = SerializableCompanionIrGenerator.getSerializerGetterFunction(typeDescriptor)
        if (serializerMethod != null) {
            // fast path
            val companionType = if (typeDescriptor.isSerializableObject) typeDescriptor else typeDescriptor.companionObject()!!
            support.instantiateObject(adapter, companionType.symbol)
            val args = (type as IrSimpleType).arguments.map {
                it.typeOrNull ?: run {
                    generateThrowOnStarProjection(type, adapter)
                    return
                }
            }
            args.forEach { generateSerializerForType(it, adapter, intrinsicType) }
            val signature = kSerializerType.descriptor.repeat(args.size)
            adapter.invokevirtual(
                typeMapper.mapClass(companionType).internalName,
                "serializer",
                "(${signature})${kSerializerType.descriptor}",
                false
            )
        } else {
            // More general path, including special or built-in serializers for e.g. List
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

    private fun stackValueSerializerInstance(
        kType: IrType, maybeSerializer: IrClassSymbol?,
        iv: InstructionAdapter,
        intrinsicType: IntrinsicType,
        genericIndex: Int? = null,
        genericSerializerFieldGetter: (InstructionAdapter.(IrType) -> Unit)? = null,
    ): Boolean {
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
        val typeArgumentsAsTypes = (kType as IrSimpleType).arguments.map {
            it.typeOrNull ?: run {
                generateThrowOnStarProjection(kType, iv)
                return false
            }
        }
        val argSerializers = typeArgumentsAsTypes.map { argType ->
            // check if any type argument is not serializable
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

        val serializer = maybeSerializer ?: iv.run {// insert noCompilerSerializer(module, kClass, arguments)
            require(intrinsicType is IntrinsicType.WithModule) // SIMPLE is covered in previous if
            // SerializersModule
            load(intrinsicType.storedIndex, serializersModuleType)
            // KClass
            aconst(typeMapper.mapTypeCommon(kType, TypeMappingMode.GENERIC_ARGUMENT))
            AsmUtil.wrapJavaClassIntoKClass(this)

            val descriptor = StringBuilder("(${serializersModuleType.descriptor}${AsmTypes.K_CLASS_TYPE.descriptor}")
            // Generic args (if present)
            if (argSerializers.isNotEmpty()) {
                fillArray(kSerializerType, argSerializers) { _, (type, _) ->
                    generateSerializerForType(type, this, intrinsicType)
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
                    // FIXME: same as fillArray above
                    aconst(null)

                    invokestatic(
                        enumFactoriesType.internalName,
                        ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME.asString(),
                        "(${stringType.descriptor}${javaEnumArray.descriptor}${stringArrayType.descriptor}${doubleAnnotationArrayType.descriptor}${annotationArrayType.descriptor})${kSerializerType.descriptor}",
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
                        fillArray(kSerializerType, argSerializers) { _, (type, _) ->
                            generateSerializerForType(type, this, intrinsicType)
                        }
                        signature.append(kSerializerArrayType.descriptor)
                    }
                }

                referenceArraySerializerId -> {
                    // a special way to instantiate reference array serializer -- need an element KClass reference
                    aconst(typeMapper.mapTypeCommon(typeArgumentsAsTypes.first(), TypeMappingMode.GENERIC_ARGUMENT))
                    AsmUtil.wrapJavaClassIntoKClass(this)
                    signature.append(AsmTypes.K_CLASS_TYPE.descriptor)
                    // Reference array serializer still needs serializer for its argument type
                    generateSerializerForType(argSerializers[0].first, this, intrinsicType)
                    signature.append(kSerializerType.descriptor)
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
                else -> argSerializers.forEach { (type, _) ->
                    generateSerializerForType(type, this, intrinsicType)
                    signature.append(kSerializerType.descriptor)
                }
            }
            signature.append(")V")
            // invoke constructor
            invokespecial(serializerType.internalName, "<init>", signature.toString(), false)
        }
        return true
    }
}
