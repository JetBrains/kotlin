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
import hair.sym.RuntimeInterface
import hair.transform.GCMResult
import hair.transform.withGCM
import llvm.*
import org.jetbrains.kotlin.backend.konan.NativeGenerationState;
import org.jetbrains.kotlin.backend.konan.llvm.CodeContext
import org.jetbrains.kotlin.backend.konan.llvm.CodeGenerator
import org.jetbrains.kotlin.backend.konan.llvm.CodeGeneratorVisitor.FunctionScope
import org.jetbrains.kotlin.backend.konan.llvm.ExceptionHandler
import org.jetbrains.kotlin.backend.konan.llvm.FunctionGenerationContext
import org.jetbrains.kotlin.backend.konan.llvm.Lifetime
import org.jetbrains.kotlin.backend.konan.llvm.computeFullName
import org.jetbrains.kotlin.backend.konan.llvm.theUnitInstanceRef
import org.jetbrains.kotlin.backend.konan.llvm.toLLVMType
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.isNothing

// TODO move to utils
context(gcm: GCMResult)
val gcm get() = gcm

val HairType.isIntegral get() = when (this) {
    HairType.INT,
    HairType.LONG -> true
    HairType.FLOAT,
    HairType.DOUBLE -> false
    else -> true // FIXME error("Should not reach here $this")
}

internal class HairToBitcode(
        val generationState: NativeGenerationState,
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
        HairType.REFERENCE -> llvm.pointerType
        HairType.EXCEPTION -> llvm.pointerType
    }

    /**
     * Emits LLVM instructions for each Hair [Node] in a function body.
     *
     * An instance is created per [generateFunctionBody] call and shared across all
     * blocks of that function. Each [visitXxx] method is called while the
     * [FunctionGenerationContext] builder is positioned at the owning block's LLVM basic
     * block (via [FunctionGenerationContext.appendingTo]).
     */
    private inner class NodeCodegen(
            val fgc: FunctionGenerationContext,
            val currentCodeContext: CodeContext,
            val declaration: IrFunction,
            val blocks: Map<BlockEntry, LLVMBasicBlockRef>,
            val nodeValues: MutableMap<Node, LLVMValueRef>,
            val blockExitBlocks: MutableMap<BlockExit, LLVMBasicBlockRef>,
            val deferredPhies: MutableList<Phi>,
    ) : NodeVisitor<LLVMValueRef?>() {

        /** Runs [block] with [fgc] as the implicit receiver, giving access to all LLVM builder helpers. */
        private fun <R> emit(block: FunctionGenerationContext.() -> R): R = fgc.block()

        /** Widens narrow integral types to i32 (Hair's canonical integer width). */
        fun adaptToHair(value: LLVMValueRef): LLVMValueRef = when (LLVMTypeOf(value)) {
            llvm.int1Type,
            llvm.int8Type,
            llvm.int16Type -> fgc.sext(value, llvm.int32Type)
            else -> value
        }

        /** Truncates from Hair's i32 back to a narrower [targetType] when needed. */
        fun adaptFromHair(value: LLVMValueRef, targetType: LLVMTypeRef): LLVMValueRef = when (targetType) {
            llvm.int1Type,
            llvm.int8Type,
            llvm.int16Type -> fgc.trunc(value, targetType)
            else -> value
        }

        /** Retrieves the already-emitted LLVM value for an operand node. */
        private fun Node.value(): LLVMValueRef =
                nodeValues[this] ?: error("No LLVM value generated for Hair node $this")

        override fun visitNode(node: Node): LLVMValueRef? =
                error("Unhandled Hair node in LLVM codegen: ${node::class.simpleName} — $node")

        /** NoValue is a structural placeholder; it carries no LLVM value. */
        override fun visitNoValue(node: NoValue): LLVMValueRef? = null

        // ---------------------------------------------------------------
        // Control-flow

        override fun visitBlockEntry(node: BlockEntry): LLVMValueRef? {
            // LLVM requires phis first; GCM may interleave other nodes, so defer wiring.
            for (phi in node.uses.filterIsInstance<Phi>()) {
                deferredPhies += phi
                nodeValues[phi] = fgc.phi(phi.type.asLLVMType(), "phi_${phi.id}")
            }
            return null
        }

        override fun visitGoto(node: Goto): LLVMValueRef? = emit {
            val bb = basicBlock("blockExit_${node.id}", null)
            br(bb)
            blockExitBlocks[node] = bb
            appendingTo(bb) {
                br(blocks[node.next]!!)
            }
            null
        }

        override fun visitIf(node: If): LLVMValueRef? = emit {
            if (node.trueExit.next == node.falseExit.next) {
                // Hack: LLVM does not allow a condBr whose two successors are the same block.
                val trueBB = basicBlock("trueExit_${node.id}", null)
                blockExitBlocks[node.trueExit] = trueBB
                val falseBB = basicBlock("falseExit_${node.id}", null)
                blockExitBlocks[node.falseExit] = falseBB
                condBr(adaptFromHair(node.cond.value(), llvm.int1Type), trueBB, falseBB)
                appendingTo(trueBB) { br(blocks[node.trueExit.next]!!) }
                appendingTo(falseBB) { br(blocks[node.falseExit.next]!!) }
            } else {
                val bb = basicBlock("blockEnd_${node.id}", null)
                br(bb)
                blockExitBlocks[node.trueExit] = bb
                blockExitBlocks[node.falseExit] = bb
                appendingTo(bb) {
                    condBr(
                            adaptFromHair(node.cond.value(), llvm.int1Type),
                            blocks[node.trueExit.next]!!,
                            blocks[node.falseExit.next]!!
                    )
                }
            }
            null
        }

        override fun visitIfProjection(node: IfProjection): LLVMValueRef? = null

        override fun visitReturn(node: Return): LLVMValueRef? {
            val result = adaptFromHair(node.result.value(), fgc.returnType!!)
            currentCodeContext.genReturn(declaration, result)
            return null
        }

        override fun visitUnreachable(node: Unreachable): LLVMValueRef? = emit { unreachable() }

        // ---------------------------------------------------------------
        // Values

        override fun visitParam(node: Param): LLVMValueRef = adaptToHair(fgc.param(node.index))

        override fun visitPhi(node: Phi): LLVMValueRef =
                nodeValues[node]!! // already allocated by visitBlockEntry

        override fun visitConstI(node: ConstI): LLVMValueRef = llvm.constInt32(node.value).llvm
        override fun visitConstL(node: ConstL): LLVMValueRef = llvm.constInt64(node.value).llvm
        override fun visitConstF(node: ConstF): LLVMValueRef = llvm.constFloat32(node.value).llvm
        override fun visitConstD(node: ConstD): LLVMValueRef = llvm.constFloat64(node.value).llvm
        override fun visitNull(node: Null): LLVMValueRef = llvm.kNull
        override fun visitUnitValue(node: UnitValue): LLVMValueRef = codegen.theUnitInstanceRef.llvm

        // ---------------------------------------------------------------
        // Arithmetic

        override fun visitAdd(node: Add): LLVMValueRef = emit {
            if (node.type.isIntegral) add(node.lhs.value(), node.rhs.value())
            else fadd(node.lhs.value(), node.rhs.value())
        }

        override fun visitSub(node: Sub): LLVMValueRef = emit {
            if (node.type.isIntegral) sub(node.lhs.value(), node.rhs.value())
            else fsub(node.lhs.value(), node.rhs.value())
        }

        override fun visitMul(node: Mul): LLVMValueRef = emit {
            // TODO use FGC helpers once mul/fmul are promoted
            if (node.type.isIntegral) LLVMBuildMul(builder, node.lhs.value(), node.rhs.value(), "")!!
            else LLVMBuildFMul(builder, node.lhs.value(), node.rhs.value(), "")!!
        }

        // TODO divs
        // TODO shifts

        override fun visitAnd(node: And): LLVMValueRef = emit { and(node.lhs.value(), node.rhs.value()) }
        override fun visitOr(node: Or): LLVMValueRef = emit { or(node.lhs.value(), node.rhs.value()) }
        override fun visitXor(node: Xor): LLVMValueRef = emit { xor(node.lhs.value(), node.rhs.value()) }

        override fun visitNot(node: Not): LLVMValueRef =
                adaptToHair(emit { not(adaptFromHair(node.operand.value(), llvm.int1Type)) })

        override fun visitCmp(node: Cmp): LLVMValueRef {
            val lhs = node.lhs.value()
            val rhs = node.rhs.value()
            return if (node.type.isIntegral) {
                adaptToHair(emit {
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
                })
            } else {
                emit {
                    when (node.op) {
                        CmpOp.EQ -> fcmpEq(lhs, rhs)
                        CmpOp.S_GT -> fcmpGt(lhs, rhs)
                        CmpOp.S_GE -> fcmpGe(lhs, rhs)
                        CmpOp.S_LT -> fcmpLt(lhs, rhs)
                        CmpOp.S_LE -> fcmpLe(lhs, rhs)
                        else -> error("Unsupported floating-point CmpOp: ${node.op}")
                    }
                }
            }
        }

        // ---------------------------------------------------------------
        // Casts

        override fun visitSignExtend(node: SignExtend): LLVMValueRef =
                emit { sext(node.operand.value(), node.targetType.asLLVMType()) }

        override fun visitZeroExtend(node: ZeroExtend): LLVMValueRef =
                emit { zext(node.operand.value(), node.targetType.asLLVMType()) }

        override fun visitTruncate(node: Truncate): LLVMValueRef =
                emit { trunc(node.operand.value(), node.targetType.asLLVMType()) }

        override fun visitReinterpret(node: Reinterpret): LLVMValueRef =
                emit { bitcast(node.targetType.asLLVMType(), node.operand.value()) }

        // ---------------------------------------------------------------
        // Calls

        override fun visitInvokeStatic(node: InvokeStatic): LLVMValueRef {
            val hairTarget = node.function
            val llvmTarget = when (hairTarget) {
                is HairFunctionImpl -> codegen.llvmFunction(hairTarget.irFunction)
                RuntimeInterface.isSubtype -> llvm.isSubtypeFunction
                else -> error("Unexpected function $hairTarget")
            }
            // FIXME derive param types from the Hair type system, not from IrFunction
            val llvmParamTypes = when (hairTarget) {
                is HairFunctionImpl -> hairTarget.irFunction.parameters.map { it.type.toLLVMType(llvm) }
                RuntimeInterface.isSubtype -> listOf(llvm.pointerType, llvm.pointerType)
                else -> error("Unexpected function $hairTarget")
            }
            // TODO there are more things to do around the function call (EH, thread-state, …)
            val args = node.callArgs.zip(llvmParamTypes).map { [arg, paramType] ->
                adaptFromHair(arg.value(), paramType)
            }
            val res = emit {
                call(
                        llvmCallable = llvmTarget,
                        args = args,
                        resultLifetime = Lifetime.GLOBAL,
                        exceptionHandler = ExceptionHandler.Caller, // FIXME proper exception handling
                )
            }
            // TODO what about Unit returns?
            return if ((hairTarget as? HairFunctionImpl)?.irFunction?.returnType?.isNothing() == true) {
                // FIXME try to avoid dead code as the result of HaIR
                emit { unreachable() }
                codegen.theUnitInstanceRef.llvm
            } else adaptToHair(res)
        }

        // ---------------------------------------------------------------
        // Memory / fields

        override fun visitLoadGlobal(node: LoadGlobal): LLVMValueRef {
            val irField = (node.field as HairGlobalImpl).irField
            // TODO require(irField.correspondingPropertySymbol?.owner?.isConst != true)
            return adaptToHair(fgc.loadIrField(irField, thisPtr = null, resultSlot = null))
        }

        override fun visitLoadField(node: LoadField): LLVMValueRef {
            val irField = (node.field as HairFieldImpl).irField
            // TODO require(irField.correspondingPropertySymbol?.owner?.isConst != true)
            // TODO return slot!!
            return adaptToHair(fgc.loadIrField(irField, node.obj.value(), resultSlot = null))
        }

        override fun visitStoreGlobal(node: StoreGlobal): LLVMValueRef? {
            val irField = (node.field as HairGlobalImpl).irField
            fgc.storeIrField(irField, thisPtr = null, adaptFromHair(node.value.value(), irField.type.toLLVMType(llvm)))
            return null
        }

        override fun visitStoreField(node: StoreField): LLVMValueRef? {
            val irField = (node.field as HairFieldImpl).irField
            // TODO special handling for field initialization
            fgc.storeIrField(irField, node.obj.value(), adaptFromHair(node.value.value(), irField.type.toLLVMType(llvm)))
            return null
        }

        // ---------------------------------------------------------------
        // Allocation

        override fun visitNew(node: New): LLVMValueRef {
            val irClass = (node.objectType as HairClassImpl).irClass
            val typeInfo = codegen.typeInfoForAllocation(irClass)
            return emit { call(llvm.allocInstanceFunction, listOf(typeInfo), Lifetime.GLOBAL) }
        }

        override fun visitNewArray(node: NewArray): LLVMValueRef {
            val irClass = (node.elementType as HairClassImpl).irClass
            return emit {
                allocArray(
                        irClass,
                        node.size.value(),
                        Lifetime.GLOBAL,
                        ExceptionHandler.Caller, // FIXME should not throw
                )
            }
        }

        // ---------------------------------------------------------------
        // Type-info / misc

        override fun visitTypeInfo(node: TypeInfo): LLVMValueRef = emit { loadTypeInfo(node.obj.value()) }

        override fun visitConstTypeInfo(node: ConstTypeInfo): LLVMValueRef =
                codegen.typeInfoValue((node.type as HairClassImpl).irClass)

        // ---------------------------------------------------------------
        // Static initializers — no codegen yet

        override fun visitGlobalInit(node: GlobalInit): LLVMValueRef? = null // TODO()
        override fun visitThreadLocalInit(node: ThreadLocalInit): LLVMValueRef? = null // TODO()
        override fun visitStandaloneThreadLocalInit(node: StandaloneThreadLocalInit): LLVMValueRef? = null // TODO()
    }

    fun generateFunctionBody(
            currentCodeContext: CodeContext,
            declaration: IrFunction,
            hairComp: FunctionCompilation
    ) {
        context.log { "# Generating llvm from HaIR for ${declaration.computeFullName()}" }
        val functionGenerationContext = (currentCodeContext.functionScope() as FunctionScope).functionGenerationContext
        val entryBlock = functionGenerationContext.currentBlock

        val session = hairComp.session
        with(session) {
            withGCM {
                hairComp.dumpHair("before_codegen")
                val blocks = topSort(cfg()).associateWith {
                    functionGenerationContext.basicBlock("block_${it.id}", null)
                }
                // FIXME codegen inserts additional blocks (e.g. around calls)
                //     so we can't rely on the blocks map to locate the llvm block of a random node
                val blockExitBlocks = mutableMapOf<BlockExit, LLVMBasicBlockRef>()
                val nodeValues = mutableMapOf<Node, LLVMValueRef>()
                val deferredPhies = mutableListOf<Phi>()

                val nodeCodegen = NodeCodegen(
                        fgc = functionGenerationContext,
                        currentCodeContext = currentCodeContext,
                        declaration = declaration,
                        blocks = blocks,
                        nodeValues = nodeValues,
                        blockExitBlocks = blockExitBlocks,
                        deferredPhies = deferredPhies,
                )

                for ([block, llvmBlock] in blocks) {
                    functionGenerationContext.appendingTo(llvmBlock) {
                        for (node in gcm.linearOrder(block)) {
                            val value = node.accept(nodeCodegen)
                            if (value != null) nodeValues[node] = value
                        }
                    }
                }

                for (phi in deferredPhies) {
                    val llvmPhi = nodeValues[phi]!!
                    val incoming = phi.inputs.map { [value, blockExit] ->
                        val inBlock = blockExitBlocks[blockExit] ?: error("No LLVM block for Hair block-exit $blockExit")
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
