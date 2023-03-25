/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

import llvm.LLVMLinkage
import llvm.LLVMSetLinkage
import llvm.LLVMStoreSizeOfType
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.objcexport.BlockPointerBridge
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.util.simpleFunctions
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun ObjCExportCodeGeneratorBase.generateBlockToKotlinFunctionConverter(
        bridge: BlockPointerBridge
): LlvmCallable {
    val irInterface = symbols.functionN(bridge.numberOfParameters).owner
    val invokeMethod = irInterface.declarations.filterIsInstance<IrSimpleFunction>()
            .single { it.name == OperatorNameConventions.INVOKE }

    // Note: we can store Objective-C block pointer as associated object of Kotlin function object itself,
    // but only if it is equivalent to its dynamic translation result. If block returns void, then it's not like that:
    val useSeparateHolder = bridge.returnsVoid

    val bodyType = if (useSeparateHolder) {
        llvm.structType(codegen.kObjHeader, codegen.kObjHeaderPtr)
    } else {
        llvm.structType(codegen.kObjHeader)
    }

    val invokeImpl = functionGenerator(
            LlvmFunctionSignature(invokeMethod, codegen).toProto(
                    "invokeFunction${bridge.nameSuffix}",
                    null,
                    LLVMLinkage.LLVMInternalLinkage
            )
    ).generate {
        val thisRef = param(0)
        val associatedObjectHolder = if (useSeparateHolder) {
            val bodyPtr = bitcast(pointerType(bodyType), thisRef)
            loadSlot(structGep(bodyPtr, 1), isVar = false)
        } else {
            thisRef
        }
        val blockPtr = callFromBridge(
                llvm.Kotlin_ObjCExport_GetAssociatedObject,
                listOf(associatedObjectHolder)
        )

        val invoke = loadBlockInvoke(blockPtr, bridge)

        val args = (0 until bridge.numberOfParameters).map { index ->
            kotlinReferenceToRetainedObjC(param(index + 1))
        }

        switchThreadStateIfExperimentalMM(ThreadState.Native)
        // Using terminatingExceptionHandler, so any exception thrown by `invoke` will lead to the termination,
        // and switching the thread state back to `Runnable` on exceptional path is not required.
        val result = callAndMaybeRetainAutoreleased(
                invoke,
                bridge.blockType.toBlockInvokeLlvmType(llvm),
                listOf(blockPtr) + args,
                exceptionHandler = terminatingExceptionHandler,
                doRetain = !bridge.returnsVoid
        )
        args.forEach {
            objcReleaseFromNativeThreadState(it)
        }

        switchThreadStateIfExperimentalMM(ThreadState.Runnable)

        val kotlinResult = if (bridge.returnsVoid) {
            theUnitInstanceRef.llvm
        } else {
            // TODO: in some cases the sequence below will have redundant retain-release pair.
            // We could implement an optimized objCRetainedReferenceToKotlin, which takes ownership
            // of its argument (i.e. consumes retained reference).
            objCReferenceToKotlin(result, Lifetime.RETURN_VALUE)
                    .also { objcReleaseFromRunnableThreadState(result) }
        }
        ret(kotlinResult)
    }

    val typeInfo = rttiGenerator.generateSyntheticInterfaceImpl(
            irInterface,
            mapOf(invokeMethod to invokeImpl.toConstPointer()),
            bodyType,
            immutable = true
    )
    val functionSig = LlvmFunctionSignature(LlvmRetType(codegen.kObjHeaderPtr), listOf(LlvmParamType(llvm.int8PtrType), LlvmParamType(codegen.kObjHeaderPtrPtr)))
    return functionGenerator(
            functionSig.toProto("convertBlock${bridge.nameSuffix}", null, LLVMLinkage.LLVMInternalLinkage)
    ).generate {
        val blockPtr = param(0)
        ifThen(icmpEq(blockPtr, llvm.kNullInt8Ptr)) {
            ret(kNullObjHeaderPtr)
        }

        val retainedBlockPtr = callFromBridge(retainBlock, listOf(blockPtr))

        val result = if (useSeparateHolder) {
            val result = allocInstance(typeInfo.llvm, Lifetime.RETURN_VALUE, null)
            val bodyPtr = bitcast(pointerType(bodyType), result)
            val holder = allocInstanceWithAssociatedObject(
                    symbols.interopForeignObjCObject.owner.typeInfoPtr,
                    retainedBlockPtr,
                    Lifetime.ARGUMENT
            )
            storeHeapRef(holder, structGep(bodyPtr, 1))
            result
        } else {
            allocInstanceWithAssociatedObject(typeInfo, retainedBlockPtr, Lifetime.RETURN_VALUE)
        }

        ret(result)
    }
}

private fun FunctionGenerationContext.loadBlockInvoke(
        blockPtr: LLVMValueRef,
        bridge: BlockPointerBridge
): LlvmCallable {
    val invokePtr = structGep(bitcast(pointerType(codegen.runtime.blockLiteralType), blockPtr), 3)
    val signature = bridge.blockType.toBlockInvokeLlvmType(llvm)

    return LlvmCallable(bitcast(pointerType(signature.llvmFunctionType), load(invokePtr)), signature)
}

private fun FunctionGenerationContext.allocInstanceWithAssociatedObject(
        typeInfo: ConstPointer,
        associatedObject: LLVMValueRef,
        resultLifetime: Lifetime
): LLVMValueRef = call(
        llvm.Kotlin_ObjCExport_AllocInstanceWithAssociatedObject,
        listOf(typeInfo.llvm, associatedObject),
        resultLifetime
)

private val BlockPointerBridge.blockType: BlockType
    get() = BlockType(numberOfParameters = this.numberOfParameters, returnsVoid = this.returnsVoid)

/**
 * Type of block having [numberOfParameters] reference-typed parameters and reference- or void-typed return value.
 */
internal data class BlockType(val numberOfParameters: Int, val returnsVoid: Boolean)

private fun BlockType.toBlockInvokeLlvmType(llvm: CodegenLlvmHelpers): LlvmFunctionSignature =
        LlvmFunctionSignature(
                LlvmRetType(if (returnsVoid) llvm.voidType else llvm.int8PtrType),
                (0..numberOfParameters).map { LlvmParamType(llvm.int8PtrType) }
        )

private val BlockPointerBridge.nameSuffix: String
    get() = numberOfParameters.toString() + if (returnsVoid) "V" else ""

internal class BlockGenerator(private val codegen: CodeGenerator) {
    private val llvm = codegen.llvm

    private val blockLiteralType = llvm.structType(
            codegen.runtime.blockLiteralType,
            codegen.runtime.kRefSharedHolderType
    )

    val disposeProto = LlvmFunctionSignature(
            LlvmRetType(llvm.voidType),
            listOf(LlvmParamType(llvm.int8PtrType))
    ).toProto(
            "blockDisposeHelper",
            null,
            LLVMLinkage.LLVMInternalLinkage
    )

    val disposeHelper = generateFunction(
            codegen,
            disposeProto,
            switchToRunnable = true
    ) {
        val blockPtr = bitcast(pointerType(blockLiteralType), param(0))
        val refHolder = structGep(blockPtr, 1)
        call(llvm.kRefSharedHolderDispose, listOf(refHolder))

        ret(null)
    }

    val copyProto = LlvmFunctionSignature(
            LlvmRetType(llvm.voidType),
            listOf(LlvmParamType(llvm.int8PtrType), LlvmParamType(llvm.int8PtrType))
    ).toProto(
            "blockCopyHelper",
            null,
            LLVMLinkage.LLVMInternalLinkage
    )

    val copyHelper = generateFunction(
            codegen,
            copyProto,
    ) {
        val dstBlockPtr = bitcast(pointerType(blockLiteralType), param(0))
        val dstRefHolder = structGep(dstBlockPtr, 1)

        val srcBlockPtr = bitcast(pointerType(blockLiteralType), param(1))
        val srcRefHolder = structGep(srcBlockPtr, 1)

        // Note: in current implementation copy helper is invoked only for stack-allocated blocks from the same thread,
        // so it is technically not necessary to check owner.
        // However this is not guaranteed by Objective-C runtime, so keep it suboptimal but reliable:
        val ref = call(
                llvm.kRefSharedHolderRef,
                listOf(srcRefHolder),
                exceptionHandler = ExceptionHandler.Caller,
                verbatim = true
        )

        call(llvm.kRefSharedHolderInit, listOf(dstRefHolder, ref))

        ret(null)
    }

    fun CodeGenerator.LongInt(value: Long) =
            when (val longWidth = llvm.longTypeWidth) {
                32L -> llvm.constInt32(value.toInt())
                64L -> llvm.constInt64(value)
                else -> error("Unexpected width of long type: $longWidth")
            }

    private fun generateDescriptorForBlock(blockType: BlockType): ConstValue {
        val numberOfParameters = blockType.numberOfParameters

        val signature = buildString {
            append(if (blockType.returnsVoid) 'v' else '@')
            val pointerSize = codegen.runtime.pointerSize
            append(pointerSize * (numberOfParameters + 1))

            var paramOffset = 0L

            (0 .. numberOfParameters).forEach { index ->
                append('@')
                if (index == 0) append('?')
                append(paramOffset)
                paramOffset += pointerSize
            }
        }

        return Struct(codegen.runtime.blockDescriptorType,
                codegen.LongInt(0L),
                codegen.LongInt(LLVMStoreSizeOfType(codegen.runtime.targetData, blockLiteralType)),
                copyHelper.toConstPointer(),
                disposeHelper.toConstPointer(),
                codegen.staticData.cStringLiteral(signature),
                NullPointer(llvm.int8Type)
        )
    }


    private fun ObjCExportCodeGeneratorBase.generateInvoke(
            blockType: BlockType,
            invokeName: String,
            genBody: ObjCExportFunctionGenerationContext.(LLVMValueRef, List<LLVMValueRef>) -> Unit
    ): ConstPointer {
        val result = functionGenerator(blockType.toBlockInvokeLlvmType(llvm).toProto(invokeName, null, LLVMLinkage.LLVMInternalLinkage)) {
            switchToRunnable = true
        }.generate {
            val blockPtr = bitcast(pointerType(blockLiteralType), param(0))
            val kotlinObject = call(
                    llvm.kRefSharedHolderRef,
                    listOf(structGep(blockPtr, 1)),
                    exceptionHandler = ExceptionHandler.Caller,
                    verbatim = true
            )

            val arguments = (1 .. blockType.numberOfParameters).map { index -> param(index) }

            genBody(kotlinObject, arguments)
        }

        return result.toConstPointer()
    }

    fun ObjCExportCodeGeneratorBase.generateConvertFunctionToRetainedBlock(
            bridge: BlockPointerBridge
    ): LlvmCallable {
        return generateWrapKotlinObjectToRetainedBlock(
                bridge.blockType,
                convertName = "convertFunction${bridge.nameSuffix}",
                invokeName = "invokeBlock${bridge.nameSuffix}"
        ) { kotlinFunction, arguments ->
            val numberOfParameters = bridge.numberOfParameters

            val kotlinArguments = arguments.map { objCReferenceToKotlin(it, Lifetime.ARGUMENT) }

            val invokeMethod = context.ir.symbols.functionN(numberOfParameters).owner.simpleFunctions()
                    .single { it.name == OperatorNameConventions.INVOKE }
            val llvmDeclarations = codegen.getVirtualFunctionTrampoline(invokeMethod)
            val result = callFromBridge(llvmDeclarations, listOf(kotlinFunction) + kotlinArguments, Lifetime.ARGUMENT)
            if (bridge.returnsVoid) {
                ret(null)
            } else {
                autoreleaseAndRet(kotlinReferenceToRetainedObjC(result))
            }
        }
    }

    internal fun ObjCExportCodeGeneratorBase.generateWrapKotlinObjectToRetainedBlock(
            blockType: BlockType,
            convertName: String,
            invokeName: String,
            genBlockBody: ObjCExportFunctionGenerationContext.(LLVMValueRef, List<LLVMValueRef>) -> Unit
    ): LlvmCallable {
        val blockDescriptor = codegen.staticData.placeGlobal(
                "",
                generateDescriptorForBlock(blockType)
        )

        return functionGenerator(
                LlvmFunctionSignature(LlvmRetType(llvm.int8PtrType), listOf(LlvmParamType(codegen.kObjHeaderPtr))).toProto(
                    convertName, null, LLVMLinkage.LLVMInternalLinkage
                )
        ).generate {
            val kotlinRef = param(0)
            ifThen(icmpEq(kotlinRef, kNullObjHeaderPtr)) {
                ret(llvm.kNullInt8Ptr)
            }

            val isa = codegen.importObjCGlobal("_NSConcreteStackBlock", llvm.int8PtrType)

            val flags = llvm.int32((1 shl 25) or (1 shl 30) or (1 shl 31))
            val reserved = llvm.int32(0)

            val invokeType = pointerType(functionType(llvm.voidType, true, llvm.int8PtrType))
            val invoke = generateInvoke(blockType, invokeName, genBlockBody).bitcast(invokeType).llvm
            val descriptor = blockDescriptor.llvmGlobal

            val blockOnStack = alloca(blockLiteralType)
            val blockOnStackBase = structGep(blockOnStack, 0)
            val refHolder = structGep(blockOnStack, 1)

            listOf(bitcast(llvm.int8PtrType, isa), flags, reserved, invoke, descriptor).forEachIndexed { index, value ->
                // Although value is actually on the stack, it's not in normal slot area, so we cannot handle it
                // as if it was on the stack.
                store(value, structGep(blockOnStackBase, index))
            }

            call(llvm.kRefSharedHolderInitLocal, listOf(refHolder, kotlinRef))

            val copiedBlock = callFromBridge(retainBlock, listOf(bitcast(llvm.int8PtrType, blockOnStack)))

            ret(copiedBlock)
        }
    }
}

private val ObjCExportCodeGeneratorBase.retainBlock: LlvmCallable
    get() = llvm.externalNativeRuntimeFunction(
            "objc_retainBlock",
            LlvmRetType(llvm.int8PtrType),
            listOf(LlvmParamType(llvm.int8PtrType))
    )