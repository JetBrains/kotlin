/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.toKString
import llvm.*
import org.jetbrains.kotlin.backend.konan.CachedLibraries
import org.jetbrains.kotlin.library.resolver.TopologicalLibraryOrder
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.backend.konan.TargetAbiInfo
import org.jetbrains.kotlin.backend.konan.ir.llvmSymbolOrigin
import org.jetbrains.kotlin.descriptors.konan.CompiledKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.uniqueName
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.cast
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
    val context: Context

    override val runtime: Runtime
        get() = context.llvm.runtime

    val argumentAbiInfo: TargetAbiInfo
        get() = context.targetAbiInfo

    /**
     * Describes the target platform.
     *
     * TODO: using [llvmTargetData] usually results in generating non-portable bitcode.
     */
    val llvmTargetData: LLVMTargetDataRef
        get() = runtime.targetData

    val staticData: StaticData
        get() = context.llvm.staticData

    /**
     * TODO: maybe it'd be better to replace with [IrDeclaration::isEffectivelyExternal()],
     * or just drop all [else] branches of corresponding conditionals.
     */
    fun isExternal(declaration: IrDeclaration): Boolean {
        return !context.llvmModuleSpecification.containsDeclaration(declaration)
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
                    val proto = LlvmFunctionProto(this, this.computeSymbolName(), this@ContextUtils)
                    context.llvm.externalFunction(proto)
                }
            } else {
                context.llvmDeclarations.forFunctionOrNull(this)
            }
        }

    /**
     * Address of entry point of [llvmFunction].
     */
    val IrFunction.entryPointAddress: ConstPointer
        get() {
            val result = LLVMConstBitCast(this.llvmFunction.llvmValue, int8TypePtr)!!
            return constPointer(result)
        }

    val IrClass.typeInfoPtr: ConstPointer
        get() {
            return if (isExternal(this)) {
                constPointer(importGlobal(this.computeTypeInfoSymbolName(), runtime.typeInfoType,
                        origin = this.llvmSymbolOrigin))
            } else {
                context.llvmDeclarations.forClass(this).typeInfo
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

internal val String.localHash: LocalHash
    get() = LocalHash(localHash(stringAsBytes(this)))

internal val Name.localHash: LocalHash
    get() = this.toString().localHash

internal val FqName.localHash: LocalHash
    get() = this.toString().localHash

internal class InitializersGenerationState {
    val fileGlobalInitStates = mutableMapOf<IrFile, LLVMValueRef>()
    val fileThreadLocalInitStates = mutableMapOf<IrFile, AddressAccess>()

    val topLevelFields = mutableListOf<IrField>()
    val moduleThreadLocalInitializers = mutableListOf<IrFunction>()
    val moduleGlobalInitializers = mutableListOf<IrFunction>()
    var globalInitFunction: IrFunction? = null
    var globalInitState: LLVMValueRef? = null
    var threadLocalInitFunction: IrFunction? = null
    var threadLocalInitState: AddressAccess? = null

    fun reset() {
        moduleThreadLocalInitializers.clear()
        moduleGlobalInitializers.clear()
        topLevelFields.clear()
        globalInitFunction = null
        globalInitState = null
        threadLocalInitFunction = null
        threadLocalInitState = null
    }

    fun isEmpty() = topLevelFields.isEmpty() && globalInitState == null && threadLocalInitState == null
            && moduleGlobalInitializers.isEmpty() && moduleThreadLocalInitializers.isEmpty()
}

internal class Llvm(val context: Context, val llvmModule: LLVMModuleRef) : RuntimeAware {

    private fun importFunction(name: String, otherModule: LLVMModuleRef): LlvmCallable {
        if (LLVMGetNamedFunction(llvmModule, name) != null) {
            throw IllegalArgumentException("function $name already exists")
        }

        val externalFunction = LLVMGetNamedFunction(otherModule, name) ?: throw Error("function $name not found")

        val attributesCopier = LlvmFunctionAttributeProvider.copyFromExternal(externalFunction)

        val functionType = getFunctionType(externalFunction)
        val function = LLVMAddFunction(llvmModule, name, functionType)!!

        attributesCopier.addFunctionAttributes(function)

        return LlvmCallable(function, attributesCopier)
    }

    private fun importGlobal(name: String, otherModule: LLVMModuleRef): LLVMValueRef {
        if (LLVMGetNamedGlobal(llvmModule, name) != null) {
            throw IllegalArgumentException("global $name already exists")
        }

        val externalGlobal = LLVMGetNamedGlobal(otherModule, name)!!
        val globalType = getGlobalType(externalGlobal)
        val global = LLVMAddGlobal(llvmModule, globalType, name)!!

        return global
    }

    private fun importMemset(): LlvmCallable {
        val functionType = functionType(voidType, false, int8TypePtr, int8Type, int32Type, int1Type)
        return llvmIntrinsic("llvm.memset.p0i8.i32", functionType)
    }

    private fun llvmIntrinsic(name: String, type: LLVMTypeRef, vararg attributes: String): LlvmCallable {
        val result = LLVMAddFunction(llvmModule, name, type)!!
        attributes.forEach {
            val kindId = getLlvmAttributeKindId(it)
            addLlvmFunctionEnumAttribute(result, kindId)
        }
        return LlvmCallable(result, LlvmFunctionAttributeProvider.copyFromExternal(result))
    }

    internal fun externalFunction(llvmFunctionProto: LlvmFunctionProto): LlvmCallable {
        this.imports.add(llvmFunctionProto.origin, onlyBitcode = llvmFunctionProto.independent)
        val found = LLVMGetNamedFunction(llvmModule, llvmFunctionProto.name)
        if (found != null) {
            assert(getFunctionType(found) == llvmFunctionProto.llvmFunctionType) {
                "Expected: ${LLVMPrintTypeToString(llvmFunctionProto.llvmFunctionType)!!.toKString()} " +
                        "found: ${LLVMPrintTypeToString(getFunctionType(found))!!.toKString()}"
            }
            assert(LLVMGetLinkage(found) == LLVMLinkage.LLVMExternalLinkage)
            return LlvmCallable(found, llvmFunctionProto)
        } else {
            val function = addLlvmFunctionWithDefaultAttributes(context, llvmModule, llvmFunctionProto.name, llvmFunctionProto.llvmFunctionType)
            llvmFunctionProto.addFunctionAttributes(function)
            return LlvmCallable(function, llvmFunctionProto)
        }
    }

    val imports get() = context.llvmImports

    class ImportsImpl(private val context: Context) : LlvmImports {

        private val usedBitcode = mutableSetOf<KotlinLibrary>()
        private val usedNativeDependencies = mutableSetOf<KotlinLibrary>()

        private val allLibraries by lazy { context.librariesWithDependencies.toSet() }

        override fun add(origin: CompiledKlibModuleOrigin, onlyBitcode: Boolean) {
            val library = when (origin) {
                CurrentKlibModuleOrigin -> return
                is DeserializedKlibModuleOrigin -> origin.library
            }

            if (library !in allLibraries) {
                error("Library (${library.libraryName}) is used but not requested.\nRequested libraries: ${allLibraries.joinToString { it.libraryName }}")
            }

            usedBitcode.add(library)
            if (!onlyBitcode) {
                usedNativeDependencies.add(library)
            }
        }

        override fun bitcodeIsUsed(library: KonanLibrary) = library in usedBitcode

        override fun nativeDependenciesAreUsed(library: KonanLibrary) = library in usedNativeDependencies
    }

    val nativeDependenciesToLink: List<KonanLibrary> by lazy {
        context.config.resolvedLibraries
                .getFullList(TopologicalLibraryOrder)
                .filter {
                    require(it is KonanLibrary)
                    (!it.isDefault && !context.config.purgeUserLibs) || imports.nativeDependenciesAreUsed(it)
                }.cast<List<KonanLibrary>>()
    }

    private val immediateBitcodeDependencies: List<KonanLibrary> by lazy {
        context.config.resolvedLibraries.getFullList(TopologicalLibraryOrder).cast<List<KonanLibrary>>()
                .filter { (!it.isDefault && !context.config.purgeUserLibs) || imports.bitcodeIsUsed(it) }
    }

    val allCachedBitcodeDependencies: List<KonanLibrary> by lazy {
        val allLibraries = context.config.resolvedLibraries.getFullList().associateBy { it.uniqueName }
        val result = mutableSetOf<KonanLibrary>()

        fun addDependencies(cachedLibrary: CachedLibraries.Cache) {
            cachedLibrary.bitcodeDependencies.forEach {
                val library = allLibraries[it] ?: error("Bitcode dependency to an unknown library: $it")
                result.add(library as KonanLibrary)
                addDependencies(context.config.cachedLibraries.getLibraryCache(library)
                        ?: error("Library $it is expected to be cached"))
            }
        }

        for (library in immediateBitcodeDependencies) {
            val cache = context.config.cachedLibraries.getLibraryCache(library)
            if (cache != null) {
                result += library
                addDependencies(cache)
            }
        }

        result.toList()
    }

    val allNativeDependencies: List<KonanLibrary> by lazy {
        (nativeDependenciesToLink + allCachedBitcodeDependencies).distinct()
    }

    val allBitcodeDependencies: List<KonanLibrary> by lazy {
        val allNonCachedDependencies = context.librariesWithDependencies.filter {
            context.config.cachedLibraries.getLibraryCache(it) == null
        }
        val set = (allNonCachedDependencies + allCachedBitcodeDependencies).toSet()
        // This list is used in particular to build the libraries' initializers chain.
        // The initializers must be called in the topological order, so make sure that the
        // libraries list being returned is also toposorted.
        context.config.resolvedLibraries
                .getFullList(TopologicalLibraryOrder)
                .cast<List<KonanLibrary>>()
                .filter { it in set }
    }

    val bitcodeToLink: List<KonanLibrary> by lazy {
        (context.config.resolvedLibraries.getFullList(TopologicalLibraryOrder).cast<List<KonanLibrary>>())
                .filter { shouldContainBitcode(it) }
    }

    private fun shouldContainBitcode(library: KonanLibrary): Boolean {
        if (!context.llvmModuleSpecification.containsLibrary(library)) {
            return false
        }

        if (!context.llvmModuleSpecification.isFinal) {
            return true
        }

        // Apply some DCE:
        return (!library.isDefault && !context.config.purgeUserLibs) || imports.bitcodeIsUsed(library)
    }

    val additionalProducedBitcodeFiles = mutableListOf<String>()

    val staticData = StaticData(context)

    private val target = context.config.target

    val runtimeFile = context.config.distribution.runtime(target)
    override val runtime = Runtime(runtimeFile) // TODO: dispose

    val targetTriple = runtime.target

    init {
        LLVMSetDataLayout(llvmModule, runtime.dataLayout)
        LLVMSetTarget(llvmModule, targetTriple)
    }

    private fun importRtFunction(name: String) = importFunction(name, runtime.llvmModule)

    private fun importRtGlobal(name: String) = importGlobal(name, runtime.llvmModule)

    val allocInstanceFunction = importRtFunction("AllocInstance")
    val allocArrayFunction = importRtFunction("AllocArrayInstance")
    val initThreadLocalSingleton = importRtFunction("InitThreadLocalSingleton")
    val initSingletonFunction = importRtFunction("InitSingleton")
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
    val isInstanceFunction = importRtFunction("IsInstance")
    val isInstanceOfClassFastFunction = importRtFunction("IsInstanceOfClassFast")
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
    val Kotlin_ObjCExport_RethrowNSErrorAsException by lazyRtFunction
    val Kotlin_ObjCExport_AllocInstanceWithAssociatedObject by lazyRtFunction
    val Kotlin_ObjCExport_createContinuationArgument by lazyRtFunction
    val Kotlin_ObjCExport_resumeContinuation by lazyRtFunction

    private val Kotlin_ObjCExport_NSIntegerTypeProvider by lazyRtFunction
    private val Kotlin_longTypeProvider by lazyRtFunction

    val Kotlin_mm_safePointFunctionPrologue by lazyRtFunction
    val Kotlin_mm_safePointWhileLoopBody by lazyRtFunction

    val tlsMode by lazy {
        when (target) {
            KonanTarget.WASM32,
            is KonanTarget.ZEPHYR -> LLVMThreadLocalMode.LLVMNotThreadLocal
            else -> LLVMThreadLocalMode.LLVMGeneralDynamicTLSModel
        }
    }

    var tlsCount = 0

    val tlsKey by lazy {
        val global = LLVMAddGlobal(llvmModule, kInt8Ptr, "__KonanTlsKey")!!
        LLVMSetLinkage(global, LLVMLinkage.LLVMInternalLinkage)
        LLVMSetInitializer(global, LLVMConstNull(kInt8Ptr))
        global
    }

    private val personalityFunctionName = when (target) {
        KonanTarget.IOS_ARM32 -> "__gxx_personality_sj0"
        KonanTarget.MINGW_X64 -> "__gxx_personality_seh0"
        else -> "__gxx_personality_v0"
    }

    val cxxStdTerminate = externalFunction(LlvmFunctionProto(
            "_ZSt9terminatev", // mangled C++ 'std::terminate'
            returnType = LlvmRetType(voidType),
            functionAttributes = listOf(LlvmFunctionAttribute.NoUnwind),
            origin = context.standardLlvmSymbolsOrigin
    ))

    val gxxPersonalityFunction = externalFunction(LlvmFunctionProto(
            personalityFunctionName,
            returnType = LlvmRetType(int32Type),
            functionAttributes = listOf(LlvmFunctionAttribute.NoUnwind),
            isVararg = true,
            origin = context.standardLlvmSymbolsOrigin
    ))

    val cxaBeginCatchFunction = externalFunction(LlvmFunctionProto(
            "__cxa_begin_catch",
            returnType = LlvmRetType(int8TypePtr),
            functionAttributes = listOf(LlvmFunctionAttribute.NoUnwind),
            parameterTypes = listOf(LlvmParamType(int8TypePtr)),
            origin = context.standardLlvmSymbolsOrigin
    ))

    val cxaEndCatchFunction = externalFunction(LlvmFunctionProto(
            "__cxa_end_catch",
            returnType = LlvmRetType(voidType),
            functionAttributes = listOf(LlvmFunctionAttribute.NoUnwind),
            origin = context.standardLlvmSymbolsOrigin
    ))

    val memsetFunction = importMemset()
    //val memcpyFunction = importMemcpy()

    val llvmTrap = llvmIntrinsic(
            "llvm.trap",
            functionType(voidType, false),
            "cold", "noreturn", "nounwind"
    )

    val llvmEhTypeidFor = llvmIntrinsic(
            "llvm.eh.typeid.for",
            functionType(int32Type, false, int8TypePtr),
            "nounwind", "readnone"
    )

    val usedFunctions = mutableListOf<LLVMValueRef>()
    val usedGlobals = mutableListOf<LLVMValueRef>()
    val compilerUsedGlobals = mutableListOf<LLVMValueRef>()
    val irStaticInitializers = mutableListOf<IrStaticInitializer>()
    val otherStaticInitializers = mutableListOf<LLVMValueRef>()
    var fileUsesThreadLocalObjects = false
    val globalSharedObjects = mutableSetOf<LLVMValueRef>()
    val initializersGenerationState = InitializersGenerationState()

    private object lazyRtFunction {
        operator fun provideDelegate(
                thisRef: Llvm, property: KProperty<*>
        ) = object : ReadOnlyProperty<Llvm, LlvmCallable> {

            val value: LlvmCallable by lazy { thisRef.importRtFunction(property.name) }

            override fun getValue(thisRef: Llvm, property: KProperty<*>): LlvmCallable = value
        }
    }

    val llvmInt1 = int1Type
    val llvmInt8 = int8Type
    val llvmInt16 = int16Type
    val llvmInt32 = int32Type
    val llvmInt64 = int64Type
    val llvmFloat = floatType
    val llvmDouble = doubleType
    val llvmVector128 = vector128Type

    private fun getSizeOfReturnTypeInBits(functionPointer: LLVMValueRef): Long {
        // LLVMGetElementType is called because we need to dereference a pointer to function.
        val nsIntegerType = LLVMGetReturnType(LLVMGetElementType(functionPointer.type))
        return LLVMSizeOfTypeInBits(runtime.targetData, nsIntegerType)
    }

    /**
     * Width of NSInteger in bits.
     */
    val nsIntegerTypeWidth: Long by lazy {
        getSizeOfReturnTypeInBits(Kotlin_ObjCExport_NSIntegerTypeProvider.llvmValue)
    }

    /**
     * Width of C long type in bits.
     */
    val longTypeWidth: Long by lazy {
        getSizeOfReturnTypeInBits(Kotlin_longTypeProvider.llvmValue)
    }
}

class IrStaticInitializer(val konanLibrary: KotlinLibrary?, val initializer: LLVMValueRef)
