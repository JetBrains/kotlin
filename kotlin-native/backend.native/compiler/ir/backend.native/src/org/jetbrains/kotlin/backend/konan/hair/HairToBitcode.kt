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
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.NativeGenerationState;
import org.jetbrains.kotlin.backend.konan.llvm.CodeContext
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.CodeGeneratorVisitor.FunctionScope
import org.jetbrains.kotlin.backend.konan.llvm.ExceptionHandler
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.llvm.computeFullName
import org.jetbrains.kotlin.backend.konan.llvm.kNullObjHeaderPtr
import org.jetbrains.kotlin.backend.konan.llvm.kObjHeaderPtr
import org.jetbrains.kotlin.backend.konan.llvm.theUnitInstanceRef
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

    fun HairType.asLLLVMType() = when (this) {
        HairType.VOID -> llvm.voidType
        HairType.BOOLEAN -> llvm.int1Type
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
        val entryBLock = functionGenerationContext.currentBlock

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
                                is BlockEntry -> null // blocks[node]!!
                                is Param ->
                                    param(node.index)
                                is True -> llvm.constInt1(true).llvm
                                is False -> llvm.constInt1(false).llvm
                                is ConstI -> llvm.constInt32(node.value).llvm
                                is ConstL -> llvm.constInt64(node.value).llvm
                                is ConstF -> llvm.constFloat32(node.value).llvm
                                is ConstD -> llvm.constFloat64(node.value).llvm
                                is Null -> llvm.kNullObjHeaderPtr
                                is Add -> {
                                    // TODO respect type
                                    add(nodeValues[node.lhs]!!, nodeValues[node.rhs]!!)
                                }
                                // TODO other arithmetics
                                is Phi -> {
                                    deferredPhies += node
                                    functionGenerationContext.phi(node.type.asLLLVMType(), "phi_${node.id}")
                                }
                                is Return -> {
                                    // FIXME short ints
                                    val result = nodeValues[node.result]!!.let {
                                        if (returnType in listOf(llvm.int8Type, llvm.int16Type)) {
                                            trunc(it, returnType!!)
                                        } else it
                                    }
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
                                                trunc(nodeValues[node.cond]!!, llvm.int1Type),
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
                                                    trunc(nodeValues[node.cond]!!, llvm.int1Type),
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
                                    val res = call(
                                            llvmCallable = llvmTarget,
                                            args = node.callArgs.map { nodeValues[it]!! },
                                            resultLifetime = Lifetime.GLOBAL,
                                            exceptionHandler = ExceptionHandler.Caller, // FIXME proper exception handling
                                    )
                                    // TODO what about Unit?
                                    if (target.returnType.isNothing()) {
                                        // FIXME try to avoid dead code as the result of HaIR
                                        unreachable()
                                        codegen.theUnitInstanceRef.llvm
                                    } else res
                                }
                                is Cmp -> {
                                    val lhs = nodeValues[node.lhs]!!
                                    val rhs = nodeValues[node.rhs]!!
                                    when (node.op) {
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
                                    }
                                }
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

                functionGenerationContext.positionAtEnd(entryBLock)
                functionGenerationContext.br(blocks[entry]!!)
            }
        }
    }
}
