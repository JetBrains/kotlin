/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.backend.konan.Context
import org.jetbrains.kotlin.descriptors.konan.CompiledKlibModuleOrigin

private val llvmContextHolder = ThreadLocal<LLVMContextRef>()
internal var llvmContext: LLVMContextRef
    get() = llvmContextHolder.get()
    set(value) { llvmContextHolder.set(value) }

internal fun tryDisposeLLVMContext() {
    val llvmContext = llvmContextHolder.get()
    if (llvmContext != null)
        LLVMContextDispose(llvmContext)
    llvmContextHolder.remove()
}

internal val LLVMTypeRef.context: LLVMContextRef
    get() = LLVMGetTypeContext(this)!!

internal val List<LLVMTypeRef>.context: LLVMContextRef
    get() {
        val context = this[0].context
        for (i in 1 until this.size)
            assert(this[i].context == context) {
                "Expected the same context for all types in a list"
            }
        return context
    }

internal val LLVMValueRef.type: LLVMTypeRef
    get() = LLVMTypeOf(this)!!

/**
 * Represents the value which can be emitted as bitcode const value
 */
internal interface ConstValue {
    val llvm: LLVMValueRef
}

internal val ConstValue.llvmType: LLVMTypeRef
    get() = this.llvm.type

internal interface ConstPointer : ConstValue {
    fun getElementPtr(index: Int): ConstPointer = ConstGetElementPtr(this, index)
}

internal fun constPointer(value: LLVMValueRef) = object : ConstPointer {
    init {
        assert(LLVMIsConstant(value) == 1)
    }

    override val llvm = value
}

private class ConstGetElementPtr(val pointer: ConstPointer, val index: Int) : ConstPointer {
    override val llvm = LLVMConstInBoundsGEP(pointer.llvm, cValuesOf(Int32(0).llvm, Int32(index).llvm), 2)!!
    // TODO: squash multiple GEPs
}

internal fun ConstPointer.bitcast(toType: LLVMTypeRef) = constPointer(LLVMConstBitCast(this.llvm, toType)!!)

internal class ConstArray(elementType: LLVMTypeRef?, val elements: List<ConstValue>) : ConstValue {
    init {
        elements.forEach {
            assert(it.llvmType == elementType) {
                "Expected element type: ${llvmtype2string(elementType)}, actual: ${llvmtype2string(it.llvmType)}"
            }
        }
    }
    override val llvm = LLVMConstArray(elementType, elements.map { it.llvm }.toCValues(), elements.size)!!
}

internal open class Struct(val type: LLVMTypeRef?, val elements: List<ConstValue?>) : ConstValue {

    constructor(type: LLVMTypeRef?, vararg elements: ConstValue?) : this(type, elements.toList())

    constructor(vararg elements: ConstValue) : this(structType(elements.map { it.llvmType }), *elements)

    override val llvm = LLVMConstNamedStruct(type, elements.mapIndexed { index, element ->
        val expectedType = LLVMStructGetTypeAtIndex(type, index)
        if (element == null) {
            LLVMConstNull(expectedType)!!
        } else {
            element.llvm.also {
                assert(it.type == expectedType) {
                    "Unexpected type at $index: expected ${LLVMPrintTypeToString(expectedType)!!.toKString()} " +
                            "got ${LLVMPrintTypeToString(it.type)!!.toKString()}"
                }
            }
        }
    }.toCValues(), elements.size)!!

    init {
        assert(elements.size == LLVMCountStructElementTypes(type))
    }
}

internal val int1Type get() = LLVMInt1TypeInContext(llvmContext)!!
internal val int8Type get() = LLVMInt8TypeInContext(llvmContext)!!
internal val int16Type get() = LLVMInt16TypeInContext(llvmContext)!!
internal val int32Type get() = LLVMInt32TypeInContext(llvmContext)!!
internal val int64Type get() = LLVMInt64TypeInContext(llvmContext)!!
internal val int8TypePtr get() = pointerType(int8Type)
internal val floatType get() = LLVMFloatTypeInContext(llvmContext)!!
internal val doubleType get() = LLVMDoubleTypeInContext(llvmContext)!!
internal val vector128Type get() = LLVMVectorType(floatType, 4)!!

internal val voidType get() = LLVMVoidTypeInContext(llvmContext)!!

internal class Int1(val value: Boolean) : ConstValue {
    override val llvm = LLVMConstInt(int1Type, if (value) 1 else 0, 1)!!
}

internal class Int8(val value: Byte) : ConstValue {
    override val llvm = LLVMConstInt(int8Type, value.toLong(), 1)!!
}

internal class Int16(val value: Short) : ConstValue {
    override val llvm = LLVMConstInt(int16Type, value.toLong(), 1)!!
}

internal class Char16(val value: Char) : ConstValue {
    override val llvm = LLVMConstInt(int16Type, value.code.toLong(), 1)!!
}

internal class Int32(val value: Int) : ConstValue {
    override val llvm = LLVMConstInt(int32Type, value.toLong(), 1)!!
}

internal class Int64(val value: Long) : ConstValue {
    override val llvm = LLVMConstInt(int64Type, value, 1)!!
}

internal class Float32(val value: Float) : ConstValue {
    override val llvm = LLVMConstReal(floatType, value.toDouble())!!
}

internal class Float64(val value: Double) : ConstValue {
    override val llvm = LLVMConstReal(doubleType, value)!!
}

internal class Zero(val type: LLVMTypeRef) : ConstValue {
    override val llvm = LLVMConstNull(type)!!
}

internal class NullPointer(pointeeType: LLVMTypeRef): ConstPointer {
    override val llvm = LLVMConstNull(pointerType(pointeeType))!!
}

internal fun constValue(value: LLVMValueRef) = object : ConstValue {
    init {
        assert (LLVMIsConstant(value) == 1)
    }

    override val llvm = value
}

internal val RuntimeAware.kTypeInfo: LLVMTypeRef
    get() = runtime.typeInfoType
internal val RuntimeAware.kObjHeader: LLVMTypeRef
    get() = runtime.objHeaderType
internal val RuntimeAware.kObjHeaderPtr: LLVMTypeRef
    get() = pointerType(kObjHeader)
internal val RuntimeAware.kObjHeaderPtrPtr: LLVMTypeRef
    get() = pointerType(kObjHeaderPtr)
internal val RuntimeAware.kArrayHeader: LLVMTypeRef
    get() = runtime.arrayHeaderType
internal val RuntimeAware.kArrayHeaderPtr: LLVMTypeRef
    get() = pointerType(kArrayHeader)
internal val RuntimeAware.kTypeInfoPtr: LLVMTypeRef
    get() = pointerType(kTypeInfo)
internal val kInt1         get() = int1Type
internal val kBoolean      get() = kInt1
internal val kInt8Ptr      get() = pointerType(int8Type)
internal val kInt8PtrPtr   get() = pointerType(kInt8Ptr)
internal val kNullInt8Ptr  get() = LLVMConstNull(kInt8Ptr)!!
internal val kImmInt32Zero get() = Int32(0).llvm
internal val kImmInt32One  get() = Int32(1).llvm
internal val ContextUtils.kNullObjHeaderPtr: LLVMValueRef
    get() = LLVMConstNull(this.kObjHeaderPtr)!!
internal val ContextUtils.kNullObjHeaderPtrPtr: LLVMValueRef
    get() = LLVMConstNull(this.kObjHeaderPtrPtr)!!

// Nothing type has no values, but we do generate unreachable code and thus need some fake value:
internal val ContextUtils.kNothingFakeValue: LLVMValueRef
    get() = LLVMGetUndef(kObjHeaderPtr)!!

internal fun pointerType(pointeeType: LLVMTypeRef) = LLVMPointerType(pointeeType, 0)!!

internal fun structType(vararg types: LLVMTypeRef): LLVMTypeRef = structType(types.toList())

internal fun structType(types: List<LLVMTypeRef>): LLVMTypeRef =
    LLVMStructTypeInContext(llvmContext, types.toCValues(), types.size, 0)!!

internal fun ContextUtils.numParameters(functionType: LLVMTypeRef) : Int {
    // Note that type is usually function pointer, so we have to dereference it.
    return LLVMCountParamTypes(LLVMGetElementType(functionType))
}

fun extractConstUnsignedInt(value: LLVMValueRef): Long {
    assert(LLVMIsConstant(value) != 0)
    return LLVMConstIntGetZExtValue(value)
}

internal fun ContextUtils.isObjectReturn(functionType: LLVMTypeRef) : Boolean {
    // Note that type is usually function pointer, so we have to dereference it.
    val returnType = LLVMGetReturnType(LLVMGetElementType(functionType))!!
    return isObjectType(returnType)
}

internal fun ContextUtils.isObjectRef(value: LLVMValueRef): Boolean {
    return isObjectType(value.type)
}

internal fun RuntimeAware.isObjectType(type: LLVMTypeRef): Boolean {
    return type == kObjHeaderPtr || type == kArrayHeaderPtr
}

/**
 * Reads [size] bytes contained in this array.
 */
internal fun CArrayPointer<ByteVar>.getBytes(size: Long) =
        (0 .. size-1).map { this[it] }.toByteArray()

internal fun LLVMValueRef.getAsCString() : String {
    memScoped {
        val lengthPtr = alloc<size_tVar>()
        val data = LLVMGetAsString(this@getAsCString, lengthPtr.ptr)!!
        require(lengthPtr.value >= 1 && data[lengthPtr.value - 1] == 0.toByte()) { "Expected null-terminated string from llvm"}
        return data.toKString()
    }
}


internal fun getFunctionType(ptrToFunction: LLVMValueRef): LLVMTypeRef {
    return getGlobalType(ptrToFunction)
}

internal fun getGlobalType(ptrToGlobal: LLVMValueRef): LLVMTypeRef {
    return LLVMGetElementType(ptrToGlobal.type)!!
}

internal fun ContextUtils.addGlobal(name: String, type: LLVMTypeRef, isExported: Boolean): LLVMValueRef {
    if (isExported)
        assert(LLVMGetNamedGlobal(context.llvmModule, name) == null)
    return LLVMAddGlobal(context.llvmModule, type, name)!!
}

internal fun ContextUtils.importGlobal(name: String, type: LLVMTypeRef, origin: CompiledKlibModuleOrigin): LLVMValueRef {

    context.llvm.imports.add(origin)

    val found = LLVMGetNamedGlobal(context.llvmModule, name)
    return if (found != null) {
        assert (getGlobalType(found) == type)
        assert (LLVMGetInitializer(found) == null) { "$name is already declared in the current module" }
        found
    } else {
        addGlobal(name, type, isExported = false)
    }
}

internal abstract class AddressAccess {
    abstract fun getAddress(generationContext: FunctionGenerationContext?): LLVMValueRef
}

internal class GlobalAddressAccess(private val address: LLVMValueRef): AddressAccess() {
    override fun getAddress(generationContext: FunctionGenerationContext?): LLVMValueRef = address
}

internal class TLSAddressAccess(
        private val context: Context, private val index: Int): AddressAccess() {

    override fun getAddress(generationContext: FunctionGenerationContext?): LLVMValueRef {
        return generationContext!!.call(context.llvm.lookupTLS,
                listOf(context.llvm.tlsKey, Int32(index).llvm))
    }
}

internal fun ContextUtils.addKotlinThreadLocal(name: String, type: LLVMTypeRef): AddressAccess {
    return if (isObjectType(type)) {
        val index = context.llvm.tlsCount++
        TLSAddressAccess(context, index)
    } else {
        // TODO: This will break if Workers get decoupled from host threads.
        GlobalAddressAccess(LLVMAddGlobal(context.llvmModule, type, name)!!.also {
            LLVMSetThreadLocalMode(it, context.llvm.tlsMode)
            LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
        })
    }
}

internal fun ContextUtils.addKotlinGlobal(name: String, type: LLVMTypeRef, isExported: Boolean): AddressAccess {
    return GlobalAddressAccess(LLVMAddGlobal(context.llvmModule, type, name)!!.also {
        if (!isExported)
            LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
    })
}

internal fun functionType(returnType: LLVMTypeRef, isVarArg: Boolean = false, vararg paramTypes: LLVMTypeRef) =
        LLVMFunctionType(
                returnType,
                cValuesOf(*paramTypes), paramTypes.size,
                if (isVarArg) 1 else 0
        )!!

internal fun functionType(returnType: LLVMTypeRef, isVarArg: Boolean = false, paramTypes: List<LLVMTypeRef>) =
        functionType(returnType, isVarArg, *paramTypes.toTypedArray())


fun llvm2string(value: LLVMValueRef?): String {
  if (value == null) return "<null>"
  return LLVMPrintValueToString(value)!!.toKString()
}

fun llvmtype2string(type: LLVMTypeRef?): String {
    if (type == null) return "<null type>"
    return LLVMPrintTypeToString(type)!!.toKString()
}

fun getStructElements(type: LLVMTypeRef): List<LLVMTypeRef> {
    val count = LLVMCountStructElementTypes(type)
    return (0 until count).map {
        LLVMStructGetTypeAtIndex(type, it)!!
    }
}

fun parseBitcodeFile(path: String): LLVMModuleRef = memScoped {
    val bufRef = alloc<LLVMMemoryBufferRefVar>()
    val errorRef = allocPointerTo<ByteVar>()

    val res = LLVMCreateMemoryBufferWithContentsOfFile(path, bufRef.ptr, errorRef.ptr)
    if (res != 0) {
        throw Error(errorRef.value?.toKString())
    }

    val memoryBuffer = bufRef.value
    try {

        val moduleRef = alloc<LLVMModuleRefVar>()
        val parseRes = LLVMParseBitcodeInContext2(llvmContext, memoryBuffer, moduleRef.ptr)
        if (parseRes != 0) {
            throw Error(parseRes.toString())
        }

        moduleRef.value!!
    } finally {
        LLVMDisposeMemoryBuffer(memoryBuffer)
    }
}

private val nounwindAttrKindId by lazy {
    getLlvmAttributeKindId("nounwind")
}

private val noreturnAttrKindId by lazy {
    getLlvmAttributeKindId("noreturn")
}

private val noinlineAttrKindId by lazy {
    getLlvmAttributeKindId("noinline")
}

private val signextAttrKindId by lazy {
    getLlvmAttributeKindId("signext")
}


fun isFunctionNoUnwind(function: LLVMValueRef): Boolean {
    val attribute = LLVMGetEnumAttributeAtIndex(function, LLVMAttributeFunctionIndex, nounwindAttrKindId.value)
    return attribute != null
}

internal fun getLlvmAttributeKindId(attributeName: String): LLVMAttributeKindId {
    val attrKindId = LLVMGetEnumAttributeKindForName(attributeName, attributeName.length.signExtend())
    if (attrKindId == 0) {
        throw Error("Unable to find '$attributeName' attribute kind id")
    }
    return LLVMAttributeKindId(attrKindId)
}

data class LLVMAttributeKindId(val value: Int)

fun setFunctionNoUnwind(function: LLVMValueRef) {
    addLlvmFunctionEnumAttribute(function, nounwindAttrKindId)
}

fun setFunctionNoReturn(function: LLVMValueRef) {
    addLlvmFunctionEnumAttribute(function, noreturnAttrKindId)
}

fun setFunctionNoInline(function: LLVMValueRef) {
    addLlvmFunctionEnumAttribute(function, noinlineAttrKindId)
}

internal fun addLlvmFunctionEnumAttribute(function: LLVMValueRef, attrKindId: LLVMAttributeKindId, value: Long = 0) {
    val attribute = createLlvmEnumAttribute(LLVMGetTypeContext(function.type)!!, attrKindId, value)
    addLlvmFunctionAttribute(function, attribute)
}

internal fun createLlvmEnumAttribute(llvmContext: LLVMContextRef, attrKindId: LLVMAttributeKindId, value: Long = 0) =
        LLVMCreateEnumAttribute(llvmContext, attrKindId.value, value)!!

internal fun addLlvmFunctionAttribute(function: LLVMValueRef, attribute: LLVMAttributeRef) {
    LLVMAddAttributeAtIndex(function, LLVMAttributeFunctionIndex, attribute)
}

fun addFunctionSignext(function: LLVMValueRef, index: Int, type: LLVMTypeRef?) {
    if (type == int1Type || type == int8Type || type == int16Type) {
        val attribute = createLlvmEnumAttribute(LLVMGetTypeContext(function.type)!!, signextAttrKindId)
        LLVMAddAttributeAtIndex(function, index, attribute)
    }
}

internal fun String.mdString() = LLVMMDStringInContext(llvmContext, this, this.length)!!
internal fun node(vararg it:LLVMValueRef) = LLVMMDNodeInContext(llvmContext, it.toList().toCValues(), it.size)

internal fun LLVMValueRef.setUnaligned() = apply { LLVMSetAlignment(this, 1) }

internal fun getOperands(value: LLVMValueRef) =
        (0 until LLVMGetNumOperands(value)).map { LLVMGetOperand(value, it)!! }

internal fun getGlobalAliases(module: LLVMModuleRef) =
        generateSequence(LLVMGetFirstGlobalAlias(module), { LLVMGetNextGlobalAlias(it) })

internal fun getFunctions(module: LLVMModuleRef) =
        generateSequence(LLVMGetFirstFunction(module), { LLVMGetNextFunction(it) })

internal fun getGlobals(module: LLVMModuleRef) =
        generateSequence(LLVMGetFirstGlobal(module), { LLVMGetNextGlobal(it) })

fun LLVMTypeRef.isFloatingPoint(): Boolean = when (llvm.LLVMGetTypeKind(this)) {
    LLVMTypeKind.LLVMFloatTypeKind, LLVMTypeKind.LLVMDoubleTypeKind -> true
    else -> false
}

fun LLVMTypeRef.isVectorElementType(): Boolean = when (llvm.LLVMGetTypeKind(this)) {
    LLVMTypeKind.LLVMIntegerTypeKind,
    LLVMTypeKind.LLVMFloatTypeKind,
    LLVMTypeKind.LLVMDoubleTypeKind -> true
    else -> false
}
