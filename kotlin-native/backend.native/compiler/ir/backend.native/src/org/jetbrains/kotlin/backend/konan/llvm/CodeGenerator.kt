/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.llvm


import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.NativeGenerationState
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.binaryTypeIsReference
import org.jetbrains.kotlin.backend.konan.cgen.CBridgeOrigin
import org.jetbrains.kotlin.backend.konan.ir.ClassGlobalHierarchyInfo
import org.jetbrains.kotlin.backend.konan.ir.isAbstract
import org.jetbrains.kotlin.backend.konan.ir.isAny
import org.jetbrains.kotlin.backend.konan.llvm.ThreadState.Native
import org.jetbrains.kotlin.backend.konan.llvm.ThreadState.Runnable
import org.jetbrains.kotlin.backend.konan.llvm.objc.ObjCDataGenerator
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.objcinterop.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.konan.ForeignExceptionMode
import org.jetbrains.kotlin.konan.target.CompilerOutputKind


internal class CodeGenerator(override val generationState: NativeGenerationState) : ContextUtils {
    fun addFunction(proto: LlvmFunctionProto): LlvmCallable =
            proto.createLlvmFunction(context, llvm.module)

    fun llvmFunction(function: IrSimpleFunction): LlvmCallable =
            llvmFunctionOrNull(function)
                    ?: error("no function ${function.name} in ${function.file.packageFqName}")

    fun llvmFunctionOrNull(function: IrSimpleFunction): LlvmCallable? =
            function.llvmFunctionOrNull

    val llvmDeclarations = generationState.llvmDeclarations
    val intPtrType = LLVMIntPtrTypeInContext(llvm.llvmContext, llvmTargetData)!!
    internal val immOneIntPtrType = LLVMConstInt(intPtrType, 1, 1)!!
    internal val immThreeIntPtrType = LLVMConstInt(intPtrType, 3, 1)!!
    // Keep in sync with OBJECT_TAG_MASK in C++.
    internal val immTypeInfoMask = LLVMConstNot(LLVMConstInt(intPtrType, 3, 0)!!)!!

    //-------------------------------------------------------------------------//

    fun typeInfoValue(irClass: IrClass): LLVMValueRef = irClass.llvmTypeInfoPtr

    fun param(fn: IrSimpleFunction, i: Int) = fn.llvmFunction.param(i)

    fun functionEntryPointAddress(function: IrSimpleFunction) = function.entryPointAddress.llvm

    fun typeInfoForAllocation(constructedClass: IrClass): LLVMValueRef {
        require(!constructedClass.isObjCClass()) { "Allocation of Obj-C class ${constructedClass.render()} should have been lowered" }
        require(!constructedClass.isAbstract()) { "Allocation of abstract class ${constructedClass.render()} is not allowed" }
        return typeInfoValue(constructedClass)
    }

    fun generateLocationInfo(locationInfo: LocationInfo): DILocationRef? = if (locationInfo.inlinedAt != null)
        LLVMCreateLocationInlinedAt(LLVMGetModuleContext(llvm.module), locationInfo.line, locationInfo.column,
                locationInfo.scope, generateLocationInfo(locationInfo.inlinedAt))
    else
        LLVMCreateLocation(LLVMGetModuleContext(llvm.module), locationInfo.line, locationInfo.column, locationInfo.scope)

    val objCDataGenerator = when {
        context.config.target.family.isAppleFamily -> ObjCDataGenerator(this)
        else -> null
    }

}

internal sealed class ExceptionHandler {
    object None : ExceptionHandler()
    object Caller : ExceptionHandler()
    abstract class Local : ExceptionHandler() {
        abstract val unwind: LLVMBasicBlockRef
    }

    open fun genThrow(
            functionGenerationContext: FunctionGenerationContext,
            kotlinException: LLVMValueRef
    ): Unit = with(functionGenerationContext) {
        call(
                llvm.throwExceptionFunction,
                listOf(kotlinException),
                Lifetime.IRRELEVANT,
                this@ExceptionHandler
        )
        unreachable()
    }
}

internal enum class ThreadState {
    Native, Runnable
}

val LLVMValueRef.name:String?
    get() = LLVMGetValueName(this)?.toKString()

val LLVMValueRef.isConst:Boolean
    get() = (LLVMIsConstant(this) == 1)


internal inline fun generateFunction(
        codegen: CodeGenerator,
        function: IrSimpleFunction,
        startLocation: LocationInfo?,
        endLocation: LocationInfo?,
        code: FunctionGenerationContext.() -> Unit
) {
    val llvmFunction = codegen.llvmFunction(function)

    val isCToKotlinBridge = function.origin == CBridgeOrigin.C_TO_KOTLIN_BRIDGE
            // TODO: Alternative approach: lowering that changes origin of such functions to C_TO_KOTLIN_BRIDGE?
            || function.hasAnnotation(RuntimeNames.exportedBridge)

    val functionGenerationContext = DefaultFunctionGenerationContext(
            llvmFunction,
            codegen,
            startLocation,
            endLocation,
            switchToRunnable = isCToKotlinBridge,
            needSafePoint = true,
            function)
    functionGenerationContext.needsRuntimeInit = isCToKotlinBridge

    try {
        generateFunctionBody(functionGenerationContext, code)
    } finally {
        functionGenerationContext.dispose()
    }
}

internal inline fun <T : FunctionGenerationContext> FunctionGenerationContextBuilder<T>.generate(code: T.() -> Unit): LlvmCallable {
    val functionGenerationContext = this.build()
    return try {
        generateFunctionBody(functionGenerationContext, code)
        functionGenerationContext.function
    } finally {
        functionGenerationContext.dispose()
    }
}

internal inline fun generateFunction(
        codegen: CodeGenerator,
        functionProto: LlvmFunctionProto,
        startLocation: LocationInfo? = null,
        endLocation: LocationInfo? = null,
        switchToRunnable: Boolean = false,
        needSafePoint: Boolean = true,
        code: FunctionGenerationContext.() -> Unit
) : LlvmCallable {
    val function = codegen.addFunction(functionProto)
    val functionGenerationContext = DefaultFunctionGenerationContext(
            function,
            codegen,
            startLocation,
            endLocation,
            switchToRunnable = switchToRunnable,
            needSafePoint = needSafePoint
    )
    try {
        generateFunctionBody(functionGenerationContext, code)
    } finally {
        functionGenerationContext.dispose()
    }
    return function
}

// TODO: Consider using different abstraction than `FunctionGenerationContext` for `generateFunctionNoRuntime`.
internal inline fun generateFunctionNoRuntime(
        codegen: CodeGenerator,
        functionProto: LlvmFunctionProto,
        code: FunctionGenerationContext.() -> Unit,
) : LlvmCallable {
    val function = codegen.addFunction(functionProto)
    val functionGenerationContext = DefaultFunctionGenerationContext(function, codegen, null, null,
            switchToRunnable = false, needSafePoint = true)
    try {
        functionGenerationContext.forbidRuntime = true
        require(!functionProto.signature.returnsObjectType) {
            "Cannot return object from function without Kotlin runtime"
        }

        generateFunctionBody(functionGenerationContext, code)
    } finally {
        functionGenerationContext.dispose()
    }
    return function
}

private inline fun <T : FunctionGenerationContext> generateFunctionBody(
        functionGenerationContext: T,
        code: T.() -> Unit) {
    functionGenerationContext.prologue()
    functionGenerationContext.code()
    if (!functionGenerationContext.isAfterTerminator())
        functionGenerationContext.unreachable()
    functionGenerationContext.epilogue()
    functionGenerationContext.resetDebugLocation()
}

internal object VirtualTablesLookup {
    private fun FunctionGenerationContext.getInterfaceTableRecord(typeInfo: LLVMValueRef, interfaceId: Int): LLVMValueRef {
        val interfaceTableRecordPtrType = pointerType(runtime.interfaceTableRecordType)
        val interfaceTableSize = load(llvm.int32Type, structGep(runtime.typeInfoType, typeInfo, 9 /* interfaceTableSize_ */))
        val interfaceTable = load(interfaceTableRecordPtrType, structGep(runtime.typeInfoType, typeInfo, 10 /* interfaceTable_ */))

        fun fastPath(): LLVMValueRef {
            // The fastest optimistic version.
            val interfaceTableIndex = and(interfaceTableSize, llvm.int32(interfaceId))
            return gep(runtime.interfaceTableRecordType, interfaceTable, interfaceTableIndex)
        }

        // See details in ClassLayoutBuilder.
        return if (context.ghaEnabled()
                && context.globalHierarchyAnalysisResult.bitsPerColor <= ClassGlobalHierarchyInfo.MAX_BITS_PER_COLOR
                && context.config.produce != CompilerOutputKind.FRAMEWORK
        ) {
            // All interface tables are small and no unknown interface inheritance.
            fastPath()
        } else {
            val startLocationInfo = position()?.start
            val fastPathBB = basicBlock("fast_path", startLocationInfo)
            val slowPathBB = basicBlock("slow_path", startLocationInfo)
            val takeResBB = basicBlock("take_res", startLocationInfo)
            condBr(icmpGe(interfaceTableSize, llvm.kImmInt32Zero), fastPathBB, slowPathBB)
            positionAtEnd(takeResBB)
            val resultPhi = phi(interfaceTableRecordPtrType)
            appendingTo(fastPathBB) {
                val fastValue = fastPath()
                br(takeResBB)
                addPhiIncoming(resultPhi, currentBlock to fastValue)
            }
            appendingTo(slowPathBB) {
                val actualInterfaceTableSize = sub(llvm.kImmInt32Zero, interfaceTableSize) // -interfaceTableSize
                val slowValue = call(llvm.lookupInterfaceTableRecord,
                        listOf(interfaceTable, actualInterfaceTableSize, llvm.int32(interfaceId)))
                br(takeResBB)
                addPhiIncoming(resultPhi, currentBlock to slowValue)
            }
            resultPhi
        }
    }

    fun FunctionGenerationContext.checkIsSubtype(objTypeInfo: LLVMValueRef, dstClass: IrClass) = if (!context.ghaEnabled()) {
        call(llvm.isSubtypeFunction, listOf(objTypeInfo, codegen.typeInfoValue(dstClass)))
    } else {
        val dstHierarchyInfo = context.getLayoutBuilder(dstClass).hierarchyInfo
        if (!dstClass.isInterface) {
            call(llvm.isSubclassFastFunction,
                    listOf(objTypeInfo, llvm.int32(dstHierarchyInfo.classIdLo), llvm.int32(dstHierarchyInfo.classIdHi)))
        } else {
            // Essentially: typeInfo.itable[place(interfaceId)].id == interfaceId
            val interfaceId = dstHierarchyInfo.interfaceId
            val interfaceTableRecord = getInterfaceTableRecord(objTypeInfo, interfaceId)
            icmpEq(load(llvm.int32Type, structGep(runtime.interfaceTableRecordType, interfaceTableRecord, 0 /* id */)), llvm.int32(interfaceId))
        }
    }

    fun FunctionGenerationContext.getVirtualImpl(receiver: LLVMValueRef, irFunction: IrSimpleFunction): LlvmCallable {
        assert(LLVMTypeOf(receiver) == codegen.kObjHeaderPtr)

        val typeInfoPtr: LLVMValueRef = if (irFunction.getObjCMethodInfo() != null)
            call(llvm.getObjCKotlinTypeInfo, listOf(receiver))
        else
            loadTypeInfo(receiver)

        assert(typeInfoPtr.type == codegen.kTypeInfoPtr) { llvmtype2string(typeInfoPtr.type) }

        val owner = irFunction.parentAsClass
        val canCallViaVtable = !owner.isInterface
        val layoutBuilder = generationState.context.getLayoutBuilder(owner)

        val llvmFunctionSignature = LlvmFunctionSignature(irFunction, this)
        val functionType = llvmFunctionSignature.llvmFunctionType
        val functionPtrType = pointerType(functionType)
        val functionPtrPtrType = pointerType(functionPtrType)
        val llvmMethod = when {
            canCallViaVtable -> {
                val index = layoutBuilder.vtableIndex(irFunction)
                val vtablePlace = gep(runtime.typeInfoType, typeInfoPtr, llvm.int32(1)) // typeInfoPtr + 1
                val vtable = bitcast(llvm.int8PtrPtrType, vtablePlace)
                val slot = gep(llvm.int8PtrType, vtable, llvm.int32(index))
                load(functionPtrType, bitcast(functionPtrPtrType, slot))
            }

            else -> {
                // Essentially: typeInfo.itable[place(interfaceId)].vtable[method]
                val itablePlace = layoutBuilder.itablePlace(irFunction)
                val interfaceTableRecord = getInterfaceTableRecord(typeInfoPtr, itablePlace.interfaceId)
                val vtable = load(llvm.int8PtrPtrType, structGep(runtime.interfaceTableRecordType, interfaceTableRecord, 2 /* vtable */))
                val slot = gep(llvm.int8PtrType, vtable, llvm.int32(itablePlace.methodIndex))
                load(functionPtrType, bitcast(functionPtrPtrType, slot))
            }
        }
        return LlvmCallable(
                bitcast(functionPtrType, llvmMethod),
                llvmFunctionSignature
        )
    }
}

internal fun IrSimpleFunction.findOverriddenMethodOfAny() =
    resolveFakeOverride().takeIf { it?.parentClassOrNull?.isAny() == true }

/*
 * Special trampoline function to call actual virtual implementation. This helps with reducing
 * dependence between klibs/files (if vtable/itable of some class has changed, the call sites
 * would be the same and wouldn't need recompiling).
 */
internal fun CodeGenerator.getVirtualFunctionTrampoline(irFunction: IrSimpleFunction): LlvmCallable {
    /*
     * Resolve owner of the call with special handling of Any methods:
     * if toString/eq/hc is invoked on an interface instance, we resolve
     * owner as Any and dispatch it via vtable.
     * Note: Keep on par with DFG builder.
     */
    val anyMethod = irFunction.findOverriddenMethodOfAny()

    return getVirtualFunctionTrampolineImpl(anyMethod ?: irFunction)
}

private fun CodeGenerator.getVirtualFunctionTrampolineImpl(irFunction: IrSimpleFunction) =
        generationState.virtualFunctionTrampolines.getOrPut(irFunction) {
            val targetName = if (irFunction.isExported())
                irFunction.computeSymbolName()
            else
                irFunction.computePrivateSymbolName(irFunction.parentAsClass.fqNameForIrSerialization.asString())
            val proto = LlvmFunctionProto(
                    name = "$targetName-trampoline",
                    signature = LlvmFunctionSignature(irFunction, this),
                    origin = null,
                    linkage = linkageOf(irFunction)
            )
            if (isExternal(irFunction))
                llvm.externalFunction(proto)
            else {
                val offset = irFunction.startOffset.takeIf { it != UNDEFINED_OFFSET }
                        ?: irFunction.parentAsClass.startOffset.takeIf { it != UNDEFINED_OFFSET }
                val fileEntry = irFunction.fileOrNull?.fileEntry.takeIf {
                    offset != null && context.shouldContainLocationDebugInfo()
                }
                val diFunctionScope = fileEntry?.let {
                    with(generationState.debugInfo) {
                        irFunction.diFunctionScope(
                                it,
                                proto.name,
                                it.line(offset!!),
                                false,
                                isTransparentStepping = generationState.config.enableDebugTransparentStepping
                        )
                    }
                }
                @Suppress("UNCHECKED_CAST") val location = diFunctionScope?.let {
                    val (line, column) = fileEntry.lineAndColumn(offset!!)
                    LocationInfo(it as DIScopeOpaqueRef, line, column)
                }
                generateFunction(this, proto, needSafePoint = false, startLocation = location, endLocation = location) {
                    val args = proto.signature.parameterTypes.indices.map { param(it) }
                    val receiver = param(0)
                    val callee = with(VirtualTablesLookup) { getVirtualImpl(receiver, irFunction) }
                    val result = call(callee, args, exceptionHandler = ExceptionHandler.Caller, verbatim = true)
                    ret(result)
                }.also { llvmFunction ->
                    diFunctionScope?.let { llvmFunction.addDebugInfoSubprogram(it) }
                }
            }
        }

/**
 * There're cases when we don't need end position or it is meaningless.
 */
internal data class LocationInfoRange(var start: LocationInfo, var end: LocationInfo?)

internal interface StackLocalsManager {
    fun alloc(irClass: IrClass): LLVMValueRef

    fun allocArray(irClass: IrClass, count: LLVMValueRef): LLVMValueRef

    fun clean(refsOnly: Boolean)

    fun enterScope()
    fun exitScope()
}

internal class StackLocalsManagerImpl(
        val functionGenerationContext: FunctionGenerationContext,
        val bbInitStackLocals: LLVMBasicBlockRef
) : StackLocalsManager {
    private var scopeDepth = 0
    override fun enterScope() { scopeDepth++ }
    override fun exitScope() { scopeDepth-- }
    private fun isRootScope() = scopeDepth == 0

    private class StackLocal(
            val arraySize: Int?,
            val irClass: IrClass,
            val stackAllocationPtr: LLVMValueRef,
            val objHeaderPtr: LLVMValueRef,
            val gcRootSetSlot: LLVMValueRef?
    ) {
        val isArray
            get() = arraySize != null
    }

    private val stackLocals = mutableListOf<StackLocal>()

    fun isEmpty() = stackLocals.isEmpty()

    private fun FunctionGenerationContext.createRootSetSlot() =
            alloca(kObjHeaderPtr, true)

    override fun alloc(irClass: IrClass): LLVMValueRef = with(functionGenerationContext) {
        val classInfo = llvmDeclarations.forClass(irClass)
        val type = classInfo.bodyType.llvmBodyType
        val stackLocal = appendingTo(bbInitStackLocals) {
            val stackSlot = LLVMBuildAlloca(builder, type, "")!!
            LLVMSetAlignment(stackSlot, classInfo.alignment)

            memset(bitcast(llvm.int8PtrType, stackSlot), 0, LLVMSizeOfTypeInBits(codegen.llvmTargetData, type).toInt() / 8)

            val objectHeader = structGep(type, stackSlot, 0, "objHeader")
            val typeInfo = codegen.typeInfoForAllocation(irClass)
            setTypeInfoForLocalObject(runtime.objHeaderType, objectHeader, typeInfo)
            val gcRootSetSlot = createRootSetSlot()
            StackLocal(null, irClass, stackSlot, objectHeader, gcRootSetSlot)
        }

        stackLocals += stackLocal
        if (!isRootScope()) {
            clean(stackLocal, false)
        }
        if (stackLocal.gcRootSetSlot != null) {
            storeStackRef(stackLocal.objHeaderPtr, stackLocal.gcRootSetSlot)
        }
        stackLocal.objHeaderPtr
    }

    // Returns generated special type for local array.
    // It's needed to prevent changing variables order on stack.
    private fun localArrayType(irClass: IrClass, count: Int) = with(functionGenerationContext) {
        val name = "local#${irClass.name}${count}#internal"
        // Create new type or get already created.
        context.declaredLocalArrays.getOrPut(name) {
            val fieldTypes = listOf(kArrayHeader, LLVMArrayType(arrayToElementType[irClass.symbol]!!, count))
            val classType = LLVMStructCreateNamed(LLVMGetModuleContext(llvm.module), name)!!
            LLVMStructSetBody(classType, fieldTypes.toCValues(), fieldTypes.size, 1)
            classType
        }
    }

    private val symbols = functionGenerationContext.context.ir.symbols
    private val llvm = functionGenerationContext.llvm

    // TODO: find better place?
    private val arrayToElementType = mapOf(
            symbols.array to functionGenerationContext.kObjHeaderPtr,
            symbols.byteArray to llvm.int8Type,
            symbols.charArray to llvm.int16Type,
            symbols.string to llvm.int16Type,
            symbols.shortArray to llvm.int16Type,
            symbols.intArray to llvm.int32Type,
            symbols.longArray to llvm.int64Type,
            symbols.floatArray to llvm.floatType,
            symbols.doubleArray to llvm.doubleType,
            symbols.booleanArray to llvm.int8Type
    )

    override fun allocArray(irClass: IrClass, count: LLVMValueRef) = with(functionGenerationContext) {
        val stackLocal = appendingTo(bbInitStackLocals) {
            val constCount = extractConstUnsignedInt(count).toInt()
            val arrayType = localArrayType(irClass, constCount)
            val typeInfo = codegen.typeInfoValue(irClass)
            val arraySlot = LLVMBuildAlloca(builder, arrayType, "")!!
            // Set array size in ArrayHeader.
            val arrayHeaderSlot = structGep(arrayType, arraySlot, 0, "arrayHeader")
            setTypeInfoForLocalObject(runtime.arrayHeaderType, arrayHeaderSlot, typeInfo)
            val sizeField = structGep(runtime.arrayHeaderType, arrayHeaderSlot, 1, "count_")
            store(count, sizeField)

            memset(bitcast(llvm.int8PtrType, structGep(arrayType, arraySlot, 1, "arrayBody")),
                    0,
                    constCount * LLVMSizeOfTypeInBits(codegen.llvmTargetData, arrayToElementType[irClass.symbol]).toInt() / 8
            )
            val gcRootSetSlot = createRootSetSlot()
            StackLocal(constCount, irClass, arraySlot, arrayHeaderSlot, gcRootSetSlot)
        }

        stackLocals += stackLocal
        val result = bitcast(kObjHeaderPtr, stackLocal.objHeaderPtr)
        if (!isRootScope()) {
            clean(stackLocal, false)
        }
        if (stackLocal.gcRootSetSlot != null) {
            storeStackRef(result, stackLocal.gcRootSetSlot)
        }
        result
    }

    override fun clean(refsOnly: Boolean) = stackLocals.forEach { clean(it, refsOnly) }

    private fun clean(stackLocal: StackLocal, refsOnly: Boolean) = with(functionGenerationContext) {
        if (stackLocal.isArray) {
            if (stackLocal.irClass.symbol == context.ir.symbols.array) {
                call(llvm.zeroArrayRefsFunction, listOf(stackLocal.objHeaderPtr))
            } else if (!refsOnly) {
                val arrayType = localArrayType(stackLocal.irClass, stackLocal.arraySize!!)
                memset(bitcast(llvm.int8PtrType, structGep(arrayType, stackLocal.stackAllocationPtr, 1, "arrayBody")),
                        0,
                        stackLocal.arraySize * LLVMSizeOfTypeInBits(codegen.llvmTargetData, arrayToElementType[stackLocal.irClass.symbol]).toInt() / 8
                )
            }
        } else {
            val info = llvmDeclarations.forClass(stackLocal.irClass)
            val type = info.bodyType.llvmBodyType
            for ((fieldSymbol, fieldIndex) in info.fieldIndices.entries.sortedBy{ e -> e.value }) {

                if (fieldSymbol.owner.type.binaryTypeIsReference()) {
                    val fieldPtr = structGep(type, stackLocal.stackAllocationPtr, fieldIndex, "")
                    if (refsOnly)
                        storeHeapRef(kNullObjHeaderPtr, fieldPtr)
                    else
                        call(llvm.zeroHeapRefFunction, listOf(fieldPtr))
                }
            }

            if (!refsOnly) {
                val bodyPtr = ptrToInt(stackLocal.stackAllocationPtr, codegen.intPtrType)
                val bodySize = LLVMSizeOfTypeInBits(codegen.llvmTargetData, type).toInt() / 8
                val serviceInfoSize = runtime.pointerSize
                val serviceInfoSizeLlvm = LLVMConstInt(codegen.intPtrType, serviceInfoSize.toLong(), 1)!!
                val bodyWithSkippedServiceInfoPtr = intToPtr(add(bodyPtr, serviceInfoSizeLlvm), llvm.int8PtrType)
                memset(bodyWithSkippedServiceInfoPtr, 0, bodySize - serviceInfoSize)
            }
        }
        if (stackLocal.gcRootSetSlot != null) {
            storeStackRef(kNullObjHeaderPtr, stackLocal.gcRootSetSlot)
        }
    }

    private fun setTypeInfoForLocalObject(headerType: LLVMTypeRef, header: LLVMValueRef, typeInfoPointer: LLVMValueRef) = with(functionGenerationContext) {
        val typeInfo = structGep(headerType, header, 0, "typeInfoOrMeta_")
        // Set tag OBJECT_TAG_PERMANENT_CONTAINER | OBJECT_TAG_NONTRIVIAL_CONTAINER.
        val typeInfoValue = intToPtr(or(ptrToInt(typeInfoPointer, codegen.intPtrType),
                codegen.immThreeIntPtrType), kTypeInfoPtr)
        store(typeInfoValue, typeInfo)
    }
}

internal abstract class FunctionGenerationContextBuilder<T : FunctionGenerationContext>(
        val function: LlvmCallable,
        val codegen: CodeGenerator
) {
    constructor(functionProto: LlvmFunctionProto, codegen: CodeGenerator) :
            this(
                    codegen.addFunction(functionProto),
                    codegen
            )

    var startLocation: LocationInfo? = null
    var endLocation: LocationInfo? = null
    var switchToRunnable = false
    var needSafePoint = true
    var irFunction: IrSimpleFunction? = null

    abstract fun build(): T
}

internal abstract class FunctionGenerationContext(
        val function: LlvmCallable,
        val codegen: CodeGenerator,
        private val startLocation: LocationInfo?,
        protected val endLocation: LocationInfo?,
        private val switchToRunnable: Boolean,
        private val needSafePoint: Boolean,
        internal val irFunction: IrSimpleFunction? = null
) : ContextUtils {

    constructor(builder: FunctionGenerationContextBuilder<*>) : this(
            function = builder.function,
            codegen = builder.codegen,
            startLocation = builder.startLocation,
            endLocation = builder.endLocation,
            switchToRunnable = builder.switchToRunnable,
            needSafePoint = builder.needSafePoint,
            irFunction = builder.irFunction
    )

    override val generationState = codegen.generationState
    val llvmDeclarations = generationState.llvmDeclarations
    val vars = VariableManager(this)
    private val basicBlockToLastLocation = mutableMapOf<LLVMBasicBlockRef, LocationInfoRange>()

    private fun update(block: LLVMBasicBlockRef, startLocationInfo: LocationInfo?, endLocation: LocationInfo? = startLocationInfo) {
        startLocationInfo ?: return
        basicBlockToLastLocation.put(block, LocationInfoRange(startLocationInfo, endLocation))
    }

    var returnType: LLVMTypeRef? = function.returnType
    var returnSlot: LLVMValueRef? = null
        private set
    private var slotsPhi: LLVMValueRef? = null
    private val frameOverlaySlotCount =
            (LLVMStoreSizeOfType(llvmTargetData, runtime.frameOverlayType) / runtime.pointerSize).toInt()
    private var slotCount = frameOverlaySlotCount
    private var localAllocs = 0
    // TODO: remove if exactly unused.
    //private var arenaSlot: LLVMValueRef? = null
    private val slotToVariableLocation = mutableMapOf<Int, VariableDebugLocation>()

    private val prologueBb = basicBlockInFunction("prologue", null)
    private val localsInitBb = basicBlockInFunction("locals_init", null)
    private val stackLocalsInitBb = basicBlockInFunction("stack_locals_init", null)
    private val entryBb = basicBlockInFunction("entry", startLocation)
    protected val cleanupLandingpad = basicBlockInFunction("cleanup_landingpad", endLocation)

    // Functions that can be exported and called not only from Kotlin code should have cleanup_landingpad and `LeaveFrame`
    // because there is no guarantee of catching Kotlin exception in Kotlin code.
    protected open val needCleanupLandingpadAndLeaveFrame: Boolean
        get() = irFunction?.annotations?.hasAnnotation(RuntimeNames.exportForCppRuntime) == true || switchToRunnable

    private var setCurrentFrameIsCalled: Boolean = false

    val stackLocalsManager = StackLocalsManagerImpl(this, stackLocalsInitBb)

    data class FunctionInvokeInformation(
            val invokeInstruction: LLVMValueRef,
            val llvmFunction: LlvmCallable,
            val args: List<LLVMValueRef>,
            val success: LLVMBasicBlockRef,
    )

    private val invokeInstructions = mutableListOf<FunctionInvokeInformation>()

    // Whether the generating function needs to initialize Kotlin runtime before execution. Useful for interop bridges,
    // for example.
    var needsRuntimeInit = false

    // Marks that function is not allowed to call into Kotlin runtime. For this function no safepoints, no enter/leave
    // frames are generated.
    // TODO: Should forbid all calls into runtime except for explicitly allowed. Also should impose the same restriction
    //       on function being called from this one.
    // TODO: Consider using a different abstraction than `FunctionGenerationContext`.
    var forbidRuntime = false

    fun dispose() {
        currentPositionHolder.dispose()
    }

    protected fun basicBlockInFunction(name: String, locationInfo: LocationInfo?): LLVMBasicBlockRef {
        val bb = function.addBasicBlock(llvm.llvmContext, name)
        update(bb, locationInfo)
        return bb
    }

    fun basicBlock(name: String = "label_", startLocationInfo: LocationInfo?, endLocationInfo: LocationInfo? = startLocationInfo): LLVMBasicBlockRef {
        val result = LLVMInsertBasicBlockInContext(llvm.llvmContext, this.currentBlock, name)!!
        update(result, startLocationInfo, endLocationInfo)
        LLVMMoveBasicBlockAfter(result, this.currentBlock)
        return result
    }

    /**
     *  This function shouldn't be used normally.
     *  It is used to move block with strange debug info in the middle of function, to avoid last debug info being too strange,
     *  because it will break heuristics in CoreSymbolication
     */
    fun moveBlockAfterEntry(block: LLVMBasicBlockRef) {
        LLVMMoveBasicBlockAfter(block, this.entryBb)
    }

    fun alloca(type: LLVMTypeRef?, isObjectType: Boolean, name: String = "", variableLocation: VariableDebugLocation? = null): LLVMValueRef {
        if (isObjectType) {
            appendingTo(localsInitBb) {
                val slotAddress = gep(type!!, slotsPhi!!, llvm.int32(slotCount), name)
                variableLocation?.let {
                    slotToVariableLocation[slotCount] = it
                }
                slotCount++
                return slotAddress
            }
        }

        appendingTo(prologueBb) {
            val slotAddress = LLVMBuildAlloca(builder, type, name)!!
            variableLocation?.let {
                DIInsertDeclaration(
                        builder = generationState.debugInfo.builder,
                        value = slotAddress,
                        localVariable = it.localVariable,
                        location = it.location,
                        bb = prologueBb,
                        expr = null,
                        exprCount = 0)
            }
            return slotAddress
        }
    }


    abstract fun ret(value: LLVMValueRef?): LLVMValueRef

    fun param(index: Int): LLVMValueRef = function.param(index)

    private fun applyMemoryOrderAndAlignment(value: LLVMValueRef, memoryOrder: LLVMAtomicOrdering?, alignment: Int?): LLVMValueRef {
        memoryOrder?.let { LLVMSetOrdering(value, it) }
        alignment?.let { LLVMSetAlignment(value, it) }
        return value
    }

    fun load(type: LLVMTypeRef, address: LLVMValueRef, name: String = "",
             memoryOrder: LLVMAtomicOrdering? = null, alignment: Int? = null
    ): LLVMValueRef {
        return applyMemoryOrderAndAlignment(LLVMBuildLoad2(builder, type, address, name)!!, memoryOrder, alignment)
    }

    fun loadSlot(
            type: LLVMTypeRef,
            isObjectType: Boolean,
            address: LLVMValueRef,
            isVar: Boolean,
            resultSlot: LLVMValueRef? = null,
            name: String = "",
            memoryOrder: LLVMAtomicOrdering? = null,
            alignment: Int? = null
    ): LLVMValueRef {
        val value = LLVMBuildLoad2(builder, type, address, name)!!
        memoryOrder?.let { LLVMSetOrdering(value, it) }
        alignment?.let { LLVMSetAlignment(value, it) }
        if (isObjectType && isVar) {
            val slot = resultSlot ?: alloca(type, isObjectType, variableLocation = null)
            storeStackRef(value, slot)
        }
        return value
    }

    fun store(value: LLVMValueRef, ptr: LLVMValueRef, memoryOrder: LLVMAtomicOrdering? = null, alignment: Int? = null) {
        val store = LLVMBuildStore(builder, value, ptr)
        memoryOrder?.let { LLVMSetOrdering(store, it) }
        alignment?.let { LLVMSetAlignment(store, it) }
    }

    fun storeHeapRef(value: LLVMValueRef, ptr: LLVMValueRef) {
        updateRef(value, ptr, onStack = false)
    }

    fun storeStackRef(value: LLVMValueRef, ptr: LLVMValueRef) {
        updateRef(value, ptr, onStack = true)
    }

    fun storeAny(value: LLVMValueRef, ptr: LLVMValueRef, isObjectRef: Boolean, onStack: Boolean, isVolatile: Boolean = false, alignment: Int? = null) {
        when {
            isObjectRef -> updateRef(value, ptr, onStack, isVolatile, alignment)
            else -> store(value, ptr, if (isVolatile) LLVMAtomicOrdering.LLVMAtomicOrderingSequentiallyConsistent else null, alignment)
        }
    }

    private fun updateReturnRef(value: LLVMValueRef, address: LLVMValueRef) {
        call(llvm.updateReturnRefFunction, listOf(address, value))
    }

    private fun updateRef(value: LLVMValueRef, address: LLVMValueRef, onStack: Boolean,
                          isVolatile: Boolean = false, alignment: Int? = null) {
        require(alignment == null || alignment % runtime.pointerAlignment == 0)
        if (onStack) {
            require(!isVolatile) { "Stack ref update can't be volatile"}
            call(llvm.updateStackRefFunction, listOf(address, value))
        } else {
            if (isVolatile) {
                call(llvm.UpdateVolatileHeapRef, listOf(address, value))
            } else {
                call(llvm.updateHeapRefFunction, listOf(address, value))
            }
        }
    }

    //-------------------------------------------------------------------------//

    fun switchThreadState(state: ThreadState) {
        check(!forbidRuntime) {
            "Attempt to switch the thread state when runtime is forbidden"
        }
        when (state) {
            Native -> call(llvm.Kotlin_mm_switchThreadStateNative, emptyList())
            Runnable -> call(llvm.Kotlin_mm_switchThreadStateRunnable, emptyList())
        }.let {} // Force exhaustive.
    }

    fun memset(pointer: LLVMValueRef, value: Byte, size: Int, isVolatile: Boolean = false) =
            call(llvm.memsetFunction,
                    listOf(pointer,
                            llvm.int8(value),
                            llvm.int32(size),
                            llvm.int1(isVolatile)))

    fun call(llvmCallable: LlvmCallable, args: List<LLVMValueRef>,
             resultLifetime: Lifetime = Lifetime.IRRELEVANT,
             exceptionHandler: ExceptionHandler = ExceptionHandler.None,
             verbatim: Boolean = false,
             resultSlot: LLVMValueRef? = null,
    ): LLVMValueRef {
        val callArgs = if (verbatim || !llvmCallable.returnsObjectType) {
            args
        } else {
            // If function returns an object - create slot for the returned value or give local arena.
            // This allows appropriate rootset accounting by just looking at the stack slots,
            // along with ability to allocate in appropriate arena.
            val realResultSlot = resultSlot ?: when (resultLifetime.slotType) {
                SlotType.STACK -> {
                    localAllocs++
                    // Case of local call. Use memory allocated on stack.
                    val type = llvmCallable.returnType
                    val stackPointer = alloca(type, llvmCallable.returnsObjectType)
                    //val objectHeader = structGep(stackPointer, 0)
                    //setTypeInfoForLocalObject(objectHeader)
                    stackPointer
                    //arenaSlot!!
                }

                SlotType.RETURN -> returnSlot!!

                SlotType.ANONYMOUS -> vars.createAnonymousSlot(llvmCallable.returnsObjectType)

                else -> throw Error("Incorrect slot type: ${resultLifetime.slotType}")
            }
            args + realResultSlot
        }
        return callRaw(llvmCallable, callArgs, exceptionHandler)
    }

    private fun callRaw(llvmCallable: LlvmCallable, args: List<LLVMValueRef>,
                        exceptionHandler: ExceptionHandler): LLVMValueRef {
        if (llvmCallable.isNoUnwind) {
            return llvmCallable.buildCall(builder, args)
        } else {
            val unwind = when (exceptionHandler) {
                ExceptionHandler.Caller -> cleanupLandingpad
                is ExceptionHandler.Local -> exceptionHandler.unwind

                ExceptionHandler.None -> {
                    // When calling a function that is not marked as nounwind (can throw an exception),
                    // it is required to specify an unwind label to handle exceptions properly.
                    // Runtime C++ function can be marked as non-throwing using `RUNTIME_NOTHROW`.
                    val functionName = llvmCallable.name
                    val message =
                            "no exception handler specified when calling function $functionName without nounwind attr"
                    throw IllegalArgumentException(message)
                }
            }

            val position = position()
            val endLocation = position?.end
            val success = basicBlock("call_success", endLocation)
            val result = llvmCallable.buildInvoke(builder, args, success, unwind)
            // Store invoke instruction and its success block in reverse order.
            // Reverse order allows save arguments valid during all work with invokes
            // because other invokes processed before can be inside arguments list.
            if (exceptionHandler == ExceptionHandler.Caller)
                invokeInstructions.add(0, FunctionInvokeInformation(result, llvmCallable, args, success))
            positionAtEnd(success)

            return result
        }
    }

    //-------------------------------------------------------------------------//

    fun phi(type: LLVMTypeRef, name: String = ""): LLVMValueRef {
        return LLVMBuildPhi(builder, type, name)!!
    }

    fun addPhiIncoming(phi: LLVMValueRef, vararg incoming: Pair<LLVMBasicBlockRef, LLVMValueRef>) {
        memScoped {
            val incomingValues = incoming.map { it.second }.toCValues()
            val incomingBlocks = incoming.map { it.first }.toCValues()

            LLVMAddIncoming(phi, incomingValues, incomingBlocks, incoming.size)
        }
    }

    fun assignPhis(vararg phiToValue: Pair<LLVMValueRef, LLVMValueRef>) {
        phiToValue.forEach {
            addPhiIncoming(it.first, currentBlock to it.second)
        }
    }

    fun allocInstance(typeInfo: LLVMValueRef, lifetime: Lifetime, resultSlot: LLVMValueRef?) : LLVMValueRef =
            call(llvm.allocInstanceFunction, listOf(typeInfo), lifetime, resultSlot = resultSlot)

    fun allocInstance(irClass: IrClass, lifetime: Lifetime, resultSlot: LLVMValueRef?) =
        if (lifetime == Lifetime.STACK)
            stackLocalsManager.alloc(irClass)
        else
            allocInstance(codegen.typeInfoForAllocation(irClass), lifetime, resultSlot)

    fun allocArray(
        irClass: IrClass,
        count: LLVMValueRef,
        lifetime: Lifetime,
        exceptionHandler: ExceptionHandler,
        resultSlot: LLVMValueRef? = null
    ): LLVMValueRef {
        val typeInfo = codegen.typeInfoValue(irClass)
        return if (lifetime == Lifetime.STACK) {
            require(LLVMIsConstant(count) != 0) { "Expected a constant for the size of a stack-allocated array" }
            stackLocalsManager.allocArray(irClass, count)
        } else {
            call(llvm.allocArrayFunction, listOf(typeInfo, count), lifetime, exceptionHandler, resultSlot = resultSlot)
        }
    }

    fun unreachable(): LLVMValueRef? {
        if (context.config.debug) {
            call(llvm.llvmTrap, emptyList())
        }
        val res = LLVMBuildUnreachable(builder)
        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun br(bbLabel: LLVMBasicBlockRef): LLVMValueRef {
        val res = LLVMBuildBr(builder, bbLabel)!!
        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun condBr(condition: LLVMValueRef?, bbTrue: LLVMBasicBlockRef?, bbFalse: LLVMBasicBlockRef?): LLVMValueRef? {
        val res = LLVMBuildCondBr(builder, condition, bbTrue, bbFalse)
        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun blockAddress(bbLabel: LLVMBasicBlockRef): LLVMValueRef {
        return function.blockAddress(bbLabel)
    }

    fun not(arg: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildNot(builder, arg, name)!!
    fun and(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildAnd(builder, arg0, arg1, name)!!
    fun or(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildOr(builder, arg0, arg1, name)!!
    fun xor(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildXor(builder, arg0, arg1, name)!!

    fun zext(arg: LLVMValueRef, type: LLVMTypeRef): LLVMValueRef =
            LLVMBuildZExt(builder, arg, type, "")!!

    fun sext(arg: LLVMValueRef, type: LLVMTypeRef): LLVMValueRef =
            LLVMBuildSExt(builder, arg, type, "")!!

    fun ext(arg: LLVMValueRef, type: LLVMTypeRef, signed: Boolean): LLVMValueRef =
            if (signed) {
                sext(arg, type)
            } else {
                zext(arg, type)
            }

    fun trunc(arg: LLVMValueRef, type: LLVMTypeRef): LLVMValueRef =
            LLVMBuildTrunc(builder, arg, type, "")!!

    private fun shift(op: LLVMOpcode, arg: LLVMValueRef, amount: Int) =
            if (amount == 0) {
                arg
            } else {
                LLVMBuildBinOp(builder, op, arg, LLVMConstInt(arg.type, amount.toLong(), 0), "")!!
            }

    fun shl(arg: LLVMValueRef, amount: Int) = shift(LLVMOpcode.LLVMShl, arg, amount)

    fun shr(arg: LLVMValueRef, amount: Int, signed: Boolean) =
            shift(if (signed) LLVMOpcode.LLVMAShr else LLVMOpcode.LLVMLShr,
                    arg, amount)

    /* integers comparisons */
    fun icmpEq(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntEQ, arg0, arg1, name)!!

    fun icmpGt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSGT, arg0, arg1, name)!!
    fun icmpGe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSGE, arg0, arg1, name)!!
    fun icmpLt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSLT, arg0, arg1, name)!!
    fun icmpLe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntSLE, arg0, arg1, name)!!
    fun icmpNe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntNE, arg0, arg1, name)!!
    fun icmpULt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntULT, arg0, arg1, name)!!
    fun icmpULe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntULE, arg0, arg1, name)!!
    fun icmpUGt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntUGT, arg0, arg1, name)!!
    fun icmpUGe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildICmp(builder, LLVMIntPredicate.LLVMIntUGE, arg0, arg1, name)!!

    /* floating-point comparisons */
    fun fcmpEq(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOEQ, arg0, arg1, name)!!
    fun fcmpGt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOGT, arg0, arg1, name)!!
    fun fcmpGe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOGE, arg0, arg1, name)!!
    fun fcmpLt(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOLT, arg0, arg1, name)!!
    fun fcmpLe(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFCmp(builder, LLVMRealPredicate.LLVMRealOLE, arg0, arg1, name)!!

    fun sub(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildSub(builder, arg0, arg1, name)!!
    fun add(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildAdd(builder, arg0, arg1, name)!!

    fun fsub(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFSub(builder, arg0, arg1, name)!!
    fun fadd(arg0: LLVMValueRef, arg1: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFAdd(builder, arg0, arg1, name)!!
    fun fneg(arg: LLVMValueRef, name: String = ""): LLVMValueRef = LLVMBuildFNeg(builder, arg, name)!!

    fun select(ifValue: LLVMValueRef, thenValue: LLVMValueRef, elseValue: LLVMValueRef, name: String = ""): LLVMValueRef =
            LLVMBuildSelect(builder, ifValue, thenValue, elseValue, name)!!

    fun bitcast(type: LLVMTypeRef?, value: LLVMValueRef, name: String = "") = LLVMBuildBitCast(builder, value, type, name)!!

    fun intToPtr(value: LLVMValueRef?, DestTy: LLVMTypeRef, Name: String = "") = LLVMBuildIntToPtr(builder, value, DestTy, Name)!!
    fun ptrToInt(value: LLVMValueRef?, DestTy: LLVMTypeRef, Name: String = "") = LLVMBuildPtrToInt(builder, value, DestTy, Name)!!

    fun gep(type: LLVMTypeRef, base: LLVMValueRef, index: LLVMValueRef, name: String = ""): LLVMValueRef =
            LLVMBuildGEP2(builder, type, base, cValuesOf(index), 1, name)!!

    fun structGep(type: LLVMTypeRef, base: LLVMValueRef, index: Int, name: String = ""): LLVMValueRef =
            LLVMBuildStructGEP2(builder, type, base, index, name)!!

    fun extractValue(aggregate: LLVMValueRef, index: Int, name: String = ""): LLVMValueRef =
            LLVMBuildExtractValue(builder, aggregate, index, name)!!

    fun gxxLandingpad(numClauses: Int, name: String = "", switchThreadState: Boolean = false): LLVMValueRef {
        val personalityFunction = llvm.gxxPersonalityFunction

        // Type of `landingpad` instruction result (depends on personality function):
        val landingpadType = llvm.structType(llvm.int8PtrType, llvm.int32Type)
        val landingpad = personalityFunction.buildLandingpad(builder, landingpadType, numClauses, name)

        if (switchThreadState) {
            switchThreadState(Runnable)
        }
        call(llvm.setCurrentFrameFunction, listOf(slotsPhi!!))
        setCurrentFrameIsCalled = true

        return landingpad
    }

    fun extractElement(vector: LLVMValueRef, index: LLVMValueRef, name: String = ""): LLVMValueRef {
        return LLVMBuildExtractElement(builder, vector, index, name)!!
    }

    fun filteringExceptionHandler(
            outerHandler: ExceptionHandler,
            foreignExceptionMode: ForeignExceptionMode.Mode,
            switchThreadState: Boolean
    ): ExceptionHandler {
        val lpBlock = basicBlockInFunction("filteringExceptionHandler", position()?.start)

        val wrapExceptionMode = context.config.target.family.isAppleFamily &&
                foreignExceptionMode == ForeignExceptionMode.Mode.OBJC_WRAP

        appendingTo(lpBlock) {
            val landingpad = gxxLandingpad(2, switchThreadState = switchThreadState)
            LLVMAddClause(landingpad, kotlinExceptionRtti.llvm)
            if (wrapExceptionMode) {
                LLVMAddClause(landingpad, objcNSExceptionRtti.llvm)
            }
            LLVMAddClause(landingpad, LLVMConstNull(llvm.int8PtrType))

            val fatalForeignExceptionBlock = basicBlock("fatalForeignException", position()?.start)
            val forwardKotlinExceptionBlock = basicBlock("forwardKotlinException", position()?.start)

            val typeId = extractValue(landingpad, 1)
            val isKotlinException = icmpEq(
                    typeId,
                    call(llvm.llvmEhTypeidFor, listOf(kotlinExceptionRtti.llvm))
            )

            if (wrapExceptionMode) {
                val foreignExceptionBlock = basicBlock("foreignException", position()?.start)
                val forwardNativeExceptionBlock = basicBlock("forwardNativeException", position()?.start)

                condBr(isKotlinException, forwardKotlinExceptionBlock, foreignExceptionBlock)
                appendingTo(foreignExceptionBlock) {
                    val isObjCException = icmpEq(
                            typeId,
                            call(llvm.llvmEhTypeidFor, listOf(objcNSExceptionRtti.llvm))
                    )
                    condBr(isObjCException, forwardNativeExceptionBlock, fatalForeignExceptionBlock)

                    appendingTo(forwardNativeExceptionBlock) {
                        val exception = createForeignException(landingpad, outerHandler)
                        outerHandler.genThrow(this, exception)
                    }
                }
            } else {
                condBr(isKotlinException, forwardKotlinExceptionBlock, fatalForeignExceptionBlock)
            }

            appendingTo(forwardKotlinExceptionBlock) {
                // Rethrow Kotlin exception to real handler.
                outerHandler.genThrow(this, extractKotlinException(landingpad))
            }

            appendingTo(fatalForeignExceptionBlock) {
                terminateWithCurrentException(landingpad)
            }

        }

        return object : ExceptionHandler.Local() {
            override val unwind: LLVMBasicBlockRef
                get() = lpBlock
        }
    }

    fun terminateWithCurrentException(landingpad: LLVMValueRef) {
        val exceptionRecord = extractValue(landingpad, 0)
        // So `std::terminate` is called from C++ catch block:
        call(llvm.cxaBeginCatchFunction, listOf(exceptionRecord))
        terminate()
    }

    fun terminate() {
        call(llvm.cxxStdTerminate, emptyList())

        // Note: unreachable instruction to be generated here, but debug information is improper in this case.
        val loopBlock = basicBlock("loop", position()?.start)
        br(loopBlock)
        appendingTo(loopBlock) {
            br(loopBlock)
        }
    }

    fun kotlinExceptionHandler(block: FunctionGenerationContext.(exception: LLVMValueRef) -> Unit): ExceptionHandler {
        val lpBlock = basicBlock("kotlinExceptionHandler", position()?.end)

        appendingTo(lpBlock) {
            val exception = catchKotlinException()
            block(exception)
        }

        return object : ExceptionHandler.Local() {
            override val unwind: LLVMBasicBlockRef get() = lpBlock
        }
    }

    fun catchKotlinException(): LLVMValueRef {
        val landingpadResult = gxxLandingpad(numClauses = 1, name = "lp")

        LLVMAddClause(landingpadResult, LLVMConstNull(llvm.int8PtrType))

        // TODO: properly handle C++ exceptions: currently C++ exception can be thrown out from try-finally
        // bypassing the finally block.

        return extractKotlinException(landingpadResult)
    }

    private fun extractKotlinException(landingpadResult: LLVMValueRef): LLVMValueRef {
        val exceptionRecord = extractValue(landingpadResult, 0, "er")

        // __cxa_begin_catch returns pointer to C++ exception object.
        val beginCatch = llvm.cxaBeginCatchFunction
        val exceptionRawPtr = call(beginCatch, listOf(exceptionRecord))

        // Pointer to Kotlin exception object:
        val exceptionPtr = call(llvm.Kotlin_getExceptionObject, listOf(exceptionRawPtr), Lifetime.GLOBAL)

        // __cxa_end_catch performs some C++ cleanup, including calling `ExceptionObjHolder` class destructor.
        val endCatch = llvm.cxaEndCatchFunction
        call(endCatch, listOf())

        return exceptionPtr
    }

    private fun createForeignException(landingpadResult: LLVMValueRef, exceptionHandler: ExceptionHandler): LLVMValueRef {
        val exceptionRecord = extractValue(landingpadResult, 0, "er")

        // __cxa_begin_catch returns pointer to C++ exception object.
        val exceptionRawPtr = call(llvm.cxaBeginCatchFunction, listOf(exceptionRecord))

        // This will take care of ARC - need to be done in the catching scope, i.e. before __cxa_end_catch
        val exception = call(context.ir.symbols.createForeignException.owner.llvmFunction,
                listOf(exceptionRawPtr),
                Lifetime.GLOBAL, exceptionHandler)

        call(llvm.cxaEndCatchFunction, listOf())
        return exception
    }

    fun generateFrameCheck() {
        if (!context.shouldOptimize())
            call(llvm.checkCurrentFrameFunction, listOf(slotsPhi!!))
    }

    inline fun ifThenElse(
            condition: LLVMValueRef,
            thenValue: LLVMValueRef,
            elseBlock: () -> LLVMValueRef
    ): LLVMValueRef {
        val resultType = thenValue.type

        val position = position()
        val endPosition = position()?.end
        val bbExit = basicBlock(startLocationInfo = endPosition)
        val resultPhi = appendingTo(bbExit) {
            phi(resultType)
        }

        val bbElse = basicBlock(startLocationInfo = position?.start, endLocationInfo = endPosition)

        condBr(condition, bbExit, bbElse)
        assignPhis(resultPhi to thenValue)

        appendingTo(bbElse) {
            val elseValue = elseBlock()
            br(bbExit)
            assignPhis(resultPhi to elseValue)
        }

        positionAtEnd(bbExit)
        return resultPhi
    }

    inline fun ifThen(condition: LLVMValueRef, thenBlock: () -> Unit) {
        val endPosition = position()?.end
        val bbExit = basicBlock(startLocationInfo = endPosition)
        val bbThen = basicBlock(startLocationInfo = endPosition)

        condBr(condition, bbThen, bbExit)

        appendingTo(bbThen) {
            thenBlock()
            if (!isAfterTerminator()) br(bbExit)
        }

        positionAtEnd(bbExit)
    }

    internal fun debugLocation(startLocationInfo: LocationInfo, endLocation: LocationInfo?): DILocationRef? {
        if (!context.shouldContainLocationDebugInfo()) return null
        update(currentBlock, startLocationInfo, endLocation)
        val debugLocation = codegen.generateLocationInfo(startLocationInfo)
        currentPositionHolder.setBuilderDebugLocation(debugLocation)
        return debugLocation
    }

    fun indirectBr(address: LLVMValueRef, destinations: Collection<LLVMBasicBlockRef>): LLVMValueRef? {
        val indirectBr = LLVMBuildIndirectBr(builder, address, destinations.size)
        destinations.forEach { LLVMAddDestination(indirectBr, it) }
        currentPositionHolder.setAfterTerminator()
        return indirectBr
    }

    fun switch(value: LLVMValueRef, cases: Collection<Pair<LLVMValueRef, LLVMBasicBlockRef>>, elseBB: LLVMBasicBlockRef): LLVMValueRef? {
        val switch = LLVMBuildSwitch(builder, value, elseBB, cases.size)
        cases.forEach { LLVMAddCase(switch, it.first, it.second) }
        currentPositionHolder.setAfterTerminator()
        return switch
    }

    fun loadTypeInfo(objPtr: LLVMValueRef): LLVMValueRef {
        val typeInfoOrMetaPtr = structGep(runtime.objHeaderType, objPtr, 0  /* typeInfoOrMeta_ */)

        /**
         * Formally, this ordering is too weak, and doesn't prevent data race with installing extra object.
         * Check comment in ObjHeader::type_info for details.
         */
        val memoryOrder = LLVMAtomicOrdering.LLVMAtomicOrderingMonotonic

        // TODO: Get rid of the bitcast here by supplying the type in the GEP above.
        val typeInfoOrMetaPtrRaw = bitcast(pointerType(codegen.intPtrType), typeInfoOrMetaPtr)
        val typeInfoOrMetaWithFlags = load(codegen.intPtrType, typeInfoOrMetaPtrRaw, memoryOrder = memoryOrder)
        // Clear two lower bits.
        val typeInfoOrMetaRaw = and(typeInfoOrMetaWithFlags, codegen.immTypeInfoMask)
        val typeInfoOrMeta = intToPtr(typeInfoOrMetaRaw, kTypeInfoPtr)
        val typeInfoPtrPtr = structGep(runtime.typeInfoType, typeInfoOrMeta, 0 /* typeInfo */)
        return load(codegen.kTypeInfoPtr, typeInfoPtrPtr, memoryOrder = LLVMAtomicOrdering.LLVMAtomicOrderingMonotonic)
    }

    /**
     * Note: the same code is generated as IR in [org.jetbrains.kotlin.backend.konan.lower.EnumUsageLowering].
     */
    fun getEnumEntry(enumEntry: IrEnumEntry, exceptionHandler: ExceptionHandler): LLVMValueRef {
        val enumClass = enumEntry.parentAsClass
        val getterId = context.enumsSupport.enumEntriesMap(enumClass)[enumEntry.name]!!.getterId
        return call(
                context.enumsSupport.getValueGetter(enumClass).llvmFunction,
                listOf(llvm.int32(getterId)),
                Lifetime.GLOBAL,
                exceptionHandler
        )
    }

    // TODO: get rid of exceptionHandler argument by ensuring that all called functions are non-throwing.
    fun getObjCClass(irClass: IrClass, exceptionHandler: ExceptionHandler): LLVMValueRef {
        assert(!irClass.isInterface)

        return if (irClass.isExternalObjCClass()) {
            generationState.dependenciesTracker.add(irClass)
            if (irClass.isObjCMetaClass()) {
                val name = irClass.getExternalObjCMetaClassBinaryName()
                val objCClass = getObjCClass(name)

                val getClass = llvm.externalNativeRuntimeFunction(
                        "object_getClass",
                        LlvmRetType(llvm.int8PtrType, isObjectType = false),
                        listOf(LlvmParamType(llvm.int8PtrType))
                )
                call(getClass, listOf(objCClass), exceptionHandler = exceptionHandler)
            } else {
                getObjCClass(irClass.getExternalObjCClassBinaryName())
            }
        } else {
            if (irClass.isObjCMetaClass()) {
                error("type-checking against Kotlin classes inheriting Objective-C meta-classes isn't supported yet")
            }

            val classInfo = codegen.kotlinObjCClassInfo(irClass)
            val classPointerGlobal = load(llvm.int8PtrPtrType, structGep(runtime.kotlinObjCClassInfo, classInfo, KotlinObjCClassInfoGenerator.createdClassFieldIndex))

            val storedClass = this.load(llvm.int8PtrType, classPointerGlobal)

            val storedClassIsNotNull = this.icmpNe(storedClass, llvm.kNullInt8Ptr)

            return this.ifThenElse(storedClassIsNotNull, storedClass) {
                call(
                        llvm.createKotlinObjCClass,
                        listOf(classInfo),
                        exceptionHandler = exceptionHandler
                )
            }
        }
    }

    private fun getObjCClass(binaryName: String) = load(llvm.int8PtrType, codegen.objCDataGenerator!!.genClassRef(binaryName).llvm)

    fun getObjCClassFromNativeRuntime(binaryName: String): LLVMValueRef {
        generationState.dependenciesTracker.addNativeRuntime()
        return getObjCClass(binaryName)
    }

    fun resetDebugLocation() {
        if (!context.shouldContainLocationDebugInfo()) return
        currentPositionHolder.resetBuilderDebugLocation()
    }

    fun position() = basicBlockToLastLocation[currentBlock]

    internal fun mapParameterForDebug(index: Int, value: LLVMValueRef) {
        appendingTo(localsInitBb) {
            LLVMBuildStore(builder, value, vars.addressOf(index))
        }
    }

    internal fun prologue() {
        if (function.returnsObjectType) {
            returnSlot = function.param( function.numParams - 1)
        }

        positionAtEnd(localsInitBb)
        slotsPhi = phi(kObjHeaderPtrPtr)
        // Is removed by DCE trivially, if not needed.
        /*arenaSlot = intToPtr(
                or(ptrToInt(slotsPhi, codegen.intPtrType), codegen.immOneIntPtrType), kObjHeaderPtrPtr)*/
        positionAtEnd(entryBb)
    }

    internal fun epilogue() {
        val needCleanupLandingpadAndLeaveFrame = this.needCleanupLandingpadAndLeaveFrame

        appendingTo(prologueBb) {
            val slots = if (needSlotsPhi || needCleanupLandingpadAndLeaveFrame)
                LLVMBuildArrayAlloca(builder, kObjHeaderPtr, llvm.int32(slotCount), "")!!
            else
                kNullObjHeaderPtrPtr
            if (needSlots || needCleanupLandingpadAndLeaveFrame) {
                check(!forbidRuntime) { "Attempt to start a frame where runtime usage is forbidden" }
                // Zero-init slots.
                val slotsMem = bitcast(llvm.int8PtrType, slots)
                memset(slotsMem, 0, slotCount * codegen.runtime.pointerSize)
            }
            addPhiIncoming(slotsPhi!!, prologueBb to slots)
            memScoped {
                slotToVariableLocation.forEach { (slot, variable) ->
                    val expr = longArrayOf(DwarfOp.DW_OP_plus_uconst.value,
                            runtime.pointerSize * slot.toLong()).toCValues()
                    DIInsertDeclaration(
                            builder       = generationState.debugInfo.builder,
                            value         = slots,
                            localVariable = variable.localVariable,
                            location      = variable.location,
                            bb            = prologueBb,
                            expr          = expr,
                            exprCount     = 2)
                }
            }
            br(localsInitBb)
        }

        appendingTo(localsInitBb) {
            br(stackLocalsInitBb)
        }

        if (needCleanupLandingpadAndLeaveFrame) {
            appendingTo(cleanupLandingpad) {
                val landingpad = gxxLandingpad(numClauses = 0)
                LLVMSetCleanup(landingpad, 1)

                releaseVars()
                handleEpilogueExperimentalMM()
                LLVMBuildResume(builder, landingpad)
            }
        }

        appendingTo(stackLocalsInitBb) {
            /**
             * Function calls need to have !dbg, otherwise llvm rejects full module debug information
             * On the other hand, we don't want prologue to have debug info, because it can lead to debugger stops in
             * places with inconsistent stack layout. So we setup debug info only for this part of bb.
             */
            startLocation?.let { debugLocation(it, it) }
            if (needsRuntimeInit || switchToRunnable) {
                check(!forbidRuntime) { "Attempt to init runtime where runtime usage is forbidden" }
                call(llvm.initRuntimeIfNeeded, emptyList())
            }
            if (switchToRunnable) {
                switchThreadState(Runnable)
            }
            if (needSlots || needCleanupLandingpadAndLeaveFrame) {
                call(llvm.enterFrameFunction, listOf(slotsPhi!!, llvm.int32(vars.skipSlots), llvm.int32(slotCount)))
            } else {
                check(!setCurrentFrameIsCalled)
            }
            if (!forbidRuntime && needSafePoint) {
                call(llvm.Kotlin_mm_safePointFunctionPrologue, emptyList())
            }
            resetDebugLocation()
            br(entryBb)
        }

        processReturns()

        // If cleanup landingpad is trivial or unused, remove it.
        // It would be great not to generate it in the first place in this case,
        // but this would be complicated without a major refactoring.
        if (!needCleanupLandingpadAndLeaveFrame || invokeInstructions.isEmpty()) {
            // Replace invokes with calls and branches.
            invokeInstructions.forEach { functionInvokeInfo ->
                positionBefore(functionInvokeInfo.invokeInstruction)
                val newResult = functionInvokeInfo.llvmFunction.buildCall(builder, functionInvokeInfo.args)
                // Have to generate `br` instruction because of current scheme of debug info.
                br(functionInvokeInfo.success)
                LLVMReplaceAllUsesWith(functionInvokeInfo.invokeInstruction, newResult)
                LLVMInstructionEraseFromParent(functionInvokeInfo.invokeInstruction)
            }
            LLVMDeleteBasicBlock(cleanupLandingpad)
        }

        vars.clear()
        returnSlot = null
        slotsPhi = null
    }

    protected abstract fun processReturns()

    protected fun retValue(value: LLVMValueRef): LLVMValueRef {
        if (returnSlot != null) {
            updateReturnRef(value, returnSlot!!)
        }
        onReturn()
        return rawRet(value)
    }

    protected fun rawRet(value: LLVMValueRef): LLVMValueRef = LLVMBuildRet(builder, value)!!.also {
        currentPositionHolder.setAfterTerminator()
    }

    protected fun retVoid(): LLVMValueRef {
        check(returnSlot == null)
        onReturn()
        return LLVMBuildRetVoid(builder)!!.also {
            currentPositionHolder.setAfterTerminator()
        }
    }

    protected fun onReturn() {
        releaseVars()
        handleEpilogueExperimentalMM()
    }

    private fun handleEpilogueExperimentalMM() {
        if (switchToRunnable) {
            check(!forbidRuntime) { "Generating a bridge when runtime is forbidden" }
            switchThreadState(Native)
        }
    }

    private val kotlinExceptionRtti: ConstPointer
        get() = constPointer(importNativeRuntimeGlobal(
                "_ZTI18ExceptionObjHolder", // typeinfo for ObjHolder
                llvm.int8PtrType
        )).bitcast(llvm.int8PtrType)

    private val objcNSExceptionRtti: ConstPointer by lazy {
        constPointer(importNativeRuntimeGlobal(
                "OBJC_EHTYPE_\$_NSException", // typeinfo for NSException*
                llvm.int8PtrType
        )).bitcast(llvm.int8PtrType)
    }

    //-------------------------------------------------------------------------//

    /**
     * Represents the mutable position of instructions being inserted.
     *
     * This class is introduced to workaround unreachable code handling.
     */
    inner class PositionHolder {
        private val builder: LLVMBuilderRef = LLVMCreateBuilderInContext(llvm.llvmContext)!!


        fun getBuilder(): LLVMBuilderRef {
            if (isAfterTerminator) {
                val position = position()
                positionAtEnd(basicBlock("unreachable", position?.start, position?.end))
            }

            return builder
        }

        /**
         * Should be `true` iff the position is located after terminator instruction.
         */
        var isAfterTerminator: Boolean = false
            private set

        fun setAfterTerminator() {
            isAfterTerminator = true
        }

        val currentBlock: LLVMBasicBlockRef
            get() = LLVMGetInsertBlock(builder)!!

        fun positionAtEnd(block: LLVMBasicBlockRef) {
            LLVMPositionBuilderAtEnd(builder, block)
            basicBlockToLastLocation[block]?.let{ debugLocation(it.start, it.end) }
            val lastInstr = LLVMGetLastInstruction(block)
            isAfterTerminator = lastInstr != null && (LLVMIsATerminatorInst(lastInstr) != null)
        }

        fun positionBefore(instruction: LLVMValueRef) {
            LLVMPositionBuilderBefore(builder, instruction)
            val previousInstr = LLVMGetPreviousInstruction(instruction)
            isAfterTerminator = previousInstr != null && (LLVMIsATerminatorInst(previousInstr) != null)
        }

        fun dispose() {
            LLVMDisposeBuilder(builder)
        }

        fun resetBuilderDebugLocation() {
            if (!context.shouldContainLocationDebugInfo()) return
            LLVMBuilderResetDebugLocation(builder)
        }

        fun setBuilderDebugLocation(debugLocation: DILocationRef?) {
            if (!context.shouldContainLocationDebugInfo()) return
            LLVMBuilderSetDebugLocation(builder, debugLocation)
        }
    }

    private var currentPositionHolder: PositionHolder = PositionHolder()

    /**
     * Returns `true` iff the current code generation position is located after terminator instruction.
     */
    fun isAfterTerminator() = currentPositionHolder.isAfterTerminator

    val currentBlock: LLVMBasicBlockRef
        get() = currentPositionHolder.currentBlock

    /**
     * The builder representing the current code generation position.
     *
     * Note that it shouldn't be positioned directly using LLVM API due to some hacks.
     * Use e.g. [positionAtEnd] instead. See [PositionHolder] for details.
     */
    val builder: LLVMBuilderRef
        get() = currentPositionHolder.getBuilder()

    fun positionAtEnd(bbLabel: LLVMBasicBlockRef) = currentPositionHolder.positionAtEnd(bbLabel)

    fun positionBefore(instruction: LLVMValueRef) = currentPositionHolder.positionBefore(instruction)

    inline private fun <R> preservingPosition(code: () -> R): R {
        val oldPositionHolder = currentPositionHolder
        val newPositionHolder = PositionHolder()
        currentPositionHolder = newPositionHolder
        try {
            return code()
        } finally {
            currentPositionHolder = oldPositionHolder
            newPositionHolder.dispose()
        }
    }

    inline fun <R> appendingTo(block: LLVMBasicBlockRef, code: FunctionGenerationContext.() -> R) = preservingPosition {
        positionAtEnd(block)
        code()
    }

    private val needSlots: Boolean
        get() {
            return slotCount - vars.skipSlots > frameOverlaySlotCount
        }

    private val needSlotsPhi: Boolean
        get() {
            return slotCount > frameOverlaySlotCount || localAllocs > 0
        }

    private fun releaseVars() {
        if (needCleanupLandingpadAndLeaveFrame || needSlots) {
            check(!forbidRuntime) { "Attempt to leave a frame where runtime usage is forbidden" }
            call(llvm.leaveFrameFunction,
                    listOf(slotsPhi!!, llvm.int32(vars.skipSlots), llvm.int32(slotCount)))
        }
    }
}

internal class DefaultFunctionGenerationContext(
        function: LlvmCallable,
        codegen: CodeGenerator,
        startLocation: LocationInfo?,
        endLocation: LocationInfo?,
        switchToRunnable: Boolean,
        needSafePoint: Boolean,
        irFunction: IrSimpleFunction? = null
) : FunctionGenerationContext(
        function,
        codegen,
        startLocation,
        endLocation,
        switchToRunnable,
        needSafePoint,
        irFunction
) {
    // Note: return handling can be extracted to a separate class.

    private val returns: MutableMap<LLVMBasicBlockRef, LLVMValueRef> = mutableMapOf()

    private val epilogueBb = basicBlockInFunction("epilogue", endLocation).also {
        LLVMMoveBasicBlockBefore(it, cleanupLandingpad) // Just to make the produced code a bit more readable.
    }

    override fun ret(value: LLVMValueRef?): LLVMValueRef {
        val res = br(epilogueBb)

        if (returns.containsKey(currentBlock)) {
            // TODO: enable error throwing.
            throw Error("ret() in the same basic block twice! in ${function.name}")
        }

        if (value != null)
            returns[currentBlock] = value

        return res
    }

    override fun processReturns() {
        appendingTo(epilogueBb) {
            when {
                returnType == llvm.voidType -> {
                    retVoid()
                }
                returns.isNotEmpty() -> {
                    val returnPhi = phi(returnType!!)
                    addPhiIncoming(returnPhi, *returns.toList().toTypedArray())
                    retValue(returnPhi)
                }
                // Do nothing, all paths throw.
                else -> unreachable()
            }
        }
        returns.clear()
    }
}
