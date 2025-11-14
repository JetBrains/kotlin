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
import hair.transform.GCMResult
import hair.transform.withGCM
import hair.utils.printGraphviz
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.NativeGenerationState;
import org.jetbrains.kotlin.backend.konan.llvm.CodeContext
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.CodeGeneratorVisitor.FunctionScope
import org.jetbrains.kotlin.backend.konan.llvm.ExceptionHandler
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
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

    fun generateFunctionBody(
            currentCodeContext: CodeContext,
            declaration: IrFunction,
            hairComp: FunctionCompilation
    ) {
        context.log { "# Generating llvm from HaIR for ${declaration.name}" }
        val functionGenerationContext = (currentCodeContext.functionScope() as FunctionScope).functionGenerationContext

        val session = hairComp.session
        with (session) {
            withGCM {
                println("HaIR for ${declaration.name} before codegen")
                printGraphviz()
                val blocks = topSort(cfg()).associateWith {
                    functionGenerationContext.basicBlock("block_${it.id}", null)
                }
                val nodeValues = mutableMapOf<Node, LLVMValueRef>()
                val deferredPhies = mutableListOf<Phi>()

                for ((block, llvmBlock) in blocks) {
                    println("Generating $block")
                    functionGenerationContext.appendingTo(llvmBlock) {
                        val blockNodes = gcm.linearOrder(block)

                        fun genNode(node: Node) {
                            println("    Generating $node")
                            // context.log { "  generating $node" }
                            val value = when (node) {
                                is BlockEntry -> null // blocks[node]!!
                                is Param ->
                                    functionGenerationContext.param(node.index)
                                is ConstI -> llvm.constInt32(node.value).llvm
                                is ConstL -> llvm.constInt64(node.value).llvm
                                is ConstF -> llvm.constFloat32(node.value).llvm
                                is ConstD -> llvm.constFloat64(node.value).llvm
                                is Add -> {
                                    // TODO respect type
                                    functionGenerationContext.add(nodeValues[node.lhs]!!, nodeValues[node.rhs]!!)
                                }
                                // TODO other arithmetics
                                is Phi -> {
                                    deferredPhies += node
                                    functionGenerationContext.phi(this.llvm.int32Type, "phi_${node.id}")
                                }
                                is Return -> {
                                    currentCodeContext.genReturn(declaration, nodeValues[node.result]!!)
                                    null
                                }
                                is Goto -> {
                                    functionGenerationContext.br(blocks[node.next]!!)
                                    null
                                }
                                is If -> {
                                    functionGenerationContext.condBr(
                                            nodeValues[node.cond]!!,
                                            blocks[node.trueExit.next]!!,
                                            blocks[node.falseExit.next]
                                    )
                                }
                                is IfProjection -> null
                                is InvokeStatic -> {
                                    val target = (node.function as HairFunctionImpl).irFunction
                                    val llvmTarget = codegen.llvmFunction(target)
                                    // TODO all the stuff around
                                    val res = functionGenerationContext.call(
                                            llvmCallable = llvmTarget,
                                            args = node.callArgs.map { nodeValues[it]!! },
                                            resultLifetime = Lifetime.GLOBAL,
                                            exceptionHandler = ExceptionHandler.Caller, // FIXME proper exception handling
                                    )
                                    if (target.returnType.isNothing()) {
                                        functionGenerationContext.unreachable()
                                    }
                                    // TODO what sbout Unit?
                                    res
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

                    val incoming = phi.inputs.map { (value, blockEnd) ->
                        val inBlock = blocks[blockEnd.block]!!
                        val inValue = nodeValues[value] ?: error("No value generated for input $value of $phi")
                        inBlock to inValue
                    }
                    functionGenerationContext.addPhiIncoming(llvmPhi, *incoming.toTypedArray())
                }

                functionGenerationContext.br(blocks[entry]!!)
            }
        }
    }
}
