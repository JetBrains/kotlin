/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.toCValues
import kotlinx.cinterop.toKString
import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.konan.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

internal sealed class SlotType {
    // An object is statically allocated on stack.
    object STACK : SlotType()

    // Frame local arena slot can be used.
    object ARENA : SlotType()

    // Return slot can be used.
    object RETURN : SlotType()

    // Return slot, if it is an arena, can be used.
    object RETURN_IF_ARENA : SlotType()

    // Param slot, if it is an arena, can be used.
    class PARAM_IF_ARENA(val parameter: Int) : SlotType()

    // Params slot, if it is an arena, can be used.
    class PARAMS_IF_ARENA(val parameters: IntArray, val useReturnSlot: Boolean) : SlotType()

    // Anonymous slot.
    object ANONYMOUS : SlotType()

    // Unknown slot type.
    object UNKNOWN : SlotType()
}

// Lifetimes class of reference, computed by escape analysis.
internal sealed class Lifetime(val slotType: SlotType) {
    object STACK : Lifetime(SlotType.STACK) {
        override fun toString(): String {
            return "STACK"
        }
    }

    // If reference is frame-local (only obtained from some call and never leaves).
    object LOCAL : Lifetime(SlotType.ARENA) {
        override fun toString(): String {
            return "LOCAL"
        }
    }

    // If reference is only returned.
    object RETURN_VALUE : Lifetime(SlotType.ANONYMOUS) {
        override fun toString(): String {
            return "RETURN_VALUE"
        }
    }

    // If reference is set as field of references of class RETURN_VALUE or INDIRECT_RETURN_VALUE.
    object INDIRECT_RETURN_VALUE : Lifetime(SlotType.RETURN_IF_ARENA) {
        override fun toString(): String {
            return "INDIRECT_RETURN_VALUE"
        }
    }

    // If reference is stored to the field of an incoming parameters.
    class PARAMETER_FIELD(val parameter: Int) : Lifetime(SlotType.PARAM_IF_ARENA(parameter)) {
        override fun toString(): String {
            return "PARAMETER_FIELD($parameter)"
        }
    }

    // If reference is stored to the field of an incoming parameters.
    class PARAMETERS_FIELD(val parameters: IntArray, val useReturnSlot: Boolean)
        : Lifetime(SlotType.PARAMS_IF_ARENA(parameters, useReturnSlot)) {
        override fun toString(): String {
            return "PARAMETERS_FIELD(${parameters.contentToString()}, useReturnSlot='$useReturnSlot')"
        }
    }

    // If reference refers to the global (either global object or global variable).
    object GLOBAL : Lifetime(SlotType.ANONYMOUS) {
        override fun toString(): String {
            return "GLOBAL"
        }
    }

    // If reference used to throw.
    object THROW : Lifetime(SlotType.ANONYMOUS) {
        override fun toString(): String {
            return "THROW"
        }
    }

    // If reference used as an argument of outgoing function. Class can be improved by escape analysis
    // of called function.
    object ARGUMENT : Lifetime(SlotType.ANONYMOUS) {
        override fun toString(): String {
            return "ARGUMENT"
        }
    }

    // If reference class is unknown.
    object UNKNOWN : Lifetime(SlotType.UNKNOWN) {
        override fun toString(): String {
            return "UNKNOWN"
        }
    }

    // If reference class is irrelevant.
    object IRRELEVANT : Lifetime(SlotType.UNKNOWN) {
        override fun toString(): String {
            return "IRRELEVANT"
        }
    }
}

/**
 * Provides utility methods to the implementer.
 */
internal interface ContextUtils : RuntimeAware {
    val generationState: NativeGenerationState

    val context: Context
        get() = generationState.context

    override val runtime: Runtime
        get() = generationState.llvm.runtime

    val argumentAbiInfo: TargetAbiInfo
        get() = context.targetAbiInfo

    /**
     * Describes the target platform.
     *
     * TODO: using [llvmTargetData] usually results in generating non-portable bitcode.
     */
    val llvmTargetData: LLVMTargetDataRef
        get() = runtime.targetData

    val llvm: CodegenLlvmHelpers
        get() = generationState.llvm

    val staticData: KotlinStaticData
        get() = generationState.llvm.staticData

    /**
     * TODO: maybe it'd be better to replace with [IrDeclaration::isEffectivelyExternal()],
     * or just drop all [else] branches of corresponding conditionals.
     */
    fun isExternal(declaration: IrDeclaration): Boolean {
        return !generationState.llvmModuleSpecification.containsDeclaration(declaration)
    }

    fun linkageOf(irFunction: IrFunction) = when {
        isExternal(irFunction) -> LLVMLinkage.LLVMExternalLinkage
        irFunction.isExported() -> LLVMLinkage.LLVMExternalLinkage
        context.config.producePerFileCache && irFunction in generationState.calledFromExportedInlineFunctions -> LLVMLinkage.LLVMExternalLinkage
        else -> LLVMLinkage.LLVMInternalLinkage
    }

    /**
     * LLVM function generated from the Kotlin function.
     * It may be declared as external function prototype.
     */
    val IrFunction.llvmFunction: LlvmCallable
        get() = llvmFunctionOrNull
                ?: error("$name in ${file.name}/${parent.fqNameForIrSerialization}")

    val IrFunction.llvmFunctionOrNull: LlvmCallable?
        get() {
            assert(this.isReal) {
                this.computeFullName()
            }
            return if (isExternal(this)) {
                runtime.addedLLVMExternalFunctions.getOrPut(this) {
                    val symbolName = if (KonanBinaryInterface.isExported(this)) {
                        this.computeSymbolName()
                    } else {
                        val containerName = parentClassOrNull?.fqNameForIrSerialization?.asString()
                                ?: context.irLinker.getExternalDeclarationFileName(this)
                        this.computePrivateSymbolName(containerName)
                    }
                    val proto = LlvmFunctionProto(this, symbolName, this@ContextUtils, LLVMLinkage.LLVMExternalLinkage)
                    llvm.externalFunction(proto)
                }
            } else {
                generationState.llvmDeclarations.forFunctionOrNull(this)
            }
        }

    /**
     * Address of entry point of [llvmFunction].
     */
    val IrFunction.entryPointAddress: ConstPointer
        get() {
            return llvmFunction.toConstPointer().bitcast(llvm.int8PtrType)
        }

    val IrClass.typeInfoPtr: ConstPointer
        get() {
            return if (isExternal(this)) {
                val typeInfoSymbolName = if (KonanBinaryInterface.isExported(this)) {
                    this.computeTypeInfoSymbolName()
                } else {
                    this.computePrivateTypeInfoSymbolName(context.irLinker.getExternalDeclarationFileName(this))
                }

                constPointer(importGlobal(typeInfoSymbolName, runtime.typeInfoType, this))
            } else {
                generationState.llvmDeclarations.forClass(this).typeInfo
            }
        }

    /**
     * Pointer to type info for given class.
     * It may be declared as pointer to external variable.
     */
    val IrClass.llvmTypeInfoPtr: LLVMValueRef
        get() = typeInfoPtr.llvm

}

/**
 * Converts this string to the sequence of bytes to be used for hashing/storing to binary/etc.
 */
internal fun stringAsBytes(str: String) = str.toByteArray(Charsets.UTF_8)

internal class ScopeInitializersGenerationState {
    val topLevelFields = mutableListOf<IrField>()
    var globalInitFunction: IrFunction? = null
    var globalInitState: LLVMValueRef? = null
    var threadLocalInitFunction: IrFunction? = null
    var threadLocalInitState: AddressAccess? = null
    val globalSharedObjects = mutableSetOf<LLVMValueRef>()
    fun isEmpty() = topLevelFields.isEmpty() &&
            globalInitState == null &&
            threadLocalInitState == null &&
            globalSharedObjects.isEmpty()
}

internal class InitializersGenerationState {
    val fileGlobalInitStates = mutableMapOf<IrDeclarationContainer, LLVMValueRef>()
    val fileThreadLocalInitStates = mutableMapOf<IrDeclarationContainer, AddressAccess>()

    var scopeState = ScopeInitializersGenerationState()

    fun reset(newState: ScopeInitializersGenerationState) : ScopeInitializersGenerationState {
        val t = scopeState
        scopeState = newState
        return t
    }
}

internal class ConstInt1(llvm: CodegenLlvmHelpers, val value: Boolean) : ConstValue {
    override val llvm = LLVMConstInt(llvm.int1Type, if (value) 1 else 0, 1)!!
}

internal class ConstInt8(llvm: CodegenLlvmHelpers, val value: Byte) : ConstValue {
    override val llvm = LLVMConstInt(llvm.int8Type, value.toLong(), 1)!!
}

internal class ConstInt16(llvm: CodegenLlvmHelpers, val value: Short) : ConstValue {
    override val llvm = LLVMConstInt(llvm.int16Type, value.toLong(), 1)!!
}

internal class ConstChar16(llvm: CodegenLlvmHelpers, val value: Char) : ConstValue {
    override val llvm = LLVMConstInt(llvm.int16Type, value.code.toLong(), 1)!!
}

internal class ConstInt32(llvm: CodegenLlvmHelpers, val value: Int) : ConstValue {
    override val llvm = LLVMConstInt(llvm.int32Type, value.toLong(), 1)!!
}

internal class ConstInt64(llvm: CodegenLlvmHelpers, val value: Long) : ConstValue {
    override val llvm = LLVMConstInt(llvm.int64Type, value, 1)!!
}

internal class ConstFloat32(llvm: CodegenLlvmHelpers, val value: Float) : ConstValue {
    override val llvm = LLVMConstReal(llvm.floatType, value.toDouble())!!
}

internal class ConstFloat64(llvm: CodegenLlvmHelpers, val value: Double) : ConstValue {
    override val llvm = LLVMConstReal(llvm.doubleType, value)!!
}

internal open class BasicLlvmHelpers(bitcodeContext: BitcodePostProcessingContext, val module: LLVMModuleRef) {

    val llvmContext = bitcodeContext.llvmContext
    val targetTriple by lazy {
        LLVMGetTarget(module)!!.toKString()
    }

    val runtimeAnnotationMap by lazy {
        StaticData.getGlobal(module, "llvm.global.annotations")
                ?.getInitializer()
                ?.let { getOperands(it) }
                ?.groupBy(
                        { LLVMGetInitializer(LLVMGetOperand(LLVMGetOperand(it, 1), 0))?.getAsCString() ?: "" },
                        { LLVMGetOperand(LLVMGetOperand(it, 0), 0)!! }
                )
                ?.filterKeys { it != "" }
                ?: emptyMap()
    }
}

@Suppress("FunctionName", "PropertyName", "PrivatePropertyName")
internal class CodegenLlvmHelpers(private val generationState: NativeGenerationState, module: LLVMModuleRef) : BasicLlvmHelpers(generationState, module), RuntimeAware {
    private val context = generationState.context

    private fun importFunction(name: String, otherModule: LLVMModuleRef): LlvmCallable {
        if (LLVMGetNamedFunction(module, name) != null) {
            throw IllegalArgumentException("function $name already exists")
        }

        val externalFunction = LLVMGetNamedFunction(otherModule, name) ?: throw Error("function $name not found")

        val attributesCopier = LlvmFunctionAttributeProvider.copyFromExternal(externalFunction)

        val functionType = getFunctionType(externalFunction)
        val function = LLVMAddFunction(module, name, functionType)!!

        attributesCopier.addFunctionAttributes(function)

        return LlvmCallable(function, attributesCopier)
    }

    private fun importGlobal(name: String, otherModule: LLVMModuleRef): LLVMValueRef {
        if (LLVMGetNamedGlobal(module, name) != null) {
            throw IllegalArgumentException("global $name already exists")
        }

        val externalGlobal = LLVMGetNamedGlobal(otherModule, name)!!
        val globalType = getGlobalType(externalGlobal)
        val global = LLVMAddGlobal(module, globalType, name)!!

        return global
    }

    private fun importMemset(): LlvmCallable {
        val functionType = functionType(voidType, false, int8PtrType, int8Type, int32Type, int1Type)
        return llvmIntrinsic("llvm.memset.p0i8.i32", functionType)
    }

    private fun llvmIntrinsic(name: String, type: LLVMTypeRef, vararg attributes: String): LlvmCallable {
        val result = LLVMAddFunction(module, name, type)!!
        attributes.forEach {
            val kindId = getLlvmAttributeKindId(it)
            addLlvmFunctionEnumAttribute(result, kindId)
        }
        return LlvmCallable(result, LlvmFunctionAttributeProvider.copyFromExternal(result))
    }

    internal fun externalFunction(llvmFunctionProto: LlvmFunctionProto): LlvmCallable {
        if (llvmFunctionProto.origin != null) {
            this.dependenciesTracker.add(llvmFunctionProto.origin, onlyBitcode = llvmFunctionProto.independent)
        }
        val found = LLVMGetNamedFunction(module, llvmFunctionProto.name)
        if (found != null) {
            require(getFunctionType(found) == llvmFunctionProto.signature.llvmFunctionType) {
                "Expected: ${LLVMPrintTypeToString(llvmFunctionProto.signature.llvmFunctionType)!!.toKString()} " +
                        "found: ${LLVMPrintTypeToString(getFunctionType(found))!!.toKString()}"
            }
            require(LLVMGetLinkage(found) == llvmFunctionProto.linkage)
            return LlvmCallable(found, llvmFunctionProto.signature)
        } else {
            return llvmFunctionProto.createLlvmFunction(context, module)
        }
    }

    internal fun externalNativeRuntimeFunction(
            name: String,
            returnType: LlvmRetType,
            parameterTypes: List<LlvmParamType> = emptyList(),
            functionAttributes: List<LlvmFunctionAttribute> = emptyList(),
            isVararg: Boolean = false
    ) = externalFunction(
            LlvmFunctionSignature(returnType, parameterTypes, isVararg, functionAttributes).toProto(
                    name,
                    origin = FunctionOrigin.FromNativeRuntime,
                    linkage = LLVMLinkage.LLVMExternalLinkage,
                    independent = false
            )
    )

    internal fun externalNativeRuntimeFunction(name: String, signature: LlvmFunctionSignature) =
            externalNativeRuntimeFunction(name, signature.returnType, signature.parameterTypes, signature.functionAttributes, signature.isVararg)

    val dependenciesTracker get() = generationState.dependenciesTracker

    val additionalProducedBitcodeFiles = mutableListOf<String>()

    val staticData = KotlinStaticData(generationState, this, module)

    private val target = context.config.target

    override val runtime get() = generationState.runtime

    init {
        LLVMSetDataLayout(module, runtime.dataLayout)
        LLVMSetTarget(module, runtime.target)
    }

    private fun importRtFunction(name: String) = importFunction(name, runtime.llvmModule)

    val allocInstanceFunction = importRtFunction("AllocInstance")
    val allocArrayFunction = importRtFunction("AllocArrayInstance")
    val initAndRegisterGlobalFunction = importRtFunction("InitAndRegisterGlobal")
    val updateHeapRefFunction = importRtFunction("UpdateHeapRef")
    val updateStackRefFunction = importRtFunction("UpdateStackRef")
    val updateReturnRefFunction = importRtFunction("UpdateReturnRef")
    val zeroHeapRefFunction = importRtFunction("ZeroHeapRef")
    val zeroArrayRefsFunction = importRtFunction("ZeroArrayRefs")
    val enterFrameFunction = importRtFunction("EnterFrame")
    val leaveFrameFunction = importRtFunction("LeaveFrame")
    val setCurrentFrameFunction = importRtFunction("SetCurrentFrame")
    val checkCurrentFrameFunction = importRtFunction("CheckCurrentFrame")
    val lookupInterfaceTableRecord = importRtFunction("LookupInterfaceTableRecord")
    val isSubtypeFunction = importRtFunction("IsSubtype")
    val isSubclassFastFunction = importRtFunction("IsSubclassFast")
    val throwExceptionFunction = importRtFunction("ThrowException")
    val appendToInitalizersTail = importRtFunction("AppendToInitializersTail")
    val callInitGlobalPossiblyLock = importRtFunction("CallInitGlobalPossiblyLock")
    val callInitThreadLocal = importRtFunction("CallInitThreadLocal")
    val addTLSRecord = importRtFunction("AddTLSRecord")
    val lookupTLS = importRtFunction("LookupTLS")
    val initRuntimeIfNeeded = importRtFunction("Kotlin_initRuntimeIfNeeded")
    val mutationCheck = importRtFunction("MutationCheck")
    val checkLifetimesConstraint = importRtFunction("CheckLifetimesConstraint")
    val freezeSubgraph = importRtFunction("FreezeSubgraph")
    val checkGlobalsAccessible = importRtFunction("CheckGlobalsAccessible")
    val Kotlin_getExceptionObject = importRtFunction("Kotlin_getExceptionObject")

    val kRefSharedHolderInitLocal = importRtFunction("KRefSharedHolder_initLocal")
    val kRefSharedHolderInit = importRtFunction("KRefSharedHolder_init")
    val kRefSharedHolderDispose = importRtFunction("KRefSharedHolder_dispose")
    val kRefSharedHolderRef = importRtFunction("KRefSharedHolder_ref")

    val createKotlinObjCClass by lazy { importRtFunction("CreateKotlinObjCClass") }
    val getObjCKotlinTypeInfo by lazy { importRtFunction("GetObjCKotlinTypeInfo") }
    val missingInitImp by lazy { importRtFunction("MissingInitImp") }

    val Kotlin_mm_switchThreadStateNative by lazy { importRtFunction("Kotlin_mm_switchThreadStateNative") }
    val Kotlin_mm_switchThreadStateRunnable by lazy { importRtFunction("Kotlin_mm_switchThreadStateRunnable") }

    val Kotlin_Interop_DoesObjectConformToProtocol by lazyRtFunction
    val Kotlin_Interop_IsObjectKindOfClass by lazyRtFunction

    val Kotlin_ObjCExport_refToLocalObjC by lazyRtFunction
    val Kotlin_ObjCExport_refToRetainedObjC by lazyRtFunction
    val Kotlin_ObjCExport_refFromObjC by lazyRtFunction
    val Kotlin_ObjCExport_CreateRetainedNSStringFromKString by lazyRtFunction
    val Kotlin_ObjCExport_convertUnitToRetained by lazyRtFunction
    val Kotlin_ObjCExport_GetAssociatedObject by lazyRtFunction
    val Kotlin_ObjCExport_AbstractMethodCalled by lazyRtFunction
    val Kotlin_ObjCExport_AbstractClassConstructorCalled by lazyRtFunction
    val Kotlin_ObjCExport_RethrowExceptionAsNSError by lazyRtFunction
    val Kotlin_ObjCExport_WrapExceptionToNSError by lazyRtFunction
    val Kotlin_ObjCExport_NSErrorAsException by lazyRtFunction
    val Kotlin_ObjCExport_AllocInstanceWithAssociatedObject by lazyRtFunction
    val Kotlin_ObjCExport_createContinuationArgument by lazyRtFunction
    val Kotlin_ObjCExport_createUnitContinuationArgument by lazyRtFunction
    val Kotlin_ObjCExport_resumeContinuation by lazyRtFunction

    private val Kotlin_ObjCExport_NSIntegerTypeProvider by lazyRtFunction
    private val Kotlin_longTypeProvider by lazyRtFunction

    val Kotlin_mm_safePointFunctionPrologue by lazyRtFunction
    val Kotlin_mm_safePointWhileLoopBody by lazyRtFunction

    val Kotlin_processObjectInMark by lazyRtFunction
    val Kotlin_processArrayInMark by lazyRtFunction
    val Kotlin_processFieldInMark by lazyRtFunction
    val Kotlin_processEmptyObjectInMark by lazyRtFunction

    val UpdateVolatileHeapRef by lazyRtFunction
    val CompareAndSetVolatileHeapRef by lazyRtFunction
    val CompareAndSwapVolatileHeapRef by lazyRtFunction
    val GetAndSetVolatileHeapRef by lazyRtFunction

    // TODO: Consider implementing them directly in the code generator.
    val Kotlin_arrayGetElementAddress by lazyRtFunction
    val Kotlin_intArrayGetElementAddress by lazyRtFunction
    val Kotlin_longArrayGetElementAddress by lazyRtFunction

    val tlsMode by lazy {
        when (target) {
            KonanTarget.WASM32,
            is KonanTarget.ZEPHYR -> LLVMThreadLocalMode.LLVMNotThreadLocal
            else -> LLVMThreadLocalMode.LLVMGeneralDynamicTLSModel
        }
    }

    val usedFunctions = mutableListOf<LlvmCallable>()
    val usedGlobals = mutableListOf<LLVMValueRef>()
    val compilerUsedGlobals = mutableListOf<LLVMValueRef>()
    val irStaticInitializers = mutableListOf<IrStaticInitializer>()
    val otherStaticInitializers = mutableListOf<LlvmCallable>()
    val initializersGenerationState = InitializersGenerationState()
    val boxCacheGlobals = mutableMapOf<BoxCache, StaticData.Global>()

    private object lazyRtFunction {
        operator fun provideDelegate(
                thisRef: CodegenLlvmHelpers, property: KProperty<*>
        ) = object : ReadOnlyProperty<CodegenLlvmHelpers, LlvmCallable> {

            val value: LlvmCallable by lazy { thisRef.importRtFunction(property.name) }

            override fun getValue(thisRef: CodegenLlvmHelpers, property: KProperty<*>): LlvmCallable = value
        }
    }

    val int1Type = LLVMInt1TypeInContext(llvmContext)!!
    val int8Type = LLVMInt8TypeInContext(llvmContext)!!
    val int16Type = LLVMInt16TypeInContext(llvmContext)!!
    val int32Type = LLVMInt32TypeInContext(llvmContext)!!
    val int64Type = LLVMInt64TypeInContext(llvmContext)!!
    val floatType = LLVMFloatTypeInContext(llvmContext)!!
    val doubleType = LLVMDoubleTypeInContext(llvmContext)!!
    val vector128Type = LLVMVectorType(floatType, 4)!!
    val voidType = LLVMVoidTypeInContext(llvmContext)!!
    val int8PtrType = pointerType(int8Type)
    val int8PtrPtrType = pointerType(int8PtrType)

    fun structType(vararg types: LLVMTypeRef): LLVMTypeRef = structType(types.toList())

    fun struct(vararg elements: ConstValue) = Struct(structType(elements.map { it.llvmType }), *elements)

    private fun structType(types: List<LLVMTypeRef>): LLVMTypeRef =
            LLVMStructTypeInContext(llvmContext, types.toCValues(), types.size, 0)!!

    fun constInt1(value: Boolean) = ConstInt1(this, value)
    fun constInt8(value: Byte) = ConstInt8(this, value)
    fun constInt16(value: Short) = ConstInt16(this, value)
    fun constChar16(value: Char) = ConstChar16(this, value)
    fun constInt32(value: Int) = ConstInt32(this, value)
    fun constInt64(value: Long) = ConstInt64(this, value)
    fun constFloat32(value: Float) = ConstFloat32(this, value)
    fun constFloat64(value: Double) = ConstFloat64(this, value)

    fun int1(value: Boolean): LLVMValueRef = constInt1(value).llvm
    fun int8(value: Byte): LLVMValueRef = constInt8(value).llvm
    fun int16(value: Short): LLVMValueRef = constInt16(value).llvm
    fun char16(value: Char): LLVMValueRef = constChar16(value).llvm
    fun int32(value: Int): LLVMValueRef = constInt32(value).llvm
    fun int64(value: Long): LLVMValueRef = constInt64(value).llvm
    fun float32(value: Float): LLVMValueRef = constFloat32(value).llvm
    fun float64(value: Double): LLVMValueRef = constFloat64(value).llvm

    val kNullInt8Ptr by lazy { LLVMConstNull(int8PtrType)!! }
    val kNullInt32Ptr by lazy { LLVMConstNull(pointerType(int32Type))!! }
    val kImmInt32Zero by lazy { int32(0) }
    val kImmInt32One by lazy { int32(1) }

    val memsetFunction = importMemset()

    val llvmTrap = llvmIntrinsic(
            "llvm.trap",
            functionType(voidType, false),
            "cold", "noreturn", "nounwind"
    )

    val llvmEhTypeidFor = llvmIntrinsic(
            "llvm.eh.typeid.for",
            functionType(int32Type, false, int8PtrType),
            "nounwind", "readnone"
    )

    var tlsCount = 0

    val tlsKey by lazy {
        val global = LLVMAddGlobal(module, int8PtrType, "__KonanTlsKey")!!
        LLVMSetLinkage(global, LLVMLinkage.LLVMInternalLinkage)
        LLVMSetInitializer(global, LLVMConstNull(int8PtrType))
        global
    }

    private val personalityFunctionName = when (target) {
        KonanTarget.IOS_ARM32 -> "__gxx_personality_sj0"
        KonanTarget.MINGW_X64 -> "__gxx_personality_seh0"
        else -> "__gxx_personality_v0"
    }

    val cxxStdTerminate = externalNativeRuntimeFunction(
            "_ZSt9terminatev", // mangled C++ 'std::terminate'
            returnType = LlvmRetType(voidType),
            functionAttributes = listOf(LlvmFunctionAttribute.NoUnwind)
    )

    val gxxPersonalityFunction = externalNativeRuntimeFunction(
            personalityFunctionName,
            returnType = LlvmRetType(int32Type),
            functionAttributes = listOf(LlvmFunctionAttribute.NoUnwind),
            isVararg = true
    )

    val cxaBeginCatchFunction = externalNativeRuntimeFunction(
            "__cxa_begin_catch",
            returnType = LlvmRetType(int8PtrType),
            functionAttributes = listOf(LlvmFunctionAttribute.NoUnwind),
            parameterTypes = listOf(LlvmParamType(int8PtrType))
    )

    val cxaEndCatchFunction = externalNativeRuntimeFunction(
            "__cxa_end_catch",
            returnType = LlvmRetType(voidType),
            functionAttributes = listOf(LlvmFunctionAttribute.NoUnwind)
    )

    private fun getSizeOfTypeInBits(type: LLVMTypeRef): Long {
        return LLVMSizeOfTypeInBits(runtime.targetData, type)
    }

    /**
     * Width of NSInteger in bits.
     */
    val nsIntegerTypeWidth: Long by lazy {
        getSizeOfTypeInBits(Kotlin_ObjCExport_NSIntegerTypeProvider.returnType)
    }

    /**
     * Width of C long type in bits.
     */
    val longTypeWidth: Long by lazy {
        getSizeOfTypeInBits(Kotlin_longTypeProvider.returnType)
    }
}

class IrStaticInitializer(val konanLibrary: KotlinLibrary?, val initializer: LlvmCallable)
