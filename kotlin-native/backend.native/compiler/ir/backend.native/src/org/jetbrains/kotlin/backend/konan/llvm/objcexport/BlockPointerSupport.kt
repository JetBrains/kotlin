/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.kotlin.backend.konan.llvm.objcexport

import llvm.*
import org.jetbrains.kotlin.backend.common.ir.simpleFunctions
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.objcexport.BlockPointerBridge
import org.jetbrains.kotlin.descriptors.konan.CurrentKlibModuleOrigin
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.util.OperatorNameConventions

internal fun ObjCExportCodeGeneratorBase.generateBlockToKotlinFunctionConverter(
        bridge: BlockPointerBridge
): LLVMValueRef {
    val irInterface = symbols.functionN(bridge.numberOfParameters).owner
    val invokeMethod = irInterface.declarations.filterIsInstance<IrSimpleFunction>()
            .single { it.name == OperatorNameConventions.INVOKE }

    // Note: we can store Objective-C block pointer as associated object of Kotlin function object itself,
    // but only if it is equivalent to its dynamic translation result. If block returns void, then it's not like that:
    val useSeparateHolder = bridge.returnsVoid

    val bodyType = if (useSeparateHolder) {
        structType(codegen.kObjHeader, codegen.kObjHeaderPtr)
    } else {
        structType(codegen.kObjHeader)
    }

    val invokeImpl = functionGenerator(
            LlvmFunctionSignature(invokeMethod, codegen),
            "invokeFunction${bridge.nameSuffix}"
    ).generate {
        val args = (0 until bridge.numberOfParameters).map { index ->
            kotlinReferenceToLocalObjC(param(index + 1))
        }

        val thisRef = param(0)
        val associatedObjectHolder = if (useSeparateHolder) {
            val bodyPtr = bitcast(pointerType(bodyType), thisRef)
            loadSlot(structGep(bodyPtr, 1), isVar = false)
        } else {
            thisRef
        }
        val blockPtr = callFromBridge(
                context.llvm.Kotlin_ObjCExport_GetAssociatedObject,
                listOf(associatedObjectHolder)
        )

        val invoke = loadBlockInvoke(blockPtr, bridge)
        switchThreadStateIfExperimentalMM(ThreadState.Native)
        // Using terminatingExceptionHandler, so any exception thrown by `invoke` will lead to the termination,
        // and switching the thread state back to `Runnable` on exceptional path is not required.
        val result = call(invoke, listOf(blockPtr) + args, exceptionHandler = terminatingExceptionHandler)
        switchThreadStateIfExperimentalMM(ThreadState.Runnable)

        val kotlinResult = if (bridge.returnsVoid) {
            theUnitInstanceRef.llvm
        } else {
            objCReferenceToKotlin(result, Lifetime.RETURN_VALUE)
        }
        ret(kotlinResult)
    }.also {
        LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
    }

    val typeInfo = rttiGenerator.generateSyntheticInterfaceImpl(
            irInterface,
            mapOf(invokeMethod to constPointer(invokeImpl)),
            bodyType,
            immutable = true
    )
    return functionGenerator(
            LlvmFunctionSignature(LlvmRetType(codegen.kObjHeaderPtr), listOf(LlvmParamType(int8TypePtr), LlvmParamType(codegen.kObjHeaderPtrPtr))),
            "convertBlock${bridge.nameSuffix}"
    ).generate {
        val blockPtr = param(0)
        ifThen(icmpEq(blockPtr, kNullInt8Ptr)) {
            ret(kNullObjHeaderPtr)
        }

        val retainedBlockPtr = callFromBridge(retainBlock, listOf(blockPtr))

        val result = if (useSeparateHolder) {
            val result = allocInstance(typeInfo.llvm, Lifetime.RETURN_VALUE)
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
    }.also {
        LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
    }
}

private fun FunctionGenerationContext.loadBlockInvoke(
        blockPtr: LLVMValueRef,
        bridge: BlockPointerBridge
): LLVMValueRef {
    val blockLiteralType = codegen.runtime.getStructType("Block_literal_1")
    val invokePtr = structGep(bitcast(pointerType(blockLiteralType), blockPtr), 3)

    return bitcast(pointerType(bridge.blockType.blockInvokeLlvmType.llvmFunctionType), load(invokePtr))
}

private fun FunctionGenerationContext.allocInstanceWithAssociatedObject(
        typeInfo: ConstPointer,
        associatedObject: LLVMValueRef,
        resultLifetime: Lifetime
): LLVMValueRef = call(
        context.llvm.Kotlin_ObjCExport_AllocInstanceWithAssociatedObject,
        listOf(typeInfo.llvm, associatedObject),
        resultLifetime
)

private val BlockPointerBridge.blockType: BlockType
    get() = BlockType(numberOfParameters = this.numberOfParameters, returnsVoid = this.returnsVoid)

/**
 * Type of block having [numberOfParameters] reference-typed parameters and reference- or void-typed return value.
 */
internal data class BlockType(val numberOfParameters: Int, val returnsVoid: Boolean)

private val BlockType.blockInvokeLlvmType: LlvmFunctionSignature
    get() = LlvmFunctionSignature(
            LlvmRetType(if (returnsVoid) voidType else int8TypePtr),
            (0..numberOfParameters).map { LlvmParamType(int8TypePtr) }
    )

private val BlockPointerBridge.nameSuffix: String
    get() = numberOfParameters.toString() + if (returnsVoid) "V" else ""

internal class BlockGenerator(private val codegen: CodeGenerator) {
    private val kRefSharedHolderType = LLVMGetTypeByName(codegen.runtime.llvmModule, "class.KRefSharedHolder")!!

    private val blockLiteralType = structType(
            codegen.runtime.getStructType("Block_literal_1"),
            kRefSharedHolderType
    )

    private val blockDescriptorType = codegen.runtime.getStructType("Block_descriptor_1")

    val disposeHelper = generateFunction(
            codegen,
            functionType(voidType, false, int8TypePtr),
            "blockDisposeHelper",
            switchToRunnable = true
    ) {
        val blockPtr = bitcast(pointerType(blockLiteralType), param(0))
        val refHolder = structGep(blockPtr, 1)
        call(context.llvm.kRefSharedHolderDispose, listOf(refHolder))

        ret(null)
    }.also {
        LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
    }

    val copyHelper = generateFunction(
            codegen,
            functionType(voidType, false, int8TypePtr, int8TypePtr),
            "blockCopyHelper"
    ) {
        val dstBlockPtr = bitcast(pointerType(blockLiteralType), param(0))
        val dstRefHolder = structGep(dstBlockPtr, 1)

        val srcBlockPtr = bitcast(pointerType(blockLiteralType), param(1))
        val srcRefHolder = structGep(srcBlockPtr, 1)

        // Note: in current implementation copy helper is invoked only for stack-allocated blocks from the same thread,
        // so it is technically not necessary to check owner.
        // However this is not guaranteed by Objective-C runtime, so keep it suboptimal but reliable:
        val ref = call(
                context.llvm.kRefSharedHolderRef,
                listOf(srcRefHolder),
                exceptionHandler = ExceptionHandler.Caller,
                verbatim = true
        )

        call(context.llvm.kRefSharedHolderInit, listOf(dstRefHolder, ref))

        ret(null)
    }.also {
        LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
    }

    fun org.jetbrains.kotlin.backend.konan.Context.LongInt(value: Long) =
            when (val longWidth = llvm.longTypeWidth) {
                32L -> Int32(value.toInt())
                64L -> Int64(value)
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

        return Struct(blockDescriptorType,
                codegen.context.LongInt(0L),
                codegen.context.LongInt(LLVMStoreSizeOfType(codegen.runtime.targetData, blockLiteralType)),
                constPointer(copyHelper),
                constPointer(disposeHelper),
                codegen.staticData.cStringLiteral(signature),
                NullPointer(int8Type)
        )
    }


    private fun ObjCExportCodeGeneratorBase.generateInvoke(
            blockType: BlockType,
            invokeName: String,
            genBody: ObjCExportFunctionGenerationContext.(LLVMValueRef, List<LLVMValueRef>) -> Unit
    ): ConstPointer {
        val result = functionGenerator(blockType.blockInvokeLlvmType, invokeName) {
            switchToRunnable = true
        }.generate {
            val blockPtr = bitcast(pointerType(blockLiteralType), param(0))
            val kotlinObject = call(
                    context.llvm.kRefSharedHolderRef,
                    listOf(structGep(blockPtr, 1)),
                    exceptionHandler = ExceptionHandler.Caller,
                    verbatim = true
            )

            val arguments = (1 .. blockType.numberOfParameters).map { index -> param(index) }

            genBody(kotlinObject, arguments)
        }.also {
            LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
        }

        return constPointer(result)
    }

    fun ObjCExportCodeGeneratorBase.generateConvertFunctionToRetainedBlock(
            bridge: BlockPointerBridge
    ): LLVMValueRef {
        return generateWrapKotlinObjectToRetainedBlock(
                bridge.blockType,
                convertName = "convertFunction${bridge.nameSuffix}",
                invokeName = "invokeBlock${bridge.nameSuffix}"
        ) { kotlinFunction, arguments ->
            val numberOfParameters = bridge.numberOfParameters

            val kotlinArguments = arguments.map { objCReferenceToKotlin(it, Lifetime.ARGUMENT) }

            val invokeMethod = context.ir.symbols.functionN(numberOfParameters).owner.simpleFunctions()
                    .single { it.name == OperatorNameConventions.INVOKE }
            val llvmDeclarations = lookupVirtualImpl(kotlinFunction, invokeMethod)
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
    ): LLVMValueRef {
        val blockDescriptor = codegen.staticData.placeGlobal(
                "",
                generateDescriptorForBlock(blockType)
        )

        return functionGenerator(
                LlvmFunctionSignature(LlvmRetType(int8TypePtr), listOf(LlvmParamType(codegen.kObjHeaderPtr))),
                convertName
        ).generate {
            val kotlinRef = param(0)
            ifThen(icmpEq(kotlinRef, kNullObjHeaderPtr)) {
                ret(kNullInt8Ptr)
            }

            val isa = codegen.importGlobal(
                    "_NSConcreteStackBlock",
                    int8TypePtr,
                    CurrentKlibModuleOrigin
            )

            val flags = Int32((1 shl 25) or (1 shl 30) or (1 shl 31)).llvm
            val reserved = Int32(0).llvm

            val invokeType = pointerType(functionType(voidType, true, int8TypePtr))
            val invoke = generateInvoke(blockType, invokeName, genBlockBody).bitcast(invokeType).llvm
            val descriptor = blockDescriptor.llvmGlobal

            val blockOnStack = alloca(blockLiteralType)
            val blockOnStackBase = structGep(blockOnStack, 0)
            val refHolder = structGep(blockOnStack, 1)

            listOf(bitcast(int8TypePtr, isa), flags, reserved, invoke, descriptor).forEachIndexed { index, value ->
                // Although value is actually on the stack, it's not in normal slot area, so we cannot handle it
                // as if it was on the stack.
                store(value, structGep(blockOnStackBase, index))
            }

            call(context.llvm.kRefSharedHolderInitLocal, listOf(refHolder, kotlinRef))

            val copiedBlock = callFromBridge(retainBlock, listOf(bitcast(int8TypePtr, blockOnStack)))

            ret(copiedBlock)
        }.also {
            LLVMSetLinkage(it, LLVMLinkage.LLVMInternalLinkage)
        }
    }
}

private val ObjCExportCodeGeneratorBase.retainBlock: LlvmCallable
    get() {
        val functionProto = LlvmFunctionProto(
                "objc_retainBlock",
                LlvmRetType(int8TypePtr),
                listOf(LlvmParamType(int8TypePtr)),
                origin = CurrentKlibModuleOrigin
        )
        return context.llvm.externalFunction(functionProto)
    }