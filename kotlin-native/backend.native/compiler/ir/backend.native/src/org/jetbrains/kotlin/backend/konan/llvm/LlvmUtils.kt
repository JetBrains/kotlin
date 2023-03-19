/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.*
import llvm.*
import org.jetbrains.kotlin.ir.declarations.IrDeclaration

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
    fun getElementPtr(llvm: CodegenLlvmHelpers, index: Int): ConstPointer = ConstGetElementPtr(llvm, this, index)
}

internal fun constPointer(value: LLVMValueRef) = object : ConstPointer {
    init {
        assert(LLVMIsConstant(value) == 1)
    }

    override val llvm = value
}

private class ConstGetElementPtr(llvm: CodegenLlvmHelpers, pointer: ConstPointer, index: Int) : ConstPointer {
    override val llvm = LLVMConstInBoundsGEP(pointer.llvm, cValuesOf(llvm.int32(0), llvm.int32(index)), 2)!!
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
internal val RuntimeAware.kNullObjHeaderPtr: LLVMValueRef
    get() = LLVMConstNull(kObjHeaderPtr)!!
internal val RuntimeAware.kNullObjHeaderPtrPtr: LLVMValueRef
    get() = LLVMConstNull(kObjHeaderPtrPtr)!!

// Nothing type has no values, but we do generate unreachable code and thus need some fake value:
internal val RuntimeAware.kNothingFakeValue: LLVMValueRef
    get() = LLVMGetUndef(kObjHeaderPtr)!!

internal fun pointerType(pointeeType: LLVMTypeRef) = LLVMPointerType(pointeeType, 0)!!

internal fun ContextUtils.numParameters(functionType: LLVMTypeRef) : Int {
    // Note that type is usually function pointer, so we have to dereference it.
    return LLVMCountParamTypes(LLVMGetElementType(functionType))
}

fun extractConstUnsignedInt(value: LLVMValueRef): Long {
    assert(LLVMIsConstant(value) != 0)
    return LLVMConstIntGetZExtValue(value)
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
        assert(LLVMGetNamedGlobal(llvm.module, name) == null)
    return LLVMAddGlobal(llvm.module, type, name)!!
}

private fun ContextUtils.importGlobal(name: String, type: LLVMTypeRef): LLVMValueRef {
    val found = LLVMGetNamedGlobal(llvm.module, name)
    return if (found == null)
        addGlobal(name, type, isExported = false)
    else {
        require(getGlobalType(found) == type)
        require(LLVMGetInitializer(found) == null) { "$name is already declared in the current module" }
        found
    }
}

internal fun ContextUtils.importGlobal(name: String, type: LLVMTypeRef, declaration: IrDeclaration) =
        importGlobal(name, type).also { generationState.dependenciesTracker.add(declaration) }

internal fun ContextUtils.importObjCGlobal(name: String, type: LLVMTypeRef) = importGlobal(name, type)

internal fun ContextUtils.importNativeRuntimeGlobal(name: String, type: LLVMTypeRef) =
        importGlobal(name, type).also { generationState.dependenciesTracker.addNativeRuntime() }

private fun CodeGenerator.replaceExternalWeakOrCommonGlobal(name: String, value: ConstValue) {
    if (generationState.llvmModuleSpecification.importsKotlinDeclarationsFromOtherSharedLibraries()) {
        // When some dynamic caches are used, we consider that stdlib is in the dynamic cache as well.
        // Runtime is linked into stdlib module only, so import runtime global from it.
        val global = importGlobal(name, value.llvmType)
        val initializerProto = LlvmFunctionSignature(LlvmRetType(llvm.voidType)).toProto(
                name = "",
                origin = null,
                LLVMLinkage.LLVMPrivateLinkage
        )
        val initializer = generateFunctionNoRuntime(this, initializerProto) {
            store(value.llvm, global)
            ret(null)
        }

        llvm.otherStaticInitializers += initializer
    } else {
        val global = staticData.placeGlobal(name, value, isExported = true)

        if (generationState.llvmModuleSpecification.importsKotlinDeclarationsFromOtherObjectFiles()) {
            // Note: actually this is required only if global's weak/common definition is in another object file,
            // but it is simpler to do this for all globals, considering that all usages can't be removed by DCE anyway.
            llvm.usedGlobals += global.llvmGlobal
            LLVMSetVisibility(global.llvmGlobal, LLVMVisibility.LLVMHiddenVisibility)

            // See also [emitKt42254Hint].
        }
    }
}

internal fun CodeGenerator.replaceExternalWeakOrCommonGlobal(
        name: String,
        value: ConstValue,
        declaration: IrDeclaration
) = replaceExternalWeakOrCommonGlobal(name, value).also { generationState.dependenciesTracker.add(declaration) }

internal fun CodeGenerator.replaceExternalWeakOrCommonGlobalFromNativeRuntime(
        name: String,
        value: ConstValue
) = replaceExternalWeakOrCommonGlobal(name, value).also { generationState.dependenciesTracker.addNativeRuntime() }

internal abstract class AddressAccess {
    abstract fun getAddress(generationContext: FunctionGenerationContext?): LLVMValueRef
}

internal class GlobalAddressAccess(private val address: LLVMValueRef): AddressAccess() {
    override fun getAddress(generationContext: FunctionGenerationContext?): LLVMValueRef = address
}

internal class TLSAddressAccess(private val index: Int) : AddressAccess() {
    override fun getAddress(generationContext: FunctionGenerationContext?): LLVMValueRef {
        val llvm = generationContext!!.llvm
        return generationContext.call(llvm.lookupTLS, listOf(llvm.tlsKey, llvm.int32(index)))
    }
}

internal fun ContextUtils.addKotlinThreadLocal(name: String, type: LLVMTypeRef, alignment: Int): AddressAccess {
    return if (isObjectType(type)) {
        val index = llvm.tlsCount++
        require(llvm.runtime.pointerAlignment % alignment == 0)
        TLSAddressAccess(index)
    } else {
        // TODO: This will break if Workers get decoupled from host threads.
        GlobalAddressAccess(LLVMAddGlobal(llvm.module, type, name)!!.also {
            LLVMSetThreadLocalMode(it, llvm.tlsMode)
            LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
            LLVMSetAlignment(it, alignment)
        })
    }
}

internal fun ContextUtils.addKotlinGlobal(name: String, type: LLVMTypeRef, alignment: Int, isExported: Boolean): AddressAccess {
    return GlobalAddressAccess(LLVMAddGlobal(llvm.module, type, name)!!.also {
        if (!isExported)
            LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
        LLVMSetAlignment(it, alignment)
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

fun parseBitcodeFile(llvmContext: LLVMContextRef, path: String): LLVMModuleRef = memScoped {
    val bufRef = alloc<LLVMMemoryBufferRefVar>()
    val errorRef = allocPointerTo<ByteVar>()

    val res = LLVMCreateMemoryBufferWithContentsOfFile(path, bufRef.ptr, errorRef.ptr)
    if (res != 0) {
        throw Error("Error parsing file $path : ${errorRef.value?.toKString()}")
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

fun isFunctionNoUnwind(function: LLVMValueRef): Boolean {
    val attribute = LLVMGetEnumAttributeAtIndex(function, LLVMAttributeFunctionIndex, LlvmFunctionAttribute.NoUnwind.asAttributeKindId().value)
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
    addLlvmFunctionEnumAttribute(function, LlvmFunctionAttribute.NoUnwind)
}

fun setFunctionNoReturn(function: LLVMValueRef) {
    addLlvmFunctionEnumAttribute(function, LlvmFunctionAttribute.NoReturn)
}

fun setFunctionNoInline(function: LLVMValueRef) {
    addLlvmFunctionEnumAttribute(function, LlvmFunctionAttribute.NoInline)
}

internal fun addLlvmFunctionEnumAttribute(function: LLVMValueRef, attrKindId: LLVMAttributeKindId, value: Long = 0) {
    val attribute = createLlvmEnumAttribute(LLVMGetTypeContext(function.type)!!, attrKindId, value)
    addLlvmFunctionAttribute(function, attribute)
}

internal fun addLlvmFunctionEnumAttribute(function: LLVMValueRef, attr: LlvmAttribute, value: Long = 0) =
        addLlvmFunctionEnumAttribute(function, attr.asAttributeKindId(), value)

internal fun createLlvmEnumAttribute(llvmContext: LLVMContextRef, attrKindId: LLVMAttributeKindId, value: Long = 0) =
        LLVMCreateEnumAttribute(llvmContext, attrKindId.value, value)!!

internal fun addLlvmFunctionAttribute(function: LLVMValueRef, attribute: LLVMAttributeRef) {
    LLVMAddAttributeAtIndex(function, LLVMAttributeFunctionIndex, attribute)
}

internal fun String.mdString(llvmContext: LLVMContextRef) = LLVMMDStringInContext(llvmContext, this, this.length)!!
internal fun node(llvmContext: LLVMContextRef, vararg it: LLVMValueRef) = LLVMMDNodeInContext(llvmContext, it.toList().toCValues(), it.size)

internal fun LLVMValueRef.setUnaligned() = apply { LLVMSetAlignment(this, 1) }

internal fun getOperands(value: LLVMValueRef) =
        (0 until LLVMGetNumOperands(value)).map { LLVMGetOperand(value, it)!! }

internal fun getGlobalAliases(module: LLVMModuleRef) =
        generateSequence(LLVMGetFirstGlobalAlias(module), { LLVMGetNextGlobalAlias(it) })

internal fun getFunctions(module: LLVMModuleRef) =
        generateSequence(LLVMGetFirstFunction(module), { LLVMGetNextFunction(it) })

internal fun getBasicBlocks(function: LLVMValueRef) =
        generateSequence(LLVMGetFirstBasicBlock(function)) { LLVMGetNextBasicBlock(it) }

internal fun getInstructions(block: LLVMBasicBlockRef) =
        generateSequence(LLVMGetFirstInstruction(block)) { LLVMGetNextInstruction(it) }


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

fun LLVMModuleRef.getName(): String = memScoped {
    val sizeVar = alloc<size_tVar>()
    LLVMGetModuleIdentifier(this@getName, sizeVar.ptr)!!.toKStringFromUtf8()
}
