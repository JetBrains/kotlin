/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.hair

import hair.compilation.FunctionCompilation
import hair.graph.dfs
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

internal class HairToBitcode(
        val generationState :NativeGenerationState,
        val codegen: CodeGenerator,
) {
    private val llvm = generationState.llvm

    fun generateFunctionBody(
            currentCodeContext: CodeContext,
            declaration: IrFunction,
            hairComp: FunctionCompilation
    ) {
        val functionGenerationContext = (currentCodeContext.functionScope() as FunctionScope).functionGenerationContext

        val session = hairComp.session
        with (session) {
            withGCM { gcm ->
                val nonControlIn = allNodes().filterNot { it is ControlFlow }.groupBy { gcm.block(it) }
                val blocks = dfs(cfg()).associateWith {
                    functionGenerationContext.basicBlock("block_${it.id}", null)
                }
                val nodeValues = mutableMapOf<Node, LLVMValueRef>()

                for ((block, llvmBlock) in blocks) {
                    functionGenerationContext.appendingTo(llvmBlock) {
                        // FIXME proper ordering!!
                        val floating = mutableSetOf<Node>()
                        fun visitFloating(node: Node) {
                            if (node in floating) return
                            for (arg in node.args.filterNotNull()) {
                                // FIXME pos
                                if (arg !is ControlFlow && gcm.pos(arg).block == block) {
                                    visitFloating(arg)
                                }
                            }
                            floating += node
                        }
                        for (node in nonControlIn[block] ?: emptyList()) {
                            visitFloating(node)
                        }

                        println("in block ${block.id} floating: $floating")

                        fun genNode(node: Node) {
                            val value = when (node) {
                                is Param ->
                                    functionGenerationContext.param(node.number)
                                is ConstInt -> llvm.constInt32(node.value).llvm
                                // TODO respect type
                                is Add -> functionGenerationContext.add(nodeValues[node.lhs]!!, nodeValues[node.rhs]!!)
                                is Phi -> {
                                    val phi = functionGenerationContext.phi(llvm.int32Type, "phi_${node.id}") // FIXME type
                                    val incoming = node.inputs.map { (value, blockEnd) ->
                                        val inBlock = blocks[blockEnd.block]!!
                                        val inValue = nodeValues[value]!! // FIXME breaks on loop ?
                                        inBlock to inValue
                                    }
                                    addPhiIncoming(phi, *incoming.toTypedArray())
                                    phi
                                }
                                is Return -> {
                                    currentCodeContext.genReturn(declaration, nodeValues[node.value]!!)
                                    null
                                }
                                is Goto -> {
                                    functionGenerationContext.br(blocks[node.exit as Block]!!) // FIXME as Block?
                                    null
                                }
                                is If -> {
                                    functionGenerationContext.condBr(
                                            nodeValues[node.condition]!!,
                                            node.trueExit?.let { blocks[it]!! },
                                            node.falseExit?.let { blocks[it]!! }
                                    )
                                }
                                else -> TODO(node.toString())
                            }
                            if (value != null) {
                                nodeValues[node] = value
                            }
                        }


                        for (node in floating) {
                            genNode(node)
                        }

                        for (node in block.spine) {
                            genNode(node)
                        }
                    }
                }

                functionGenerationContext.br(blocks[entryBlock]!!)
            }
        }
    }
}
