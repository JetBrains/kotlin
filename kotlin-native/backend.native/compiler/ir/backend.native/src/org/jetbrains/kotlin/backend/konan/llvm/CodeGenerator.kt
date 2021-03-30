/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm


import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.backend.konan.descriptors.ClassGlobalHierarchyInfo
import org.jetbrains.kotlin.backend.konan.llvm.objc.*
import org.jetbrains.kotlin.konan.target.CompilerOutputKind
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.backend.konan.ir.*
import org.jetbrains.kotlin.descriptors.konan.CompiledKlibModuleOrigin
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrGetObjectValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.konan.ForeignExceptionMode

private fun IrConstructor.isAnyConstructorDelegation(context: Context): Boolean {
        val statements = this.body?.statements ?: return false
        if (statements.size != 2) return false
        val lastStatement = statements[1]
        if (lastStatement !is IrReturn ||
                (lastStatement.value as? IrGetObjectValue)?.symbol != context.irBuiltIns.unitClass) return false
        val constructorCall = statements[0] as? IrDelegatingConstructorCall ?: return false
        val constructor = constructorCall.symbol.owner as? IrConstructor ?: return false
        return constructor.constructedClass.isAny()
    }

// TODO: shall we memoize that property?
internal fun IrClass.hasConstStateAndNoSideEffects(context: Context): Boolean {
    if (!context.shouldOptimize()) return false
    if (this.hasAnnotation(KonanFqNames.canBePrecreated)) return true
    val fields = context.getLayoutBuilder(this).fields
    return fields.isEmpty() && this.constructors.all { it.isAnyConstructorDelegation(context) }
}

internal class CodeGenerator(override val context: Context) : ContextUtils {

    fun llvmFunction(function: IrFunction): LLVMValueRef = llvmFunctionOrNull(function) ?: error("no function ${function.name} in ${function.file.fqName}")
    fun llvmFunctionOrNull(function: IrFunction): LLVMValueRef? = function.llvmFunctionOrNull
    val intPtrType = LLVMIntPtrTypeInContext(llvmContext, llvmTargetData)!!
    internal val immOneIntPtrType = LLVMConstInt(intPtrType, 1, 1)!!
    internal val immThreeIntPtrType = LLVMConstInt(intPtrType, 3, 1)!!
    // Keep in sync with OBJECT_TAG_MASK in C++.
    internal val immTypeInfoMask = LLVMConstNot(LLVMConstInt(intPtrType, 3, 0)!!)!!

    //-------------------------------------------------------------------------//

    fun typeInfoValue(irClass: IrClass): LLVMValueRef = irClass.llvmTypeInfoPtr

    fun param(fn: IrFunction, i: Int): LLVMValueRef {
        assert(i >= 0 && i < countParams(fn))
        return LLVMGetParam(fn.llvmFunction, i)!!
    }

    private fun countParams(fn: IrFunction) = LLVMCountParams(fn.llvmFunction)

    fun functionEntryPointAddress(function: IrFunction) = function.entryPointAddress.llvm
    fun functionHash(function: IrFunction): LLVMValueRef = function.computeFunctionName().localHash.llvm

    fun typeInfoForAllocation(constructedClass: IrClass): LLVMValueRef {
        assert(!constructedClass.isObjCClass())
        return typeInfoValue(constructedClass)
    }

    fun generateLocationInfo(locationInfo: LocationInfo): DILocationRef? = if (locationInfo.inlinedAt != null)
        LLVMCreateLocationInlinedAt(LLVMGetModuleContext(context.llvmModule), locationInfo.line, locationInfo.column,
                locationInfo.scope, generateLocationInfo(locationInfo.inlinedAt))
    else
        LLVMCreateLocation(LLVMGetModuleContext(context.llvmModule), locationInfo.line, locationInfo.column, locationInfo.scope)

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
}

val LLVMValueRef.name:String?
    get() = LLVMGetValueName(this)?.toKString()

val LLVMValueRef.isConst:Boolean
    get() = (LLVMIsConstant(this) == 1)


internal inline fun<R> generateFunction(codegen: CodeGenerator,
                                        function: IrFunction,
                                        startLocation: LocationInfo? = null,
                                        endLocation: LocationInfo? = null,
                                        code: FunctionGenerationContext.(FunctionGenerationContext) -> R) {
    val llvmFunction = codegen.llvmFunction(function)

    val functionGenerationContext = FunctionGenerationContext(
            llvmFunction,
            codegen,
            startLocation,
            endLocation,
            function)
    try {
        generateFunctionBody(functionGenerationContext, code)
    } finally {
        functionGenerationContext.dispose()
    }

    // To perform per-function validation.
    if (false)
        LLVMVerifyFunction(llvmFunction, LLVMVerifierFailureAction.LLVMAbortProcessAction)
}


internal inline fun<R> generateFunction(codegen: CodeGenerator, function: LLVMValueRef,
                                        startLocation: LocationInfo? = null, endLocation: LocationInfo? = null,
                                        code:FunctionGenerationContext.(FunctionGenerationContext) -> R) {
    val functionGenerationContext = FunctionGenerationContext(function, codegen, startLocation, endLocation)
    try {
        generateFunctionBody(functionGenerationContext, code)
    } finally {
        functionGenerationContext.dispose()
    }
}

internal inline fun generateFunction(
        codegen: CodeGenerator,
        functionType: LLVMTypeRef,
        name: String,
        block: FunctionGenerationContext.(FunctionGenerationContext) -> Unit
): LLVMValueRef {
    val function = addLlvmFunctionWithDefaultAttributes(
            codegen.context,
            codegen.context.llvmModule!!,
            name,
            functionType
    )
    generateFunction(codegen, function, startLocation = null, endLocation = null, code = block)
    return function
}

// TODO: Consider using different abstraction than `FunctionGenerationContext` for `generateFunctionNoRuntime`.
internal inline fun <R> generateFunctionNoRuntime(
        codegen: CodeGenerator,
        function: LLVMValueRef,
        code: FunctionGenerationContext.(FunctionGenerationContext) -> R,
) {
    val functionGenerationContext = FunctionGenerationContext(function, codegen, null, null)
    try {
        functionGenerationContext.forbidRuntime = true
        require(!functionGenerationContext.isObjectType(functionGenerationContext.returnType!!)) {
            "Cannot return object from function without Kotlin runtime"
        }

        generateFunctionBody(functionGenerationContext, code)
    } finally {
        functionGenerationContext.dispose()
    }
}

internal inline fun generateFunctionNoRuntime(
        codegen: CodeGenerator,
        functionType: LLVMTypeRef,
        name: String,
        code: FunctionGenerationContext.(FunctionGenerationContext) -> Unit,
): LLVMValueRef {
    val function = addLlvmFunctionWithDefaultAttributes(
            codegen.context,
            codegen.context.llvmModule!!,
            name,
            functionType
    )
    generateFunctionNoRuntime(codegen, function, code)
    return function
}

private inline fun <R> generateFunctionBody(
        functionGenerationContext: FunctionGenerationContext,
        code: FunctionGenerationContext.(FunctionGenerationContext) -> R) {
    functionGenerationContext.prologue()
    functionGenerationContext.code(functionGenerationContext)
    if (!functionGenerationContext.isAfterTerminator())
        functionGenerationContext.unreachable()
    functionGenerationContext.epilogue()
    functionGenerationContext.resetDebugLocation()
}

/**
 * There're cases when we don't need end position or it is meaningless.
 */
internal data class LocationInfoRange(var start: LocationInfo, var end: LocationInfo?)

internal interface StackLocalsManager {
    fun alloc(irClass: IrClass, cleanFieldsExplicitly: Boolean): LLVMValueRef

    fun allocArray(irClass: IrClass, count: LLVMValueRef): LLVMValueRef

    fun clean(refsOnly: Boolean)
}

internal class StackLocalsManagerImpl(
        val functionGenerationContext: FunctionGenerationContext,
        val bbInitStackLocals: LLVMBasicBlockRef
) : StackLocalsManager {
    private class StackLocal(val isArray: Boolean, val irClass: IrClass,
                             val bodyPtr: LLVMValueRef, val objHeaderPtr: LLVMValueRef)

    private val stackLocals = mutableListOf<StackLocal>()

    override fun alloc(irClass: IrClass, cleanFieldsExplicitly: Boolean): LLVMValueRef = with(functionGenerationContext) {
        val type = context.llvmDeclarations.forClass(irClass).bodyType
        val stackLocal = appendingTo(bbInitStackLocals) {
            val stackSlot = LLVMBuildAlloca(builder, type, "")!!

            call(context.llvm.memsetFunction,
                    listOf(bitcast(kInt8Ptr, stackSlot),
                            Int8(0).llvm,
                            Int32(LLVMSizeOfTypeInBits(codegen.llvmTargetData, type).toInt() / 8).llvm,
                            Int1(0).llvm))

            val objectHeader = structGep(stackSlot, 0, "objHeader")
            val typeInfo = codegen.typeInfoForAllocation(irClass)
            setTypeInfoForLocalObject(objectHeader, typeInfo)
            StackLocal(false, irClass, stackSlot, objectHeader)
        }

        stackLocals += stackLocal
        if (cleanFieldsExplicitly)
            clean(stackLocal, false)
        stackLocal.objHeaderPtr
    }

    // Returns generated special type for local array.
    // It's needed to prevent changing variables order on stack.
    private fun localArrayType(irClass: IrClass, count: Int) = with(functionGenerationContext) {
        val name = "local#${irClass.name}${count}#internal"
        // Create new type or get already created.
        context.declaredLocalArrays.getOrPut(name) {
            val fieldTypes = listOf(kArrayHeader, LLVMArrayType(arrayToElementType[irClass.symbol]!!, count))
            val classType = LLVMStructCreateNamed(LLVMGetModuleContext(context.llvmModule), name)!!
            LLVMStructSetBody(classType, fieldTypes.toCValues(), fieldTypes.size, 1)
            classType
        }
    }

    private val symbols = functionGenerationContext.context.ir.symbols
    // TODO: find better place?
    private val arrayToElementType = mapOf(
            symbols.array              to functionGenerationContext.kObjHeaderPtr,
            symbols.byteArray          to int8Type,
            symbols.charArray          to int16Type,
            symbols.shortArray         to int16Type,
            symbols.intArray           to int32Type,
            symbols.longArray          to int64Type,
            symbols.floatArray         to floatType,
            symbols.doubleArray        to doubleType,
            symbols.booleanArray       to int8Type
    )

    override fun allocArray(irClass: IrClass, count: LLVMValueRef) = with(functionGenerationContext) {
        val stackLocal = appendingTo(bbInitStackLocals) {
            val constCount = extractConstUnsignedInt(count).toInt()
            val arrayType = localArrayType(irClass, constCount)
            val typeInfo = codegen.typeInfoValue(irClass)
            val arraySlot = LLVMBuildAlloca(builder, arrayType, "")!!
            // Set array size in ArrayHeader.
            val arrayHeaderSlot = structGep(arraySlot, 0, "arrayHeader")
            setTypeInfoForLocalObject(arrayHeaderSlot, typeInfo)
            val sizeField = structGep(arrayHeaderSlot, 1, "count_")
            store(count, sizeField)

            call(context.llvm.memsetFunction,
                    listOf(bitcast(kInt8Ptr, structGep(arraySlot, 1, "arrayBody")),
                            Int8(0).llvm,
                            Int32(constCount * LLVMSizeOfTypeInBits(codegen.llvmTargetData,
                                    arrayToElementType[irClass.symbol]).toInt() / 8).llvm,
                            Int1(0).llvm))
            StackLocal(true, irClass, arraySlot, arrayHeaderSlot)
        }

        stackLocals += stackLocal
        bitcast(kObjHeaderPtr, stackLocal.objHeaderPtr)
    }

    override fun clean(refsOnly: Boolean) = stackLocals.forEach { clean(it, refsOnly) }

    private fun clean(stackLocal: StackLocal, refsOnly: Boolean) = with(functionGenerationContext) {
        if (stackLocal.isArray) {
            if (stackLocal.irClass.symbol == context.ir.symbols.array)
                call(context.llvm.zeroArrayRefsFunction, listOf(stackLocal.objHeaderPtr))
        } else {
            val type = context.llvmDeclarations.forClass(stackLocal.irClass).bodyType
            for (field in context.getLayoutBuilder(stackLocal.irClass).fields) {
                val fieldIndex = context.llvmDeclarations.forField(field).index
                val fieldType = LLVMStructGetTypeAtIndex(type, fieldIndex)!!

                if (isObjectType(fieldType)) {
                    val fieldPtr = LLVMBuildStructGEP(builder, stackLocal.bodyPtr, fieldIndex, "")!!
                    if (refsOnly)
                        storeHeapRef(kNullObjHeaderPtr, fieldPtr)
                    else
                        call(context.llvm.zeroHeapRefFunction, listOf(fieldPtr))
                }
            }

            if (!refsOnly) {
                val bodyPtr = ptrToInt(stackLocal.bodyPtr, codegen.intPtrType)
                val bodySize = LLVMSizeOfTypeInBits(codegen.llvmTargetData, type).toInt() / 8
                val serviceInfoSize = runtime.pointerSize
                val serviceInfoSizeLlvm = LLVMConstInt(codegen.intPtrType, serviceInfoSize.toLong(), 1)!!
                val bodyWithSkippedServiceInfoPtr = intToPtr(add(bodyPtr, serviceInfoSizeLlvm), kInt8Ptr)
                call(context.llvm.memsetFunction,
                        listOf(bodyWithSkippedServiceInfoPtr,
                                Int8(0).llvm,
                                Int32(bodySize - serviceInfoSize).llvm,
                                Int1(0).llvm))
            }
        }
    }

    private fun setTypeInfoForLocalObject(objectHeader: LLVMValueRef, typeInfoPointer: LLVMValueRef) = with(functionGenerationContext) {
        val typeInfo = structGep(objectHeader, 0, "typeInfoOrMeta_")
        // Set tag OBJECT_TAG_PERMANENT_CONTAINER | OBJECT_TAG_NONTRIVIAL_CONTAINER.
        val typeInfoValue = intToPtr(or(ptrToInt(typeInfoPointer, codegen.intPtrType),
                codegen.immThreeIntPtrType), kTypeInfoPtr)
        store(typeInfoValue, typeInfo)
    }
}

internal class FunctionGenerationContext(val function: LLVMValueRef,
                                         val codegen: CodeGenerator,
                                         startLocation: LocationInfo?,
                                         endLocation: LocationInfo?,
                                         internal val irFunction: IrFunction? = null): ContextUtils {

    override val context = codegen.context
    val vars = VariableManager(this)
    private val basicBlockToLastLocation = mutableMapOf<LLVMBasicBlockRef, LocationInfoRange>()

    private fun update(block: LLVMBasicBlockRef, startLocationInfo: LocationInfo?, endLocation: LocationInfo? = startLocationInfo) {
        startLocationInfo ?: return
        basicBlockToLastLocation.put(block, LocationInfoRange(startLocationInfo, endLocation))
    }

    var returnType: LLVMTypeRef? = LLVMGetReturnType(getFunctionType(function))
    private val returns: MutableMap<LLVMBasicBlockRef, LLVMValueRef> = mutableMapOf()
    val constructedClass: IrClass?
        get() = (irFunction as? IrConstructor)?.constructedClass
    private var returnSlot: LLVMValueRef? = null
    private var slotsPhi: LLVMValueRef? = null
    private val frameOverlaySlotCount =
            (LLVMStoreSizeOfType(llvmTargetData, runtime.frameOverlayType) / runtime.pointerSize).toInt()
    private var slotCount = frameOverlaySlotCount
    private var localAllocs = 0
    // TODO: remove if exactly unused.
    //private var arenaSlot: LLVMValueRef? = null
    private val slotToVariableLocation = mutableMapOf<Int, VariableDebugLocation>()

    private val prologueBb = basicBlockInFunction("prologue", startLocation)
    private val localsInitBb = basicBlockInFunction("locals_init", startLocation)
    private val stackLocalsInitBb = basicBlockInFunction("stack_locals_init", startLocation)
    private val entryBb = basicBlockInFunction("entry", startLocation)
    private val epilogueBb = basicBlockInFunction("epilogue", endLocation)
    private val cleanupLandingpad = basicBlockInFunction("cleanup_landingpad", endLocation)

    val stackLocalsManager = StackLocalsManagerImpl(this, stackLocalsInitBb)

    /**
     * TODO: consider merging this with [ExceptionHandler].
     */
    var forwardingForeignExceptionsTerminatedWith: LLVMValueRef? = null

    // Whether the generating function needs to initialize Kotlin runtime before execution. Useful for interop bridges,
    // for example.
    var needsRuntimeInit = false

    // Marks that function is not allowed to call into Kotlin runtime. For this function no safepoints, no enter/leave
    // frames are generated.
    // TODO: Should forbid all calls into runtime except for explicitly allowed. Also should impose the same restriction
    //       on function being called from this one.
    // TODO: Consider using a different abstraction than `FunctionGenerationContext`.
    var forbidRuntime = false

    init {
        irFunction?.let {
            if (!irFunction.isExported()) {
                LLVMSetLinkage(function, LLVMLinkage.LLVMInternalLinkage)
                // (Cannot do this before the function body is created).
            }
        }
    }

    fun dispose() {
        currentPositionHolder.dispose()
    }

    private fun basicBlockInFunction(name: String, locationInfo: LocationInfo?): LLVMBasicBlockRef {
        val bb = LLVMAppendBasicBlockInContext(llvmContext, function, name)!!
        update(bb, locationInfo)
        return bb
    }

    fun basicBlock(name: String = "label_", startLocationInfo: LocationInfo?, endLocationInfo: LocationInfo? = startLocationInfo): LLVMBasicBlockRef {
        val result = LLVMInsertBasicBlockInContext(llvmContext, this.currentBlock, name)!!
        update(result, startLocationInfo, endLocationInfo)
        LLVMMoveBasicBlockAfter(result, this.currentBlock)
        return result
    }

    fun alloca(type: LLVMTypeRef?, name: String = "", variableLocation: VariableDebugLocation? = null): LLVMValueRef {
        if (isObjectType(type!!)) {
            appendingTo(localsInitBb) {
                val slotAddress = gep(slotsPhi!!, Int32(slotCount).llvm, name)
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
                        builder = codegen.context.debugInfo.builder,
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


    fun ret(value: LLVMValueRef?): LLVMValueRef {
        val res = LLVMBuildBr(builder, epilogueBb)!!
        if (returns.containsKey(currentBlock)) {
            // TODO: enable error throwing.
            throw Error("ret() in the same basic block twice! in ${function.name}")
        }

        if (value != null)
            returns[currentBlock] = value

        currentPositionHolder.setAfterTerminator()
        return res
    }

    fun param(index: Int): LLVMValueRef = LLVMGetParam(this.function, index)!!

    fun load(value: LLVMValueRef, name: String = ""): LLVMValueRef {
        val result = LLVMBuildLoad(builder, value, name)!!
        // Use loadSlot() API for that.
        assert(!isObjectRef(value))
        return result
    }

    fun loadSlot(address: LLVMValueRef, isVar: Boolean, name: String = ""): LLVMValueRef {
        val value = LLVMBuildLoad(builder, address, name)!!
        if (isObjectRef(value) && isVar) {
            val slot = alloca(LLVMTypeOf(value), variableLocation = null)
            storeStackRef(value, slot)
        }
        return value
    }

    fun store(value: LLVMValueRef, ptr: LLVMValueRef) {
        LLVMBuildStore(builder, value, ptr)
    }

    fun storeHeapRef(value: LLVMValueRef, ptr: LLVMValueRef) {
        updateRef(value, ptr, onStack = false)
    }

    fun storeStackRef(value: LLVMValueRef, ptr: LLVMValueRef) {
        updateRef(value, ptr, onStack = true)
    }

    fun storeAny(value: LLVMValueRef, ptr: LLVMValueRef, onStack: Boolean) = if (isObjectRef(value)) {
            if (onStack) storeStackRef(value, ptr) else storeHeapRef(value, ptr)
            null
        } else {
            LLVMBuildStore(builder, value, ptr)
        }

    fun freeze(value: LLVMValueRef, exceptionHandler: ExceptionHandler) {
        if (isObjectRef(value))
            call(context.llvm.freezeSubgraph, listOf(value), Lifetime.IRRELEVANT, exceptionHandler)
    }

    fun checkGlobalsAccessible(exceptionHandler: ExceptionHandler) {
        if (context.memoryModel == MemoryModel.STRICT)
            call(context.llvm.checkGlobalsAccessible, emptyList(), Lifetime.IRRELEVANT, exceptionHandler)
    }

    private fun updateReturnRef(value: LLVMValueRef, address: LLVMValueRef) {
        if (context.memoryModel == MemoryModel.STRICT)
            store(value, address)
        else
            call(context.llvm.updateReturnRefFunction, listOf(address, value))
    }

    private fun updateRef(value: LLVMValueRef, address: LLVMValueRef, onStack: Boolean) {
        if (onStack) {
            if (context.memoryModel == MemoryModel.STRICT)
                store(value, address)
            else
                call(context.llvm.updateStackRefFunction, listOf(address, value))
        } else {
            call(context.llvm.updateHeapRefFunction, listOf(address, value))
        }
    }

    //-------------------------------------------------------------------------//

    fun call(llvmFunction: LLVMValueRef, args: List<LLVMValueRef>,
             resultLifetime: Lifetime = Lifetime.IRRELEVANT,
             exceptionHandler: ExceptionHandler = ExceptionHandler.None,
             verbatim: Boolean = false): LLVMValueRef {
        val callArgs = if (verbatim || !isObjectReturn(llvmFunction.type)) {
            args
        } else {
            // If function returns an object - create slot for the returned value or give local arena.
            // This allows appropriate rootset accounting by just looking at the stack slots,
            // along with ability to allocate in appropriate arena.
            val resultSlot = when (resultLifetime.slotType) {
                SlotType.STACK -> {
                    localAllocs++
                    // Case of local call. Use memory allocated on stack.
                    val type = LLVMGetReturnType(LLVMGetElementType(llvmFunction.type))!!
                    val stackPointer = alloca(type)
                    //val objectHeader = structGep(stackPointer, 0)
                    //setTypeInfoForLocalObject(objectHeader)
                    stackPointer
                    //arenaSlot!!
                }

                SlotType.RETURN -> returnSlot!!

                SlotType.ANONYMOUS -> vars.createAnonymousSlot()

                else -> throw Error("Incorrect slot type: ${resultLifetime.slotType}")
            }
            args + resultSlot
        }
        return callRaw(llvmFunction, callArgs, exceptionHandler)
    }

    private fun callRaw(llvmFunction: LLVMValueRef, args: List<LLVMValueRef>,
                        exceptionHandler: ExceptionHandler): LLVMValueRef {
        val rargs = args.toCValues()
        if (LLVMIsAFunction(llvmFunction) != null /* the function declaration */ &&
                isFunctionNoUnwind(llvmFunction)) {
            return LLVMBuildCall(builder, llvmFunction, rargs, args.size, "")!!
        } else {
            val unwind = when (exceptionHandler) {
                ExceptionHandler.Caller -> cleanupLandingpad
                is ExceptionHandler.Local -> exceptionHandler.unwind

                ExceptionHandler.None -> {
                    // When calling a function that is not marked as nounwind (can throw an exception),
                    // it is required to specify an unwind label to handle exceptions properly.
                    // Runtime C++ function can be marked as non-throwing using `RUNTIME_NOTHROW`.
                    val functionName = llvmFunction.name
                    val message =
                            "no exception handler specified when calling function $functionName without nounwind attr"
                    throw IllegalArgumentException(message)
                }
            }

            val position = position()
            val endLocation = position?.end
            val success = basicBlock("call_success", endLocation)
            val result = LLVMBuildInvoke(builder, llvmFunction, rargs, args.size, success, unwind, "")!!
            update(success, endLocation)
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

    fun allocInstance(typeInfo: LLVMValueRef, lifetime: Lifetime): LLVMValueRef =
            call(context.llvm.allocInstanceFunction, listOf(typeInfo), lifetime)

    fun allocInstance(irClass: IrClass, lifetime: Lifetime, stackLocalsManager: StackLocalsManager) =
            if (lifetime == Lifetime.STACK)
                stackLocalsManager.alloc(irClass,
                        // In case the allocation is not from the root scope, fields must be cleaned up explicitly,
                        // as the object might be being reused.
                        cleanFieldsExplicitly = stackLocalsManager != this.stackLocalsManager)
            else
                allocInstance(codegen.typeInfoForAllocation(irClass), lifetime)

    fun allocArray(
        irClass: IrClass,
        count: LLVMValueRef,
        lifetime: Lifetime,
        exceptionHandler: ExceptionHandler
    ): LLVMValueRef {
        val typeInfo = codegen.typeInfoValue(irClass)
        return if (lifetime == Lifetime.STACK) {
            stackLocalsManager.allocArray(irClass, count)
        } else {
            call(context.llvm.allocArrayFunction, listOf(typeInfo, count), lifetime, exceptionHandler)
        }
    }

    fun unreachable(): LLVMValueRef? {
        if (context.config.debug) {
            call(context.llvm.llvmTrap, emptyList())
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
        return LLVMBlockAddress(function, bbLabel)!!
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
    fun gep(base: LLVMValueRef, index: LLVMValueRef, name: String = ""): LLVMValueRef {
        return LLVMBuildGEP(builder, base, cValuesOf(index), 1, name)!!
    }
    fun structGep(base: LLVMValueRef, index: Int, name: String = ""): LLVMValueRef =
            LLVMBuildStructGEP(builder, base, index, name)!!

    fun extractValue(aggregate: LLVMValueRef, index: Int, name: String = ""): LLVMValueRef =
            LLVMBuildExtractValue(builder, aggregate, index, name)!!

    fun gxxLandingpad(numClauses: Int, name: String = ""): LLVMValueRef {
        val personalityFunction = context.llvm.gxxPersonalityFunction

        // Type of `landingpad` instruction result (depends on personality function):
        val landingpadType = structType(int8TypePtr, int32Type)

        return LLVMBuildLandingPad(builder, landingpadType, personalityFunction, numClauses, name)!!
    }

    fun extractElement(vector: LLVMValueRef, index: LLVMValueRef, name: String = ""): LLVMValueRef {
        return LLVMBuildExtractElement(builder, vector, index, name)!!
    }

    fun filteringExceptionHandler(codeContext: CodeContext, foreignExceptionMode: ForeignExceptionMode.Mode): ExceptionHandler {
        val lpBlock = basicBlockInFunction("filteringExceptionHandler", position()?.start)

        val wrapExceptionMode = context.config.target.family.isAppleFamily &&
                foreignExceptionMode == ForeignExceptionMode.Mode.OBJC_WRAP

        appendingTo(lpBlock) {
            val landingpad = gxxLandingpad(2)
            LLVMAddClause(landingpad, kotlinExceptionRtti.llvm)
            if (wrapExceptionMode) {
                LLVMAddClause(landingpad, objcNSExceptionRtti.llvm)
            }
            LLVMAddClause(landingpad, LLVMConstNull(kInt8Ptr))

            val fatalForeignExceptionBlock = basicBlock("fatalForeignException", position()?.start)
            val forwardKotlinExceptionBlock = basicBlock("forwardKotlinException", position()?.start)

            val typeId = extractValue(landingpad, 1)
            val isKotlinException = icmpEq(
                    typeId,
                    call(context.llvm.llvmEhTypeidFor, listOf(kotlinExceptionRtti.llvm))
            )

            if (wrapExceptionMode) {
                val foreignExceptionBlock = basicBlock("foreignException", position()?.start)
                val forwardNativeExceptionBlock = basicBlock("forwardNativeException", position()?.start)

                condBr(isKotlinException, forwardKotlinExceptionBlock, foreignExceptionBlock)
                appendingTo(foreignExceptionBlock) {
                    val isObjCException = icmpEq(
                            typeId,
                            call(context.llvm.llvmEhTypeidFor, listOf(objcNSExceptionRtti.llvm))
                    )
                    condBr(isObjCException, forwardNativeExceptionBlock, fatalForeignExceptionBlock)

                    appendingTo(forwardNativeExceptionBlock) {
                        val exception = createForeignException(landingpad, codeContext.exceptionHandler)
                        codeContext.genThrow(exception)
                    }
                }
            } else {
                condBr(isKotlinException, forwardKotlinExceptionBlock, fatalForeignExceptionBlock)
            }

            appendingTo(forwardKotlinExceptionBlock) {
                // Rethrow Kotlin exception to real handler.
                codeContext.genThrow(extractKotlinException(landingpad))
            }

            appendingTo(fatalForeignExceptionBlock) {
                val exceptionRecord = extractValue(landingpad, 0)
                call(context.llvm.cxaBeginCatchFunction, listOf(exceptionRecord))
                terminate()
            }

        }

        return object : ExceptionHandler.Local() {
            override val unwind: LLVMBasicBlockRef
                get() = lpBlock
        }
    }

    fun terminate() {
        call(context.llvm.cxxStdTerminate, emptyList())

        // Note: unreachable instruction to be generated here, but debug information is improper in this case.
        val loopBlock = basicBlock("loop", position()?.start)
        br(loopBlock)
        appendingTo(loopBlock) {
            br(loopBlock)
        }
    }

    fun kotlinExceptionHandler(
            block: FunctionGenerationContext.(exception: LLVMValueRef) -> Unit
    ): ExceptionHandler {
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

        LLVMAddClause(landingpadResult, LLVMConstNull(kInt8Ptr))

        // TODO: properly handle C++ exceptions: currently C++ exception can be thrown out from try-finally
        // bypassing the finally block.

        return extractKotlinException(landingpadResult)
    }

    private fun extractKotlinException(landingpadResult: LLVMValueRef): LLVMValueRef {
        val exceptionRecord = extractValue(landingpadResult, 0, "er")

        // __cxa_begin_catch returns pointer to C++ exception object.
        val beginCatch = context.llvm.cxaBeginCatchFunction
        val exceptionRawPtr = call(beginCatch, listOf(exceptionRecord))

        // Pointer to Kotlin exception object:
        val exceptionPtr = call(context.llvm.Kotlin_getExceptionObject, listOf(exceptionRawPtr), Lifetime.GLOBAL)

        // __cxa_end_catch performs some C++ cleanup, including calling `ExceptionObjHolder` class destructor.
        val endCatch = context.llvm.cxaEndCatchFunction
        call(endCatch, listOf())

        return exceptionPtr
    }

    private fun createForeignException(landingpadResult: LLVMValueRef, exceptionHandler: ExceptionHandler): LLVMValueRef {
        val exceptionRecord = extractValue(landingpadResult, 0, "er")

        // __cxa_begin_catch returns pointer to C++ exception object.
        val exceptionRawPtr = call(context.llvm.cxaBeginCatchFunction, listOf(exceptionRecord))

        // This will take care of ARC - need to be done in the catching scope, i.e. before __cxa_end_catch
        val exception = call(context.ir.symbols.createForeignException.owner.llvmFunction,
                listOf(exceptionRawPtr),
                Lifetime.GLOBAL, exceptionHandler)

        call(context.llvm.cxaEndCatchFunction, listOf())
        return exception
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
        val typeInfoOrMetaPtr = structGep(objPtr, 0  /* typeInfoOrMeta_ */)
        val typeInfoOrMetaWithFlags = load(typeInfoOrMetaPtr)
        // Clear two lower bits.
        val typeInfoOrMetaWithFlagsRaw = ptrToInt(typeInfoOrMetaWithFlags, codegen.intPtrType)
        val typeInfoOrMetaRaw = and(typeInfoOrMetaWithFlagsRaw, codegen.immTypeInfoMask)
        val typeInfoOrMeta = intToPtr(typeInfoOrMetaRaw, kTypeInfoPtr)
        val typeInfoPtrPtr = structGep(typeInfoOrMeta, 0 /* typeInfo */)
        return load(typeInfoPtrPtr)
    }

    fun lookupInterfaceTableRecord(typeInfo: LLVMValueRef, interfaceId: Int): LLVMValueRef {
        val interfaceTableSize = load(structGep(typeInfo, 11 /* interfaceTableSize_ */))
        val interfaceTable = load(structGep(typeInfo, 12 /* interfaceTable_ */))

        fun fastPath(): LLVMValueRef {
            // The fastest optimistic version.
            val interfaceTableIndex = and(interfaceTableSize, Int32(interfaceId).llvm)
            return gep(interfaceTable, interfaceTableIndex)
        }

        // See details in ClassLayoutBuilder.
        return if (context.globalHierarchyAnalysisResult.bitsPerColor <= ClassGlobalHierarchyInfo.MAX_BITS_PER_COLOR
                && context.config.produce != CompilerOutputKind.FRAMEWORK) {
            // All interface tables are small and no unknown interface inheritance.
            fastPath()
        } else {
            val startLocationInfo = position()?.start
            val fastPathBB = basicBlock("fast_path", startLocationInfo)
            val slowPathBB = basicBlock("slow_path", startLocationInfo)
            val takeResBB = basicBlock("take_res", startLocationInfo)
            condBr(icmpGe(interfaceTableSize, kImmInt32Zero), fastPathBB, slowPathBB)
            positionAtEnd(takeResBB)
            val resultPhi = phi(pointerType(runtime.interfaceTableRecordType))
            appendingTo(fastPathBB) {
                val fastValue = fastPath()
                br(takeResBB)
                addPhiIncoming(resultPhi, currentBlock to fastValue)
            }
            appendingTo(slowPathBB) {
                val actualInterfaceTableSize = sub(kImmInt32Zero, interfaceTableSize) // -interfaceTableSize
                val slowValue = call(context.llvm.lookupInterfaceTableRecord,
                        listOf(interfaceTable, actualInterfaceTableSize, Int32(interfaceId).llvm))
                br(takeResBB)
                addPhiIncoming(resultPhi, currentBlock to slowValue)
            }
            resultPhi
        }
    }

    fun lookupVirtualImpl(receiver: LLVMValueRef, irFunction: IrFunction): LLVMValueRef {
        assert(LLVMTypeOf(receiver) == codegen.kObjHeaderPtr)

        val typeInfoPtr: LLVMValueRef = if (irFunction.getObjCMethodInfo() != null)
            call(context.llvm.getObjCKotlinTypeInfo, listOf(receiver))
        else
            loadTypeInfo(receiver)

        assert(typeInfoPtr.type == codegen.kTypeInfoPtr) { LLVMPrintTypeToString(typeInfoPtr.type)!!.toKString() }

        /*
         * Resolve owner of the call with special handling of Any methods:
         * if toString/eq/hc is invoked on an interface instance, we resolve
         * owner as Any and dispatch it via vtable.
         */
        val anyMethod = (irFunction as IrSimpleFunction).findOverriddenMethodOfAny()
        val owner = (anyMethod ?: irFunction).parentAsClass
        val methodHash = codegen.functionHash(irFunction)

        val llvmMethod = when {
            !owner.isInterface -> {
                // If this is a virtual method of the class - we can call via vtable.
                val index = context.getLayoutBuilder(owner).vtableIndex(anyMethod ?: irFunction)
                val vtablePlace = gep(typeInfoPtr, Int32(1).llvm) // typeInfoPtr + 1
                val vtable = bitcast(kInt8PtrPtr, vtablePlace)
                val slot = gep(vtable, Int32(index).llvm)
                load(slot)
            }

            !context.ghaEnabled() -> call(context.llvm.lookupOpenMethodFunction, listOf(typeInfoPtr, methodHash))

            else -> {
                // Essentially: typeInfo.itable[place(interfaceId)].vtable[method]
                val itablePlace = context.getLayoutBuilder(owner).itablePlace(irFunction)
                val interfaceTableRecord = lookupInterfaceTableRecord(typeInfoPtr, itablePlace.interfaceId)
                load(gep(load(structGep(interfaceTableRecord, 2 /* vtable */)), Int32(itablePlace.methodIndex).llvm))
            }
        }
        val functionPtrType = pointerType(codegen.getLlvmFunctionType(irFunction))
        return bitcast(functionPtrType, llvmMethod)
    }

    private fun IrSimpleFunction.findOverriddenMethodOfAny(): IrSimpleFunction? {
        if (modality == Modality.ABSTRACT) return null
        val resolved = resolveFakeOverride()!!
        if ((resolved.parent as IrClass).isAny()) {
            return resolved
        }

        return null
    }

    fun getObjectValue(irClass: IrClass, exceptionHandler: ExceptionHandler,
            startLocationInfo: LocationInfo?, endLocationInfo: LocationInfo? = null
    ): LLVMValueRef {
        // TODO: could be processed the same way as other stateless objects.
        if (irClass.isUnit()) {
            return codegen.theUnitInstanceRef.llvm
        }

        if (irClass.isCompanion) {
            val parent = irClass.parent as IrClass
            if (parent.isObjCClass()) {
                // TODO: cache it too.
                return call(
                        codegen.llvmFunction(context.ir.symbols.interopInterpretObjCPointer.owner),
                        listOf(getObjCClass(parent, exceptionHandler)),
                        Lifetime.GLOBAL,
                        exceptionHandler
                )
            }
        }

        val storageKind = irClass.storageKind(context)

        val objectPtr = if (isExternal(irClass)) {
            when (storageKind) {
                // If thread local object is imported - access it via getter function.
                ObjectStorageKind.THREAD_LOCAL -> {
                    val valueGetterName = irClass.threadLocalObjectStorageGetterSymbolName
                    val valueGetterFunction = LLVMGetNamedFunction(context.llvmModule, valueGetterName)
                            ?: addLlvmFunctionWithDefaultAttributes(
                                    context,
                                    context.llvmModule!!,
                                    valueGetterName,
                                    functionType(kObjHeaderPtrPtr, false)
                            )
                    call(valueGetterFunction,
                            listOf(),
                            resultLifetime = Lifetime.GLOBAL,
                            exceptionHandler = exceptionHandler)
                }

                // If global object is imported - import it's storage directly.
                ObjectStorageKind.PERMANENT, ObjectStorageKind.SHARED -> {
                    val llvmType = getLLVMType(irClass.defaultType)
                    importGlobal(
                            irClass.globalObjectStorageSymbolName,
                            llvmType,
                            origin = irClass.llvmSymbolOrigin
                    )
                }
            }
        } else {
            // Local globals and thread locals storage info is stored in our map.
            val singleton = context.llvmDeclarations.forSingleton(irClass)
            val instanceAddress = singleton.instanceStorage
            instanceAddress.getAddress(this)
        }

        when (storageKind) {
            ObjectStorageKind.SHARED ->
                // If current file used a shared object, make file's (de)initializer function deinit it.
                context.llvm.globalSharedObjects += objectPtr
            ObjectStorageKind.THREAD_LOCAL ->
                // If current file used locally defined TLS objects, make file's (de)initializer function
                // init and deinit TLS.
                // Note: for exported TLS objects a getter is generated in a file they're defined in. Which
                // adds TLS init and deinit to that file's (de)initializer function.
                if (!isExternal(irClass))
                    context.llvm.fileUsesThreadLocalObjects = true
            ObjectStorageKind.PERMANENT -> { /* Do nothing, no need to free such an instance. */ }
        }

        if (storageKind == ObjectStorageKind.PERMANENT) {
            return loadSlot(objectPtr, false)
        }
        val bbInit = basicBlock("label_init", startLocationInfo, endLocationInfo)
        val bbExit = basicBlock("label_continue", startLocationInfo, endLocationInfo)
        val objectVal = loadSlot(objectPtr, false)
        val objectInitialized = icmpUGt(ptrToInt(objectVal, codegen.intPtrType), codegen.immOneIntPtrType)
        val bbCurrent = currentBlock
        condBr(objectInitialized, bbExit, bbInit)

        positionAtEnd(bbInit)
        val typeInfo = codegen.typeInfoForAllocation(irClass)
        val defaultConstructor = irClass.constructors.single { it.valueParameters.size == 0 }
        val ctor = codegen.llvmFunction(defaultConstructor)
        val initFunction =
                if (storageKind == ObjectStorageKind.SHARED && context.config.threadsAreAllowed) {
                    context.llvm.initSingletonFunction
                } else {
                    context.llvm.initThreadLocalSingleton
                }
        val args = listOf(objectPtr, typeInfo, ctor)
        val newValue = call(initFunction, args, Lifetime.GLOBAL, exceptionHandler)
        val bbInitResult = currentBlock
        br(bbExit)

        positionAtEnd(bbExit)
        val valuePhi = phi(codegen.getLLVMType(irClass.defaultType))
        addPhiIncoming(valuePhi, bbCurrent to objectVal, bbInitResult to newValue)

        return valuePhi
    }

    /**
     * Note: the same code is generated as IR in [org.jetbrains.kotlin.backend.konan.lower.EnumUsageLowering].
     */
    fun getEnumEntry(enumEntry: IrEnumEntry, exceptionHandler: ExceptionHandler): LLVMValueRef {
        val enumClass = enumEntry.parentAsClass
        val loweredEnum = context.specialDeclarationsFactory.getLoweredEnum(enumClass)

        val ordinal = loweredEnum.entriesMap[enumEntry.name]!!
        val values = call(
                loweredEnum.valuesGetter.llvmFunction,
                emptyList(),
                Lifetime.ARGUMENT,
                exceptionHandler
        )

        return call(
                loweredEnum.itemGetterSymbol.owner.llvmFunction,
                listOf(values, Int32(ordinal).llvm),
                Lifetime.GLOBAL,
                exceptionHandler
        )
    }

    // TODO: get rid of exceptionHandler argument by ensuring that all called functions are non-throwing.
    fun getObjCClass(irClass: IrClass, exceptionHandler: ExceptionHandler): LLVMValueRef {
        assert(!irClass.isInterface)

        return if (irClass.isExternalObjCClass()) {
            val llvmSymbolOrigin = irClass.llvmSymbolOrigin

            if (irClass.isObjCMetaClass()) {
                val name = irClass.descriptor.getExternalObjCMetaClassBinaryName()
                val objCClass = getObjCClass(name, llvmSymbolOrigin)

                val getClass = context.llvm.externalFunction(
                        "object_getClass",
                        functionType(int8TypePtr, false, int8TypePtr),
                        origin = context.standardLlvmSymbolsOrigin
                )

                call(getClass, listOf(objCClass), exceptionHandler = exceptionHandler)
            } else {
                getObjCClass(irClass.descriptor.getExternalObjCClassBinaryName(), llvmSymbolOrigin)
            }
        } else {
            if (irClass.isObjCMetaClass()) {
                error("type-checking against Kotlin classes inheriting Objective-C meta-classes isn't supported yet")
            }

            val classInfo = codegen.kotlinObjCClassInfo(irClass)
            val classPointerGlobal = load(structGep(classInfo, KotlinObjCClassInfoGenerator.createdClassFieldIndex))

            val storedClass = this.load(classPointerGlobal)

            val storedClassIsNotNull = this.icmpNe(storedClass, kNullInt8Ptr)

            return this.ifThenElse(storedClassIsNotNull, storedClass) {
                call(
                        context.llvm.createKotlinObjCClass,
                        listOf(classInfo),
                        exceptionHandler = exceptionHandler
                )
            }
        }
    }

    fun getObjCClass(binaryName: String, llvmSymbolOrigin: CompiledKlibModuleOrigin): LLVMValueRef {
        context.llvm.imports.add(llvmSymbolOrigin)
        return load(codegen.objCDataGenerator!!.genClassRef(binaryName).llvm)
    }

    fun resetDebugLocation() {
        if (!context.shouldContainLocationDebugInfo()) return
        currentPositionHolder.resetBuilderDebugLocation()
    }

    private fun position() = basicBlockToLastLocation[currentBlock]

    internal fun mapParameterForDebug(index: Int, value: LLVMValueRef) {
        appendingTo(localsInitBb) {
            LLVMBuildStore(builder, value, vars.addressOf(index))
        }
    }

    internal fun prologue() {
        assert(returns.isEmpty())
        if (isObjectType(returnType!!)) {
            returnSlot = LLVMGetParam(function, numParameters(function.type) - 1)
        }
        positionAtEnd(localsInitBb)
        slotsPhi = phi(kObjHeaderPtrPtr)
        // Is removed by DCE trivially, if not needed.
        /*arenaSlot = intToPtr(
                or(ptrToInt(slotsPhi, codegen.intPtrType), codegen.immOneIntPtrType), kObjHeaderPtrPtr)*/
        positionAtEnd(entryBb)
    }

    internal fun epilogue() {
        appendingTo(prologueBb) {
            if (needsRuntimeInit) {
                check(!forbidRuntime) { "Attempt to init runtime where runtime usage is forbidden" }
                call(context.llvm.initRuntimeIfNeeded, emptyList())
            }
            val slots = if (needSlotsPhi)
                LLVMBuildArrayAlloca(builder, kObjHeaderPtr, Int32(slotCount).llvm, "")!!
            else
                kNullObjHeaderPtrPtr
            if (needSlots) {
                check(!forbidRuntime) { "Attempt to start a frame where runtime usage is forbidden" }
                // Zero-init slots.
                val slotsMem = bitcast(kInt8Ptr, slots)
                call(context.llvm.memsetFunction,
                        listOf(slotsMem, Int8(0).llvm,
                                Int32(slotCount * codegen.runtime.pointerSize).llvm,
                                Int1(0).llvm))
                call(context.llvm.enterFrameFunction, listOf(slots, Int32(vars.skipSlots).llvm, Int32(slotCount).llvm))
            }
            addPhiIncoming(slotsPhi!!, prologueBb to slots)
            memScoped {
                slotToVariableLocation.forEach { slot, variable ->
                    val expr = longArrayOf(DwarfOp.DW_OP_plus_uconst.value,
                            runtime.pointerSize * slot.toLong()).toCValues()
                    DIInsertDeclaration(
                            builder       = codegen.context.debugInfo.builder,
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

        appendingTo(stackLocalsInitBb) {
            br(entryBb)
        }

        appendingTo(epilogueBb) {
            when {
                returnType == voidType -> {
                    releaseVars()
                    assert(returnSlot == null)
                    if (!forbidRuntime && context.memoryModel == MemoryModel.EXPERIMENTAL)
                        call(context.llvm.Kotlin_mm_safePointFunctionEpilogue, emptyList())
                    LLVMBuildRetVoid(builder)
                }
                returns.isNotEmpty() -> {
                    val returnPhi = phi(returnType!!)
                    addPhiIncoming(returnPhi, *returns.toList().toTypedArray())
                    if (returnSlot != null) {
                        updateReturnRef(returnPhi, returnSlot!!)
                    }
                    releaseVars()
                    if (!forbidRuntime && context.memoryModel == MemoryModel.EXPERIMENTAL)
                        call(context.llvm.Kotlin_mm_safePointFunctionEpilogue, emptyList())
                    LLVMBuildRet(builder, returnPhi)
                }
                // Do nothing, all paths throw.
                else -> LLVMBuildUnreachable(builder)
            }
        }

        appendingTo(cleanupLandingpad) {
            val landingpad = gxxLandingpad(numClauses = 0)
            LLVMSetCleanup(landingpad, 1)

            forwardingForeignExceptionsTerminatedWith?.let { terminator ->
                // Catch all but Kotlin exceptions.
                val clause = ConstArray(int8TypePtr, listOf(kotlinExceptionRtti))
                LLVMAddClause(landingpad, clause.llvm)

                val bbCleanup = basicBlock("forwardException", position()?.end)
                val bbUnexpected = basicBlock("unexpectedException", position()?.end)

                val selector = extractValue(landingpad, 1)
                condBr(
                        icmpLt(selector, Int32(0).llvm),
                        bbUnexpected,
                        bbCleanup
                )

                appendingTo(bbUnexpected) {
                    val exceptionRecord = extractValue(landingpad, 0)

                    val beginCatch = context.llvm.cxaBeginCatchFunction
                    // So `terminator` is called from C++ catch block:
                    call(beginCatch, listOf(exceptionRecord))
                    call(terminator, emptyList())
                    unreachable()
                }

                positionAtEnd(bbCleanup)
            }

            releaseVars()
            if (!forbidRuntime && context.memoryModel == MemoryModel.EXPERIMENTAL)
                call(context.llvm.Kotlin_mm_safePointExceptionUnwind, emptyList())
            LLVMBuildResume(builder, landingpad)
        }

        returns.clear()
        vars.clear()
        returnSlot = null
        slotsPhi = null
    }

    private val kotlinExceptionRtti: ConstPointer
        get() = constPointer(importGlobal(
                "_ZTI18ExceptionObjHolder", // typeinfo for ObjHolder
                int8TypePtr,
                origin = context.stdlibModule.llvmSymbolOrigin
        )).bitcast(int8TypePtr)

    private val objcNSExceptionRtti: ConstPointer by lazy {
        constPointer(importGlobal(
                "OBJC_EHTYPE_\$_NSException", // typeinfo for NSException*
                int8TypePtr,
                origin = context.stdlibModule.llvmSymbolOrigin
        )).bitcast(int8TypePtr)
    }

    //-------------------------------------------------------------------------//

    /**
     * Represents the mutable position of instructions being inserted.
     *
     * This class is introduced to workaround unreachable code handling.
     */
    inner class PositionHolder {
        private val builder: LLVMBuilderRef = LLVMCreateBuilderInContext(llvmContext)!!


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
        if (needSlots) {
            check(!forbidRuntime) { "Attempt to leave a frame where runtime usage is forbidden" }
            call(context.llvm.leaveFrameFunction,
                    listOf(slotsPhi!!, Int32(vars.skipSlots).llvm, Int32(slotCount).llvm))
        }
        stackLocalsManager.clean(refsOnly = true) // Only bother about not leaving any dangling references.
    }
}



