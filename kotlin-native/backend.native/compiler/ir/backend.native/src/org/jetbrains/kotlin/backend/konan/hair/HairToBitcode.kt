/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.hair

import hair.compilation.FunctionCompilation
import hair.graph.*
import hair.ir.*
import hair.ir.nodes.*
import hair.ir.spine
import hair.transform.GCMResult
import hair.transform.withGCM
import llvm.LLVMValueRef
import org.jetbrains.kotlin.backend.konan.NativeGenerationState;
import org.jetbrains.kotlin.backend.konan.llvm.CodeContext
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.CodeGeneratorVisitor.FunctionScope
import org.jetbrains.kotlin.ir.declarations.IrFunction

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
                val blocks = topSort(cfg()).associateWith {
                    functionGenerationContext.basicBlock("block_${it.id}", null)
                }
                val nodeValues = mutableMapOf<Node, LLVMValueRef>()

                for ((block, llvmBlock) in blocks) {
                    functionGenerationContext.appendingTo(llvmBlock) {
                        val blockNodes = gcm.linearOrder(block)

                        fun genNode(node: Node) {
                            context.log { "  generating $node" }
                            val value = when (node) {
                                is BlockEntry -> null // blocks[node]!!
                                is Param ->
                                    functionGenerationContext.param(node.index)
                                is ConstI -> llvm.constInt32(node.value).llvm
                                is ConstL -> llvm.constInt64(node.value).llvm
                                is ConstF -> llvm.constFloat32(node.value).llvm
                                is ConstD -> llvm.constFloat64(node.value).llvm
                                is AddI -> functionGenerationContext.add(nodeValues[node.lhs]!!, nodeValues[node.rhs]!!)
                                // TODO other arithmetics
                                is Phi -> {
                                    val phi = functionGenerationContext.phi(llvm.int32Type, "phi_${node.id}") // FIXME type
                                    val incoming = node.inputs.map { (value, blockEnd) ->
                                        val inBlock = blocks[blockEnd.block]!!
                                        val inValue = nodeValues[value]!! // FIXME what if there is a loop?
                                        inBlock to inValue
                                    }
                                    addPhiIncoming(phi, *incoming.toTypedArray())
                                    phi
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

                functionGenerationContext.br(blocks[entry]!!)
            }
        }
    }
}
