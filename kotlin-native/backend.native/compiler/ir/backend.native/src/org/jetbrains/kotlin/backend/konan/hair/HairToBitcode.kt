/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.hair

import hair.compilation.FunctionCompilation
import hair.graph.*
import hair.ir.*
import hair.ir.nodes.*
import hair.sym.CmpOp
import hair.sym.HairType
import hair.transform.GCMResult
import hair.transform.withGCM
import hair.utils.printGraphviz
import llvm.LLVMBasicBlockRef
import llvm.LLVMBuildMul
import llvm.LLVMBuildStructGEP2
import llvm.LLVMTypeOf
import llvm.LLVMTypeRef
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.NativeGenerationState;
import org.jetbrains.kotlin.backend.konan.binaryTypeIsReference
import org.jetbrains.kotlin.backend.konan.llvm.CodeContext
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.CodeGeneratorVisitor.FunctionScope
import org.jetbrains.kotlin.backend.konan.llvm.ExceptionHandler
import org.jetbrains.kotlin.backend.konan.llvm.FunctionGenerationContext
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.llvm.computeFullName
import org.jetbrains.kotlin.backend.konan.llvm.kNullObjHeaderPtr
import org.jetbrains.kotlin.backend.konan.llvm.kObjHeaderPtr
import org.jetbrains.kotlin.backend.konan.llvm.pointerType
import org.jetbrains.kotlin.backend.konan.llvm.theUnitInstanceRef
import org.jetbrains.kotlin.backend.konan.llvm.toLLVMType
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.isNothing

// TODO move to utils
context(gcm: GCMResult)
val gcm get() = gcm

internal class HairToBitcode(
        val generationState :NativeGenerationState,
        val codegen: CodeGenerator,
) {
    private val llvm = generationState.llvm
    private val context = generationState.context

    fun HairType.asLLVMType() = when (this) {
        HairType.VOID -> llvm.voidType
//        HairType.BOOLEAN -> llvm.int1Type
//        HairType.BYTE -> llvm.int8Type
//        HairType.SHORT -> llvm.int16Type
        HairType.INT -> llvm.int32Type
        HairType.LONG -> llvm.int64Type
        HairType.FLOAT -> llvm.floatType
        HairType.DOUBLE -> llvm.doubleType
        HairType.REFERENCE -> llvm.kObjHeaderPtr
        HairType.EXCEPTION -> llvm.voidPtrType
    }


    fun generateFunctionBody(
            currentCodeContext: CodeContext,
            declaration: IrFunction,
            hairComp: FunctionCompilation
    ) {
        context.log { "# Generating llvm from HaIR for ${declaration.name}" }
        val functionGenerationContext = (currentCodeContext.functionScope() as FunctionScope).functionGenerationContext
        val entryBlock = functionGenerationContext.currentBlock

        // FIXME copy&pasted from IrToBitcode
        fun staticFieldPtr(value: IrField, context: FunctionGenerationContext) =
                generationState.llvmDeclarations
                        .forStaticField(value.symbol.owner)
                        .storageAddressAccess
                        .getAddress(context)

        fun fieldPtrOfClass(thisPtr: LLVMValueRef, value: IrField): LLVMValueRef {
            val fieldInfo = generationState.llvmDeclarations.forField(value)
            val classBodyType = fieldInfo.classBodyType
            val typedBodyPtr = functionGenerationContext.bitcast(pointerType(classBodyType), thisPtr)
            val fieldPtr = LLVMBuildStructGEP2(functionGenerationContext.builder, classBodyType, typedBodyPtr, fieldInfo.index, "")
            return fieldPtr!!
        }

        fun adaptToHair(value: LLVMValueRef): LLVMValueRef = when (LLVMTypeOf(value)) {
            llvm.int1Type,
            llvm.int8Type,
            llvm.int16Type -> functionGenerationContext.sext(value, llvm.int32Type)
            else -> value
        }

        fun adaptFromHair(value: LLVMValueRef, targetType: LLVMTypeRef): LLVMValueRef = when (targetType) {
            llvm.int1Type,
            llvm.int8Type,
            llvm.int16Type -> functionGenerationContext.trunc(value, targetType)
            else -> value
        }

        val session = hairComp.session
        with (session) {
            withGCM {
                println("HaIR for ${declaration.computeFullName()} before codegen")
                printGraphviz()
                val blocks = topSort(cfg()).associateWith {
                    functionGenerationContext.basicBlock("block_${it.id}", null)
                }
                // FIXME codegen inserts additional blocks (e.g. around calls)
                //     so we can't rely on blocks map above to get the llvm's block of a random node
                val blockExitBlocks = mutableMapOf<BlockExit, LLVMBasicBlockRef>()
                val nodeValues = mutableMapOf<Node, LLVMValueRef>()
                val deferredPhies = mutableListOf<Phi>()

                for ((block, llvmBlock) in blocks) {
                    functionGenerationContext.appendingTo(llvmBlock) {
                        val blockNodes = gcm.linearOrder(block)
                        println("Generating $block nodes: $blockNodes")

                        fun genNode(node: Node) {
                            println("    Generating $node")
                            // context.log { "  generating $node" }
                            val value = when (node) {
                                is BlockEntry -> {
                                    // llvm requires phis to be generated first, but GCM can insert floating stuff inbetween
                                    for (phi in node.uses.filterIsInstance<Phi>()) {
                                        deferredPhies += phi
                                        nodeValues[phi] = functionGenerationContext.phi(phi.type.asLLVMType(), "phi_${node.id}")
                                    }
                                    null // blocks[node]!!
                                }
                                is Param -> {
                                    adaptToHair(param(node.index))
                                }
                                is True -> llvm.constInt32(1).llvm
                                is False -> llvm.constInt32(0).llvm
                                is ConstI -> llvm.constInt32(node.value).llvm
                                is ConstL -> llvm.constInt64(node.value).llvm
                                is ConstF -> llvm.constFloat32(node.value).llvm
                                is ConstD -> llvm.constFloat64(node.value).llvm
                                is Null -> llvm.kNullObjHeaderPtr

                                // TODO respect floating types
                                is Add -> {
                                    add(nodeValues[node.lhs]!!, nodeValues[node.rhs]!!)
                                }
                                is Sub -> sub(nodeValues[node.lhs]!!, nodeValues[node.rhs]!!)
                                is Mul -> LLVMBuildMul(builder, nodeValues[node.lhs]!!, nodeValues[node.rhs]!!, "")!!

                                // TODO divs

                                is And -> and(nodeValues[node.lhs]!!, nodeValues[node.rhs]!!)
                                is Or -> or(nodeValues[node.lhs]!!, nodeValues[node.rhs]!!)
                                is Xor -> xor(nodeValues[node.lhs]!!, nodeValues[node.rhs]!!)

                                // TODO shifts

                                is SignExtend -> sext(nodeValues[node.operand]!!, node.targetType.asLLVMType())
                                is ZeroExtend -> zext(nodeValues[node.operand]!!, node.targetType.asLLVMType())
                                is Truncate -> trunc(nodeValues[node.operand]!!, node.targetType.asLLVMType())

                                // TODO other arithmetics
                                is Phi -> {
                                    nodeValues[node]!! // should be generated by block
//                                    deferredPhies += node
//                                    functionGenerationContext.phi(node.type.asLLVMType(), "phi_${node.id}")
                                }
                                is Return -> {
                                    val result = adaptFromHair(nodeValues[node.result]!!, returnType!!)
                                    currentCodeContext.genReturn(declaration, result)
                                    null
                                }
                                is Goto -> {
                                    val bb = basicBlock("blockExit_${node.id}", null)
                                    br(bb)
                                    blockExitBlocks[node] = bb
                                    appendingTo(bb) {
                                        br(blocks[node.next]!!)
                                    }
                                    null
                                }
                                is If -> {
                                    if (node.trueExit.next == node.falseExit.next) {
                                        // Hack for the case when true and false targets are the same - llvm does not support this.
                                        val trueBB = basicBlock("trueExit_${node.id}", null)
                                        blockExitBlocks[node.trueExit] = trueBB
                                        val falseBB = basicBlock("falseExit_${node.id}", null)
                                        blockExitBlocks[node.falseExit] = falseBB
                                        condBr(
                                                adaptFromHair(nodeValues[node.cond]!!, llvm.int1Type),
                                                trueBB,
                                                falseBB
                                        )
                                        appendingTo(trueBB) {
                                            br(blocks[node.trueExit.next]!!)
                                        }
                                        appendingTo(falseBB) {
                                            br(blocks[node.falseExit.next]!!)
                                        }
                                    } else {
                                        val bb = basicBlock("blockEnd_${node.id}", null)
                                        br(bb)
                                        blockExitBlocks[node.trueExit] = bb
                                        blockExitBlocks[node.falseExit] = bb
                                        appendingTo(bb) {
                                            condBr(
                                                    adaptFromHair(nodeValues[node.cond]!!, llvm.int1Type),
                                                    blocks[node.trueExit.next]!!,
                                                    blocks[node.falseExit.next]!!
                                            )
                                        }
                                    }
                                }
                                is IfProjection -> null
                                is InvokeStatic -> {
                                    val target = (node.function as HairFunctionImpl).irFunction
                                    val llvmTarget = codegen.llvmFunction(target)
                                    // TODO all the stuff around
                                    val args = node.callArgs.zip(target.parameters).map { (arg, param) ->
                                        adaptFromHair(nodeValues[arg]!!, param.type.toLLVMType(llvm))
                                    }
                                    val res = call(
                                            llvmCallable = llvmTarget,
                                            args = args,
                                            resultLifetime = Lifetime.GLOBAL,
                                            exceptionHandler = ExceptionHandler.Caller, // FIXME proper exception handling
                                    )
                                    // TODO what about Unit?
                                    if (target.returnType.isNothing()) {
                                        // FIXME try to avoid dead code as the result of HaIR
                                        unreachable()
                                        codegen.theUnitInstanceRef.llvm
                                    } else adaptToHair(res)
                                }
                                is Cmp -> {
                                    val lhs = nodeValues[node.lhs]!!
                                    val rhs = nodeValues[node.rhs]!!
                                    adaptToHair(when (node.op) {
                                        CmpOp.EQ -> icmpEq(lhs, rhs)
                                        CmpOp.NE -> icmpNe(lhs, rhs)
                                        CmpOp.U_GT -> icmpUGt(lhs, rhs)
                                        CmpOp.U_GE -> icmpUGe(lhs, rhs)
                                        CmpOp.U_LT -> icmpULt(lhs, rhs)
                                        CmpOp.U_LE -> icmpULe(lhs, rhs)
                                        CmpOp.S_GT -> icmpGt(lhs, rhs)
                                        CmpOp.S_GE -> icmpGe(lhs, rhs)
                                        CmpOp.S_LT -> icmpLt(lhs, rhs)
                                        CmpOp.S_LE -> icmpLe(lhs, rhs)
                                    })
                                }

                                is LoadGlobal -> {
                                    val irField = (node.field as HairGlobalImpl).irField

                                    // TODO require(irField.correspondingPropertySymbol?.owner?.isConst != true)

                                    val fieldAddress = staticFieldPtr(irField, functionGenerationContext)
                                    val alignment = generationState.llvmDeclarations.forStaticField(irField).alignment

                                    adaptToHair(loadSlot(
                                            irField.type.toLLVMType(llvm),
                                            irField.type.binaryTypeIsReference(),
                                            fieldAddress,
                                            !irField.isFinal,
                                            resultSlot = null, // FIXME returnSlot !!!!!!!!!!!
                                            memoryOrder = null,
                                            alignment = alignment
                                    ))
                                }

                                is LoadField -> {
                                    val irField = (node.field as HairFieldImpl).irField

                                    // TODO require(irField.correspondingPropertySymbol?.owner?.isConst != true)

                                    val fieldAddress = fieldPtrOfClass(nodeValues[node.obj]!!, irField)
                                    val alignment = generationState.llvmDeclarations.forField(irField).alignment

                                    adaptToHair(loadSlot(
                                            irField.type.toLLVMType(llvm),
                                            irField.type.binaryTypeIsReference(),
                                            fieldAddress,
                                            !irField.isFinal,
                                            resultSlot = null, // FIXME returnSlot !!!!!!!!!!!
                                            memoryOrder = null,
                                            alignment = alignment
                                    ))
                                }

                                is StoreGlobal -> {
                                    val irField = (node.field as HairGlobalImpl).irField

                                    val fieldAddress = staticFieldPtr(irField, functionGenerationContext)
                                    val alignment = generationState.llvmDeclarations.forStaticField(irField).alignment

                                    storeAny(
                                            adaptFromHair(nodeValues[node.value]!!, irField.type.toLLVMType(llvm)),
                                            fieldAddress,
                                            irField.type.binaryTypeIsReference(),
                                            onStack = false,
                                            isVolatile = false,
                                            alignment = alignment,
                                    )
                                    null
                                }

                                is StoreField -> {
                                    val irField = (node.field as HairFieldImpl).irField

                                    // TODO special handling for field initialization

                                    val fieldAddress = fieldPtrOfClass(nodeValues[node.obj]!!, irField)
                                    val alignment = generationState.llvmDeclarations.forField(irField).alignment

                                    storeAny(
                                            adaptFromHair(nodeValues[node.value]!!, irField.type.toLLVMType(llvm)),
                                            fieldAddress,
                                            irField.type.binaryTypeIsReference(),
                                            onStack = false,
                                            isVolatile = false,
                                            alignment = alignment,
                                    )
                                    null
                                }

                                is New -> {
                                    val irClass = (node.objectType as HairClassImpl).irClass
                                    val typeInfo = codegen.typeInfoForAllocation(irClass)
                                    call(
                                            llvm.allocInstanceFunction,
                                            listOf(typeInfo),
                                            Lifetime.GLOBAL,
                                    )
                                }

                                is GlobalInit -> null // TODO()
                                is ThreadLocalInit -> null // TODO()
                                is StandaloneThreadLocalInit -> null // TODO()

                                // TODO
                                is CheckCast -> null
                                is IsInstanceOf -> llvm.constInt1(true).llvm

                                is UnitValue -> codegen.theUnitInstanceRef.llvm
                                is Unreachable -> unreachable()
                                else -> TODO(node.toString())
                            }
                            if (value != null) {
                                nodeValues[node] = value
                            }
                        }

                        for (node in blockNodes) {
                            genNode(node)
                        }
                    }
                }
                for (phi in deferredPhies) {
                    val llvmPhi = nodeValues[phi]!!

                    val incoming = phi.inputs.map { (value, blockExit) ->
                        val inBlock = blockExitBlocks[blockExit] ?: error("Node BB for $blockExit")
                        val inValue = nodeValues[value] ?: error("No value generated for input $value of $phi")
                        inBlock to inValue
                    }
                    functionGenerationContext.addPhiIncoming(llvmPhi, *incoming.toTypedArray())
                }

                functionGenerationContext.positionAtEnd(entryBlock)
                functionGenerationContext.br(blocks[entry]!!)
            }
        }
    }
}
