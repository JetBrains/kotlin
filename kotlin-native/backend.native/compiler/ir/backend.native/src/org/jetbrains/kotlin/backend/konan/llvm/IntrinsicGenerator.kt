package org.jetbrains.kotlin.backend.konan.llvm

import kotlinx.cinterop.cValuesOf
import llvm.*
import org.jetbrains.kotlin.backend.konan.KonanFqNames
import org.jetbrains.kotlin.backend.konan.MemoryModel
import org.jetbrains.kotlin.backend.konan.RuntimeNames
import org.jetbrains.kotlin.backend.konan.descriptors.getAnnotationStringValue
import org.jetbrains.kotlin.backend.konan.descriptors.isConstantConstructorIntrinsic
import org.jetbrains.kotlin.backend.konan.descriptors.isTypedIntrinsic
import org.jetbrains.kotlin.backend.konan.llvm.objc.genObjCSelector
import org.jetbrains.kotlin.backend.konan.reportCompilationError
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.findAnnotation

internal enum class IntrinsicType {
    PLUS,
    MINUS,
    TIMES,
    SIGNED_DIV,
    SIGNED_REM,
    UNSIGNED_DIV,
    UNSIGNED_REM,
    INC,
    DEC,
    UNARY_PLUS,
    UNARY_MINUS,
    SHL,
    SHR,
    USHR,
    AND,
    OR,
    XOR,
    INV,
    SIGN_EXTEND,
    ZERO_EXTEND,
    INT_TRUNCATE,
    FLOAT_TRUNCATE,
    FLOAT_EXTEND,
    SIGNED_TO_FLOAT,
    UNSIGNED_TO_FLOAT,
    FLOAT_TO_SIGNED,
    SIGNED_COMPARE_TO,
    UNSIGNED_COMPARE_TO,
    NOT,
    REINTERPRET,
    EXTRACT_ELEMENT,
    ARE_EQUAL_BY_VALUE,
    IEEE_754_EQUALS,
    // OBJC
    OBJC_GET_MESSENGER,
    OBJC_GET_MESSENGER_STRET,
    OBJC_GET_OBJC_CLASS,
    OBJC_CREATE_SUPER_STRUCT,
    OBJC_INIT_BY,
    OBJC_GET_SELECTOR,
    // Other
    CREATE_UNINITIALIZED_INSTANCE,
    IDENTITY,
    IMMUTABLE_BLOB,
    INIT_INSTANCE,
    IS_SUBTYPE,
    IS_EXPERIMENTAL_MM,
    THE_UNIT_INSTANCE,
    // Enums
    ENUM_VALUES,
    ENUM_VALUE_OF,
    // Coroutines
    GET_CONTINUATION,
    RETURN_IF_SUSPENDED,
    // Interop
    INTEROP_READ_BITS,
    INTEROP_WRITE_BITS,
    INTEROP_READ_PRIMITIVE,
    INTEROP_WRITE_PRIMITIVE,
    INTEROP_GET_POINTER_SIZE,
    INTEROP_NATIVE_PTR_TO_LONG,
    INTEROP_NATIVE_PTR_PLUS_LONG,
    INTEROP_GET_NATIVE_NULL_PTR,
    INTEROP_CONVERT,
    INTEROP_BITS_TO_FLOAT,
    INTEROP_BITS_TO_DOUBLE,
    INTEROP_SIGN_EXTEND,
    INTEROP_NARROW,
    INTEROP_STATIC_C_FUNCTION,
    INTEROP_FUNPTR_INVOKE,
    INTEROP_MEMORY_COPY,
    // Worker
    WORKER_EXECUTE,
    // Atomics
    COMPARE_AND_SET_FIELD,
    COMPARE_AND_EXCHANGE_FIELD,
    GET_AND_SET_FIELD,
    GET_AND_ADD_FIELD,
    COMPARE_AND_SET,
    COMPARE_AND_EXCHANGE,
    GET_AND_SET,
    GET_AND_ADD,
}

internal enum class ConstantConstructorIntrinsicType {
    KCLASS_IMPL,
    KTYPE_IMPL,
}

// Explicit and single interface between Intrinsic Generator and IrToBitcode.
internal interface IntrinsicGeneratorEnvironment {

    val codegen: CodeGenerator

    val functionGenerationContext: FunctionGenerationContext

    val exceptionHandler: ExceptionHandler

    fun calculateLifetime(element: IrElement): Lifetime

    fun evaluateCall(function: IrFunction, args: List<LLVMValueRef>, resultLifetime: Lifetime,
                     superClass: IrClass? = null, resultSlot: LLVMValueRef? = null): LLVMValueRef

    fun evaluateExplicitArgs(expression: IrFunctionAccessExpression): List<LLVMValueRef>

    fun evaluateExpression(value: IrExpression, resultSlot: LLVMValueRef?): LLVMValueRef

    fun getObjectFieldPointer(thisRef: LLVMValueRef, field: IrField): LLVMValueRef

    fun getStaticFieldPointer(field: IrField): LLVMValueRef
}

internal fun tryGetIntrinsicType(callSite: IrFunctionAccessExpression): IntrinsicType? =
        if (callSite.symbol.owner.isTypedIntrinsic) getIntrinsicType(callSite) else null

private fun getIntrinsicType(callSite: IrFunctionAccessExpression): IntrinsicType {
    val function = callSite.symbol.owner
    val annotation = function.annotations.findAnnotation(RuntimeNames.typedIntrinsicAnnotation)!!
    val value = annotation.getAnnotationStringValue()!!
    return IntrinsicType.valueOf(value)
}

internal fun tryGetConstantConstructorIntrinsicType(constructor: IrConstructorSymbol): ConstantConstructorIntrinsicType? =
        if (constructor.owner.isConstantConstructorIntrinsic) getConstantConstructorIntrinsicType(constructor) else null

private fun getConstantConstructorIntrinsicType(constructor: IrConstructorSymbol): ConstantConstructorIntrinsicType {
    val annotation = constructor.owner.annotations.findAnnotation(KonanFqNames.constantConstructorIntrinsic)!!
    val value = annotation.getAnnotationStringValue()!!
    return ConstantConstructorIntrinsicType.valueOf(value)
}


internal class IntrinsicGenerator(private val environment: IntrinsicGeneratorEnvironment) {

    private val codegen = environment.codegen

    private val context = codegen.context

    private val IrCall.llvmReturnType: LLVMTypeRef
        get() = LLVMGetReturnType(codegen.getLlvmFunctionType(symbol.owner))!!


    private fun LLVMTypeRef.sizeInBits() = LLVMSizeOfTypeInBits(codegen.llvmTargetData, this).toInt()

    /**
     * Some intrinsics have to be processed before evaluation of their arguments.
     * So this method looks at [callSite] and if it is call to "special" intrinsic
     * processes it. Otherwise, it returns null.
     */
    @Suppress("UNUSED_PARAMETER")
    fun tryEvaluateSpecialCall(callSite: IrFunctionAccessExpression, resultSlot: LLVMValueRef?): LLVMValueRef? {
        val function = callSite.symbol.owner
        if (!function.isTypedIntrinsic) {
            return null
        }
        return when (getIntrinsicType(callSite)) {
            IntrinsicType.IMMUTABLE_BLOB -> {
                @Suppress("UNCHECKED_CAST")
                val arg = callSite.getValueArgument(0) as IrConst<String>
                codegen.llvm.staticData.createImmutableBlob(arg)
            }
            IntrinsicType.OBJC_GET_SELECTOR -> {
                val selector = (callSite.getValueArgument(0) as IrConst<*>).value as String
                environment.functionGenerationContext.genObjCSelector(selector)
            }
            IntrinsicType.INIT_INSTANCE -> {
                val initializer = callSite.getValueArgument(1) as IrConstructorCall
                val thiz = environment.evaluateExpression(callSite.getValueArgument(0)!!, null)
                environment.evaluateCall(
                        initializer.symbol.owner,
                        listOf(thiz) + environment.evaluateExplicitArgs(initializer),
                        environment.calculateLifetime(initializer),
                )
                codegen.theUnitInstanceRef.llvm
            }
            else -> null
        }
    }

    fun evaluateCall(callSite: IrCall, args: List<LLVMValueRef>, resultSlot: LLVMValueRef?): LLVMValueRef =
            environment.functionGenerationContext.evaluateCall(callSite, args, resultSlot)

    // Assuming that we checked for `TypedIntrinsic` annotation presence.
    private fun FunctionGenerationContext.evaluateCall(callSite: IrCall, args: List<LLVMValueRef>, resultSlot: LLVMValueRef?): LLVMValueRef =
            when (val intrinsicType = getIntrinsicType(callSite)) {
                IntrinsicType.PLUS -> emitPlus(args)
                IntrinsicType.MINUS -> emitMinus(args)
                IntrinsicType.TIMES -> emitTimes(args)
                IntrinsicType.SIGNED_DIV -> emitSignedDiv(args)
                IntrinsicType.SIGNED_REM -> emitSignedRem(args)
                IntrinsicType.UNSIGNED_DIV -> emitUnsignedDiv(args)
                IntrinsicType.UNSIGNED_REM -> emitUnsignedRem(args)
                IntrinsicType.INC -> emitInc(args)
                IntrinsicType.DEC -> emitDec(args)
                IntrinsicType.UNARY_PLUS -> emitUnaryPlus(args)
                IntrinsicType.UNARY_MINUS -> emitUnaryMinus(args)
                IntrinsicType.SHL -> emitShl(args)
                IntrinsicType.SHR -> emitShr(args)
                IntrinsicType.USHR -> emitUshr(args)
                IntrinsicType.AND -> emitAnd(args)
                IntrinsicType.OR -> emitOr(args)
                IntrinsicType.XOR -> emitXor(args)
                IntrinsicType.INV -> emitInv(args)
                IntrinsicType.SIGNED_COMPARE_TO -> emitSignedCompareTo(args)
                IntrinsicType.UNSIGNED_COMPARE_TO -> emitUnsignedCompareTo(args)
                IntrinsicType.NOT -> emitNot(args)
                IntrinsicType.REINTERPRET -> emitReinterpret(callSite, args)
                IntrinsicType.EXTRACT_ELEMENT -> emitExtractElement(callSite, args)
                IntrinsicType.SIGN_EXTEND -> emitSignExtend(callSite, args)
                IntrinsicType.ZERO_EXTEND -> emitZeroExtend(callSite, args)
                IntrinsicType.INT_TRUNCATE -> emitIntTruncate(callSite, args)
                IntrinsicType.SIGNED_TO_FLOAT -> emitSignedToFloat(callSite, args)
                IntrinsicType.UNSIGNED_TO_FLOAT -> emitUnsignedToFloat(callSite, args)
                IntrinsicType.FLOAT_TO_SIGNED -> emitFloatToSigned(callSite, args)
                IntrinsicType.FLOAT_EXTEND -> emitFloatExtend(callSite, args)
                IntrinsicType.FLOAT_TRUNCATE -> emitFloatTruncate(callSite, args)
                IntrinsicType.ARE_EQUAL_BY_VALUE -> emitAreEqualByValue(args)
                IntrinsicType.IEEE_754_EQUALS -> emitIeee754Equals(args)
                IntrinsicType.OBJC_GET_MESSENGER -> emitObjCGetMessenger(args, isStret = false)
                IntrinsicType.OBJC_GET_MESSENGER_STRET -> emitObjCGetMessenger(args, isStret = true)
                IntrinsicType.OBJC_GET_OBJC_CLASS -> emitGetObjCClass(callSite)
                IntrinsicType.OBJC_CREATE_SUPER_STRUCT -> emitObjCCreateSuperStruct(args)
                IntrinsicType.INTEROP_READ_BITS -> emitReadBits(args)
                IntrinsicType.INTEROP_WRITE_BITS -> emitWriteBits(args)
                IntrinsicType.INTEROP_READ_PRIMITIVE -> emitReadPrimitive(callSite, args)
                IntrinsicType.INTEROP_WRITE_PRIMITIVE -> emitWritePrimitive(callSite, args)
                IntrinsicType.INTEROP_GET_POINTER_SIZE -> emitGetPointerSize()
                IntrinsicType.CREATE_UNINITIALIZED_INSTANCE -> emitCreateUninitializedInstance(callSite, resultSlot)
                IntrinsicType.IS_SUBTYPE -> emitIsSubtype(callSite, args)
                IntrinsicType.INTEROP_NATIVE_PTR_TO_LONG -> emitNativePtrToLong(callSite, args)
                IntrinsicType.INTEROP_NATIVE_PTR_PLUS_LONG -> emitNativePtrPlusLong(args)
                IntrinsicType.INTEROP_GET_NATIVE_NULL_PTR -> emitGetNativeNullPtr()
                IntrinsicType.IDENTITY -> emitIdentity(args)
                IntrinsicType.INTEROP_MEMORY_COPY -> emitMemoryCopy(callSite, args)
                IntrinsicType.IS_EXPERIMENTAL_MM -> emitIsExperimentalMM()
                IntrinsicType.THE_UNIT_INSTANCE -> theUnitInstanceRef.llvm
                IntrinsicType.COMPARE_AND_SET -> emitCompareAndSet(callSite, args)
                IntrinsicType.COMPARE_AND_EXCHANGE -> emitCompareAndSwap(callSite, args, resultSlot)
                IntrinsicType.GET_AND_SET -> emitGetAndSet(callSite, args, resultSlot)
                IntrinsicType.GET_AND_ADD -> emitGetAndAdd(callSite, args)
                IntrinsicType.GET_CONTINUATION,
                IntrinsicType.RETURN_IF_SUSPENDED,
                IntrinsicType.INTEROP_BITS_TO_FLOAT,
                IntrinsicType.INTEROP_BITS_TO_DOUBLE,
                IntrinsicType.INTEROP_SIGN_EXTEND,
                IntrinsicType.INTEROP_NARROW,
                IntrinsicType.INTEROP_STATIC_C_FUNCTION,
                IntrinsicType.INTEROP_FUNPTR_INVOKE,
                IntrinsicType.INTEROP_CONVERT,
                IntrinsicType.ENUM_VALUES,
                IntrinsicType.ENUM_VALUE_OF,
                IntrinsicType.WORKER_EXECUTE,
                IntrinsicType.COMPARE_AND_SET_FIELD,
                IntrinsicType.COMPARE_AND_EXCHANGE_FIELD,
                IntrinsicType.GET_AND_SET_FIELD,
                IntrinsicType.GET_AND_ADD_FIELD ->
                    reportNonLoweredIntrinsic(intrinsicType)
                IntrinsicType.INIT_INSTANCE,
                IntrinsicType.OBJC_INIT_BY,
                IntrinsicType.OBJC_GET_SELECTOR,
                IntrinsicType.IMMUTABLE_BLOB ->
                    reportSpecialIntrinsic(intrinsicType)
            }

    fun evaluateConstantConstructorFields(constant: IrConstantObject, args: List<ConstValue>) : List<ConstValue> {
        return when (val intrinsicType = getConstantConstructorIntrinsicType(constant.constructor)) {
            ConstantConstructorIntrinsicType.KCLASS_IMPL -> {
                require(args.isEmpty())
                val typeArgument = constant.typeArguments[0]
                val typeArgumentClass = typeArgument.getClass()!!
                val typeInfo = codegen.typeInfoValue(typeArgumentClass)
                listOf(constPointer(typeInfo).bitcast(codegen.llvm.int8PtrType))
            }
            ConstantConstructorIntrinsicType.KTYPE_IMPL ->
                reportNonLoweredIntrinsic(intrinsicType)
        }
    }

    private fun reportSpecialIntrinsic(intrinsicType: IntrinsicType): Nothing =
            context.reportCompilationError("$intrinsicType should be handled by `tryEvaluateSpecialCall`")

    private fun reportNonLoweredIntrinsic(intrinsicType: IntrinsicType): Nothing =
            context.reportCompilationError("Intrinsic of type $intrinsicType should be handled by previous lowering phase")

    private fun reportNonLoweredIntrinsic(intrinsicType: ConstantConstructorIntrinsicType): Nothing =
            context.reportCompilationError("Constant constructor intrinsic of type $intrinsicType should be handled by previous lowering phase")


    private fun FunctionGenerationContext.emitIdentity(args: List<LLVMValueRef>): LLVMValueRef =
            args.single()

    private fun FunctionGenerationContext.emitIsExperimentalMM(): LLVMValueRef =
            llvm.int1(context.memoryModel == MemoryModel.EXPERIMENTAL)

    // cmpxcgh llvm instruction return pair. idnex is index of required element of this pair
    enum class CmpExchangeMode(val index:Int) {
        SWAP(0),
        SET(1)
    }

    private fun FunctionGenerationContext.emitCmpExchange(callSite: IrCall, args: List<LLVMValueRef>, mode: CmpExchangeMode, resultSlot: LLVMValueRef?): LLVMValueRef {
        val field = context.mapping.functionToVolatileField[callSite.symbol.owner]!!
        val address: LLVMValueRef
        val expected: LLVMValueRef
        val new: LLVMValueRef
        if (callSite.dispatchReceiver != null) {
            require(!field.isStatic)
            require(args.size == 3)
            address = environment.getObjectFieldPointer(args[0], field)
            expected = args[1]
            new = args[2]
        } else {
            require(field.isStatic)
            require(args.size == 2)
            address = environment.getStaticFieldPointer(field)
            expected = args[0]
            new = args[1]
        }
        return if (isObjectRef(args[1])) {
            require(context.memoryModel == MemoryModel.EXPERIMENTAL)
            when (mode) {
                CmpExchangeMode.SET -> call(llvm.CompareAndSetVolatileHeapRef, listOf(address, expected, new))
                CmpExchangeMode.SWAP -> call(llvm.CompareAndSwapVolatileHeapRef, listOf(address, expected, new),
                        environment.calculateLifetime(callSite), resultSlot = resultSlot)
            }
        } else {
            val cmp = LLVMBuildAtomicCmpXchg(builder, address, expected, new,
                    LLVMAtomicOrdering.LLVMAtomicOrderingSequentiallyConsistent,
                    LLVMAtomicOrdering.LLVMAtomicOrderingSequentiallyConsistent,
                    SingleThread = 0
            )!!

            LLVMBuildExtractValue(builder, cmp, mode.index, "")!!
        }
    }

    private fun FunctionGenerationContext.emitAtomicRMW(callSite: IrCall, args: List<LLVMValueRef>, op: LLVMAtomicRMWBinOp, resultSlot: LLVMValueRef?): LLVMValueRef {
        val field = context.mapping.functionToVolatileField[callSite.symbol.owner]!!
        val address: LLVMValueRef
        val value: LLVMValueRef
        if (callSite.dispatchReceiver != null) {
            require(!field.isStatic)
            require(args.size == 2)
            address = environment.getObjectFieldPointer(args[0], field)
            value = args[1]
        } else {
            require(field.isStatic)
            require(args.size == 1)
            address = environment.getStaticFieldPointer(field)
            value = args[0]
        }
        return if (isObjectRef(value)) {
            require(op == LLVMAtomicRMWBinOp.LLVMAtomicRMWBinOpXchg)
            require(context.memoryModel == MemoryModel.EXPERIMENTAL)
            call(llvm.GetAndSetVolatileHeapRef, listOf(address, value),
                    environment.calculateLifetime(callSite), resultSlot = resultSlot)
        } else {
            LLVMBuildAtomicRMW(builder, op, address, value,
                    LLVMAtomicOrdering.LLVMAtomicOrderingSequentiallyConsistent,
                    singleThread = 0
            )!!
        }
    }

    private fun FunctionGenerationContext.emitCompareAndSet(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        return emitCmpExchange(callSite, args, CmpExchangeMode.SET, null)
    }
    private fun FunctionGenerationContext.emitCompareAndSwap(callSite: IrCall, args: List<LLVMValueRef>, resultSlot: LLVMValueRef?): LLVMValueRef {
        return emitCmpExchange(callSite, args, CmpExchangeMode.SWAP, resultSlot)
    }
    private fun FunctionGenerationContext.emitGetAndSet(callSite: IrCall, args: List<LLVMValueRef>, resultSlot: LLVMValueRef?): LLVMValueRef {
        return emitAtomicRMW(callSite, args, LLVMAtomicRMWBinOp.LLVMAtomicRMWBinOpXchg, resultSlot)
    }
    private fun FunctionGenerationContext.emitGetAndAdd(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        return emitAtomicRMW(callSite, args, LLVMAtomicRMWBinOp.LLVMAtomicRMWBinOpAdd, null)
    }


    private fun FunctionGenerationContext.emitGetNativeNullPtr(): LLVMValueRef =
            llvm.kNullInt8Ptr

    private fun FunctionGenerationContext.emitNativePtrPlusLong(args: List<LLVMValueRef>): LLVMValueRef =
        gep(args[0], args[1])

    private fun FunctionGenerationContext.emitNativePtrToLong(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        val intPtrValue = ptrToInt(args.single(), codegen.intPtrType)
        val resultType = callSite.llvmReturnType
        return if (resultType == intPtrValue.type) {
            intPtrValue
        } else {
            LLVMBuildSExt(builder, intPtrValue, resultType, "")!!
        }
    }

    private fun FunctionGenerationContext.emitCreateUninitializedInstance(callSite: IrCall, resultSlot: LLVMValueRef?): LLVMValueRef {
        val typeParameterT = context.ir.symbols.createUninitializedInstance.descriptor.typeParameters[0]
        val enumClass = callSite.getTypeArgument(typeParameterT.index)!!
        val enumIrClass = enumClass.getClass()!!
        return allocInstance(enumIrClass, environment.calculateLifetime(callSite), resultSlot)
    }

    private fun FunctionGenerationContext.emitIsSubtype(callSite: IrCall, args: List<LLVMValueRef>) =
            with(VirtualTablesLookup) {
                checkIsSubtype(
                        objTypeInfo = bitcast(pointerType(llvm.kTypeInfo), args.single()),
                        dstClass = callSite.getTypeArgument(0)!!.classOrNull!!.owner
                )
            }

    private fun FunctionGenerationContext.emitGetPointerSize(): LLVMValueRef =
            llvm.int32(LLVMPointerSize(codegen.llvmTargetData))

    private fun FunctionGenerationContext.emitReadPrimitive(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        val pointerType = pointerType(callSite.llvmReturnType)
        val rawPointer = args.last()
        val pointer = bitcast(pointerType, rawPointer)
        return load(pointer)
    }

    private fun FunctionGenerationContext.emitWritePrimitive(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        val function = callSite.symbol.owner
        val pointerType = pointerType(function.valueParameters.last().type.toLLVMType(llvm))
        val rawPointer = args[1]
        val pointer = bitcast(pointerType, rawPointer)
        store(args[2], pointer)
        return codegen.theUnitInstanceRef.llvm
    }

    private fun FunctionGenerationContext.emitReadBits(args: List<LLVMValueRef>): LLVMValueRef {
        val ptr = args[0]
        assert(ptr.type == llvm.int8PtrType)

        val offset = extractConstUnsignedInt(args[1])
        val size = extractConstUnsignedInt(args[2]).toInt()
        val signed = extractConstUnsignedInt(args[3]) != 0L

        val prefixBitsNum = (offset % 8).toInt()
        val suffixBitsNum = (8 - ((size + offset) % 8).toInt()) % 8

        // Note: LLVM allows to read without padding tail up to byte boundary, but the result seems to be incorrect.

        val bitsWithPaddingNum = prefixBitsNum + size + suffixBitsNum
        val bitsWithPaddingType = LLVMIntTypeInContext(llvm.llvmContext, bitsWithPaddingNum)!!

        val bitsWithPaddingPtr = bitcast(pointerType(bitsWithPaddingType), gep(ptr, llvm.int64(offset / 8)))
        val bitsWithPadding = load(bitsWithPaddingPtr).setUnaligned()

        val bits = shr(
                shl(bitsWithPadding, suffixBitsNum),
                prefixBitsNum + suffixBitsNum, signed
        )
        return when {
            bitsWithPaddingNum == 64 -> bits
            bitsWithPaddingNum > 64 -> trunc(bits, llvm.int64Type)
            else -> ext(bits, llvm.int64Type, signed)
        }
    }

    private fun FunctionGenerationContext.emitWriteBits(args: List<LLVMValueRef>): LLVMValueRef {
        val ptr = args[0]
        assert(ptr.type == llvm.int8PtrType)

        val offset = extractConstUnsignedInt(args[1])
        val size = extractConstUnsignedInt(args[2]).toInt()

        val value = args[3]
        assert(value.type == llvm.int64Type)

        val bitsType = LLVMIntTypeInContext(llvm.llvmContext, size)!!

        val prefixBitsNum = (offset % 8).toInt()
        val suffixBitsNum = (8 - ((size + offset) % 8).toInt()) % 8

        val bitsWithPaddingNum = prefixBitsNum + size + suffixBitsNum
        val bitsWithPaddingType = LLVMIntTypeInContext(llvm.llvmContext, bitsWithPaddingNum)!!

        // 0011111000:
        val discardBitsMask = LLVMConstShl(
                LLVMConstZExt(
                        LLVMConstAllOnes(bitsType), // 11111
                        bitsWithPaddingType
                ), // 1111100000
                LLVMConstInt(bitsWithPaddingType, prefixBitsNum.toLong(), 0)
        )

        val preservedBitsMask = LLVMConstNot(discardBitsMask)!!

        val bitsWithPaddingPtr = bitcast(pointerType(bitsWithPaddingType), gep(ptr, llvm.int64(offset / 8)))

        val bits = trunc(value, bitsType)

        val bitsToStore = if (prefixBitsNum == 0 && suffixBitsNum == 0) {
            bits
        } else {
            val previousValue = load(bitsWithPaddingPtr).setUnaligned()
            val preservedBits = and(previousValue, preservedBitsMask)
            val bitsWithPadding = shl(zext(bits, bitsWithPaddingType), prefixBitsNum)

            or(bitsWithPadding, preservedBits)
        }
        LLVMBuildStore(builder, bitsToStore, bitsWithPaddingPtr)!!.setUnaligned()
        return codegen.theUnitInstanceRef.llvm
    }

    private fun FunctionGenerationContext.emitMemoryCopy(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        println("memcpy at ${callSite}")
        args.map { println(llvm2string(it)) }
        TODO("Implement me")
    }

    private fun FunctionGenerationContext.emitObjCCreateSuperStruct(args: List<LLVMValueRef>): LLVMValueRef {
        assert(args.size == 2)
        val receiver = args[0]
        val superClass = args[1]

        val structType = llvm.structType(llvm.int8PtrType, llvm.int8PtrType)
        val ptr = alloca(structType)
        store(receiver, LLVMBuildGEP(builder, ptr, cValuesOf(llvm.kImmInt32Zero, llvm.kImmInt32Zero), 2, "")!!)
        store(superClass, LLVMBuildGEP(builder, ptr, cValuesOf(llvm.kImmInt32Zero, llvm.kImmInt32One), 2, "")!!)
        return bitcast(llvm.int8PtrType, ptr)
    }

    private fun FunctionGenerationContext.emitGetObjCClass(callSite: IrCall): LLVMValueRef {
        val typeArgument = callSite.getTypeArgument(0)
        return getObjCClass(typeArgument!!.getClass()!!, environment.exceptionHandler)
    }

    private fun FunctionGenerationContext.emitObjCGetMessenger(args: List<LLVMValueRef>, isStret: Boolean): LLVMValueRef {
        val messengerNameSuffix = if (isStret) "_stret" else ""

        val functionReturnType = LlvmRetType(llvm.int8PtrType)
        val functionParameterTypes = listOf(LlvmParamType(llvm.int8PtrType), LlvmParamType(llvm.int8PtrType))

        val normalMessenger = codegen.llvm.externalNativeRuntimeFunction(
                "objc_msgSend$messengerNameSuffix",
                functionReturnType,
                functionParameterTypes,
                isVararg = true
        )
        val superMessenger = codegen.llvm.externalNativeRuntimeFunction(
                "objc_msgSendSuper$messengerNameSuffix",
                functionReturnType,
                functionParameterTypes,
                isVararg = true
        )

        val superClass = args.single()
        val messenger = LLVMBuildSelect(builder,
                If = icmpEq(superClass, llvm.kNullInt8Ptr),
                Then = normalMessenger.toConstPointer().llvm,
                Else = superMessenger.toConstPointer().llvm,
                Name = ""
        )!!

        return bitcast(llvm.int8PtrType, messenger)
    }

    private fun FunctionGenerationContext.emitAreEqualByValue(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        assert (first.type == second.type) { "Types are different: '${llvmtype2string(first.type)}' and '${llvmtype2string(second.type)}'" }

        return when (val typeKind = LLVMGetTypeKind(first.type)) {
            LLVMTypeKind.LLVMFloatTypeKind, LLVMTypeKind.LLVMDoubleTypeKind,
            LLVMTypeKind.LLVMVectorTypeKind -> {
                // TODO LLVM API does not provide guarantee for LLVMIntTypeInContext availability for longer types; consider meaningful diag message instead of NPE
                val integerType = LLVMIntTypeInContext(llvm.llvmContext, first.type.sizeInBits())!!
                icmpEq(bitcast(integerType, first), bitcast(integerType, second))
            }
            LLVMTypeKind.LLVMIntegerTypeKind, LLVMTypeKind.LLVMPointerTypeKind -> icmpEq(first, second)
            else -> error(typeKind)
        }
    }

    private fun FunctionGenerationContext.emitIeee754Equals(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        assert (first.type == second.type)
                { "Types are different: '${llvmtype2string(first.type)}' and '${llvmtype2string(second.type)}'" }
        val type = LLVMGetTypeKind(first.type)
        assert (type == LLVMTypeKind.LLVMFloatTypeKind || type == LLVMTypeKind.LLVMDoubleTypeKind)
                { "Should be of floating point kind, not: '${llvmtype2string(first.type)}'"}
        return fcmpEq(first, second)
    }

    private fun FunctionGenerationContext.emitReinterpret(callSite: IrCall, args: List<LLVMValueRef>) =
            bitcast(callSite.llvmReturnType, args[0])

    private fun FunctionGenerationContext.emitExtractElement(callSite: IrCall, args: List<LLVMValueRef>): LLVMValueRef {
        val (vector, index) = args
        val elementSize = LLVMSizeOfTypeInBits(codegen.llvmTargetData, callSite.llvmReturnType).toInt()
        val vectorSize = LLVMSizeOfTypeInBits(codegen.llvmTargetData, vector.type).toInt()

        assert(callSite.llvmReturnType.isVectorElementType()
                && vectorSize % elementSize == 0
        ) { "Invalid vector element type ${LLVMGetTypeKind(callSite.llvmReturnType)}"}

        val elementCount = vectorSize / elementSize
        emitThrowIfOOB(index, llvm.int32((elementCount)))

        val targetType = LLVMVectorType(callSite.llvmReturnType, elementCount)!!
        return extractElement(
                (if (targetType == vector.type) vector else bitcast(targetType, vector)),
                index)
    }

    private fun FunctionGenerationContext.emitNot(args: List<LLVMValueRef>) =
            not(args[0])

    private fun FunctionGenerationContext.emitPlus(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        return if (first.type.isFloatingPoint()) {
            fadd(first, second)
        } else {
            add(first, second)
        }
    }

    private fun FunctionGenerationContext.emitSignExtend(callSite: IrCall, args: List<LLVMValueRef>) =
            sext(args[0], callSite.llvmReturnType)

    private fun FunctionGenerationContext.emitZeroExtend(callSite: IrCall, args: List<LLVMValueRef>) =
            zext(args[0], callSite.llvmReturnType)

    private fun FunctionGenerationContext.emitIntTruncate(callSite: IrCall, args: List<LLVMValueRef>) =
            trunc(args[0], callSite.llvmReturnType)

    private fun FunctionGenerationContext.emitSignedToFloat(callSite: IrCall, args: List<LLVMValueRef>) =
            LLVMBuildSIToFP(builder, args[0], callSite.llvmReturnType, "")!!

    private fun FunctionGenerationContext.emitUnsignedToFloat(callSite: IrCall, args: List<LLVMValueRef>) =
            LLVMBuildUIToFP(builder, args[0], callSite.llvmReturnType, "")!!

    private fun FunctionGenerationContext.emitFloatToSigned(callSite: IrCall, args: List<LLVMValueRef>) =
            LLVMBuildFPToSI(builder, args[0], callSite.llvmReturnType, "")!!

    private fun FunctionGenerationContext.emitFloatExtend(callSite: IrCall, args: List<LLVMValueRef>) =
            LLVMBuildFPExt(builder, args[0], callSite.llvmReturnType, "")!!

    private fun FunctionGenerationContext.emitFloatTruncate(callSite: IrCall, args: List<LLVMValueRef>) =
            LLVMBuildFPTrunc(builder, args[0], callSite.llvmReturnType, "")!!

    private fun FunctionGenerationContext.emitShift(op: LLVMOpcode, args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        val shift = if (first.type == llvm.int64Type) {
            val tmp = and(second, llvm.int32(63))
            zext(tmp, llvm.int64Type)
        } else {
            and(second, llvm.int32(31))
        }
        return LLVMBuildBinOp(builder, op, first, shift, "")!!
    }

    private fun FunctionGenerationContext.emitShl(args: List<LLVMValueRef>) =
            emitShift(LLVMOpcode.LLVMShl, args)

    private fun FunctionGenerationContext.emitShr(args: List<LLVMValueRef>) =
            emitShift(LLVMOpcode.LLVMAShr, args)

    private fun FunctionGenerationContext.emitUshr(args: List<LLVMValueRef>) =
            emitShift(LLVMOpcode.LLVMLShr, args)

    private fun FunctionGenerationContext.emitAnd(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        return and(first, second)
    }

    private fun FunctionGenerationContext.emitOr(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        return or(first, second)
    }

    private fun FunctionGenerationContext.emitXor(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        return xor(first, second)
    }

    private fun FunctionGenerationContext.emitInv(args: List<LLVMValueRef>): LLVMValueRef {
        val first = args[0]
        val mask = makeConstOfType(first.type, -1)
        return xor(first, mask)
    }

    private fun FunctionGenerationContext.emitMinus(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        return if (first.type.isFloatingPoint()) {
            fsub(first, second)
        } else {
            sub(first, second)
        }
    }

    private fun FunctionGenerationContext.emitTimes(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        return if (first.type.isFloatingPoint()) {
            LLVMBuildFMul(builder, first, second, "")
        } else {
            LLVMBuildMul(builder, first, second, "")
        }!!
    }

    private fun FunctionGenerationContext.emitThrowIfZero(divider: LLVMValueRef) {
        ifThen(icmpEq(divider, Zero(divider.type).llvm)) {
            val throwArithExc = codegen.llvmFunction(context.ir.symbols.throwArithmeticException.owner)
            call(throwArithExc, emptyList(), Lifetime.GLOBAL, environment.exceptionHandler)
            unreachable()
        }
    }

    private fun FunctionGenerationContext.emitThrowIfOOB(index: LLVMValueRef, size: LLVMValueRef) {
        ifThen(icmpUGe(index, size)) {
            val throwIndexOutOfBoundsException = codegen.llvmFunction(context.ir.symbols.throwIndexOutOfBoundsException.owner)
            call(throwIndexOutOfBoundsException, emptyList(), Lifetime.GLOBAL, environment.exceptionHandler)
            unreachable()
        }
    }

    private fun FunctionGenerationContext.emitSignedDiv(args: List<LLVMValueRef>): LLVMValueRef {
        val (dividend, divisor) = args
        val divisorType = divisor.type
        return if (!divisorType.isFloatingPoint()) {
            emitThrowIfZero(divisor)
            emitSignedDivisionWithOverflow(dividend, divisor, divisorType, retZeroOnOverflow = false) {
                LLVMBuildSDiv(builder, dividend, divisor, "")!!
            }
        } else {
            LLVMBuildFDiv(builder, dividend, divisor, "")!!
        }
    }

    private fun FunctionGenerationContext.emitSignedRem(args: List<LLVMValueRef>): LLVMValueRef {
        val (dividend, divisor) = args
        val divisorType = divisor.type
        return if (!divisorType.isFloatingPoint()) {
            emitThrowIfZero(divisor)
            emitSignedDivisionWithOverflow(dividend, divisor, divisorType, retZeroOnOverflow = true) {
                LLVMBuildSRem(builder, dividend, divisor, "")!!
            }
        } else {
            LLVMBuildFRem(builder, dividend, divisor, "")!!
        }
    }

    private inline fun FunctionGenerationContext.emitSignedDivisionWithOverflow(
            dividend: LLVMValueRef,
            divisor: LLVMValueRef,
            type: LLVMTypeRef,
            retZeroOnOverflow: Boolean,
            nonOverflowValue: () -> LLVMValueRef
    ): LLVMValueRef {
        val minValue = when (val sizeInBits = type.sizeInBits()) {
            32 -> LLVMConstInt(type, Int.MIN_VALUE.toLong(), 1)!!
            64 -> LLVMConstInt(type, Long.MIN_VALUE, 1)!!
            else -> error("Unsupported signed integer division argument width: $sizeInBits")
        }

        val minusOne = LLVMConstInt(type, -1, 1)!!
        val overflowValue = if (retZeroOnOverflow) Zero(type).llvm else minValue

        return ifThenElse(and(icmpEq(dividend, minValue), icmpEq(divisor, minusOne)), overflowValue, nonOverflowValue)
    }

    private fun FunctionGenerationContext.emitUnsignedDiv(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        emitThrowIfZero(second)
        return LLVMBuildUDiv(builder, first, second, "")!!
    }

    private fun FunctionGenerationContext.emitUnsignedRem(args: List<LLVMValueRef>): LLVMValueRef {
        val (first, second) = args
        emitThrowIfZero(second)
        return LLVMBuildURem(builder, first, second, "")!!
    }

    private fun FunctionGenerationContext.emitInc(args: List<LLVMValueRef>): LLVMValueRef {
        val first = args[0]
        val const1 = makeConstOfType(first.type, 1)
        return if (first.type.isFloatingPoint()) {
            fadd(first, const1)
        } else {
            add(first, const1)
        }
    }

    private fun FunctionGenerationContext.emitDec(args: List<LLVMValueRef>): LLVMValueRef {
        val first = args[0]
        val const1 = makeConstOfType(first.type, 1)
        return if (first.type.isFloatingPoint()) {
            fsub(first, const1)
        } else {
            sub(first, const1)
        }
    }

    private fun FunctionGenerationContext.emitUnaryPlus(args: List<LLVMValueRef>) =
            args[0]

    private fun FunctionGenerationContext.emitUnaryMinus(args: List<LLVMValueRef>): LLVMValueRef {
        val first = args[0]
        val destTy = first.type
        return if (destTy.isFloatingPoint()) {
            fneg(first)
        } else {
            val const0 = makeConstOfType(destTy, 0)
            sub(const0, first)
        }
    }

    private fun FunctionGenerationContext.emitCompareTo(args: List<LLVMValueRef>, signed: Boolean): LLVMValueRef {
        val (first, second) = args
        val equal = icmpEq(first, second)
        val less = if (signed) icmpLt(first, second) else icmpULt(first, second)
        val tmp = select(less, llvm.int32(-1), llvm.int32(1))
        return select(equal, llvm.int32(0), tmp)
    }

    private fun FunctionGenerationContext.emitSignedCompareTo(args: List<LLVMValueRef>) =
            emitCompareTo(args, signed = true)

    private fun FunctionGenerationContext.emitUnsignedCompareTo(args: List<LLVMValueRef>) =
            emitCompareTo(args, signed = false)

    private fun FunctionGenerationContext.makeConstOfType(type: LLVMTypeRef, value: Int): LLVMValueRef = when (type) {
        llvm.int8Type -> llvm.int8(value.toByte())
        llvm.int16Type -> llvm.char16(value.toChar())
        llvm.int32Type -> llvm.int32(value)
        llvm.int64Type -> llvm.int64(value.toLong())
        llvm.floatType -> llvm.float32(value.toFloat())
        llvm.doubleType -> llvm.float64(value.toDouble())
        else -> context.reportCompilationError("Unexpected primitive type: $type")
    }
}