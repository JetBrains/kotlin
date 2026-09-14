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
import hair.transform.valueType
import hair.transform.withGCM
import hair.transform.withValueTypes
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
import org.jetbrains.kotlin.backend.konan.llvm.type
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.isNothing

// TODO move to utils
context(gcm: GCMResult)
val gcm get() = gcm

internal class HairToBitcode(
        val generationState: NativeGenerationState,
        val codegen: CodeGenerator,
) {
    private val llvm = generationState.llvm
    private val context = generationState.context

    fun HairType.asLLVMType() = when (this) {
        HairType.VOID -> llvm.voidType
        HairType.NOTHING -> llvm.voidType
        HairType.BOOLEAN -> llvm.int1Type
        HairType.BYTE -> llvm.int8Type
        HairType.SHORT -> llvm.int16Type
        HairType.INT -> llvm.int32Type
        HairType.LONG -> llvm.int64Type
        HairType.FLOAT -> llvm.floatType
        HairType.DOUBLE -> llvm.doubleType
        HairType.REFERENCE -> llvm.pointerType
        HairType.NATIVE_POINTER -> llvm.pointerType
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

        // TODO: copied from IntrinsicGenerator
        fun makeConstOfType(value: Int, targetType: LLVMTypeRef) = when (targetType) {
            llvm.int8Type -> llvm.int8(value.toByte())
            llvm.int16Type -> llvm.char16(value.toChar())
            llvm.int32Type -> llvm.int32(value)
            llvm.int64Type -> llvm.int64(value.toLong())
            llvm.floatType -> llvm.float32(value.toFloat())
            llvm.doubleType -> llvm.float64(value.toDouble())
            else -> error("Unexpected primitive type: $targetType")
        }

        /** Retrieves the already-emitted LLVM value for an operand node. */
        private fun Node.value(): LLVMValueRef =
                nodeValues[this] ?: error("No LLVM value generated for Hair node $this")

        override fun visitNode(node: Node): LLVMValueRef? =
                error("Unhandled Hair node in LLVM codegen: $node")

        /** NoValue is a structural placeholder; it carries no LLVM value. */
        override fun visitNoValue(node: NoValue): LLVMValueRef? = null

        // ---------------------------------------------------------------
        // Control-flow

        override fun visitBlockEntry(node: BlockEntry): LLVMValueRef? {
            // LLVM requires phis first; GCM may interleave other nodes, so defer wiring.
            for (phi in node.uses.filterIsInstance<Phi>()) {
                deferredPhies += phi
                nodeValues[phi] = fgc.phi(phi.valueType.asLLVMType(), "phi_${phi.id}")
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
                condBr(node.cond.value(), trueBB, falseBB)
                appendingTo(trueBB) { br(blocks[node.trueExit.next]!!) }
                appendingTo(falseBB) { br(blocks[node.falseExit.next]!!) }
            } else {
                val bb = basicBlock("blockEnd_${node.id}", null)
                br(bb)
                blockExitBlocks[node.trueExit] = bb
                blockExitBlocks[node.falseExit] = bb
                appendingTo(bb) {
                    condBr(
                            node.cond.value(),
                            blocks[node.trueExit.next]!!,
                            blocks[node.falseExit.next]!!
                    )
                }
            }
            null
        }

        override fun visitIfProjection(node: IfProjection): LLVMValueRef? = null

        override fun visitReturn(node: Return): LLVMValueRef? {
            val result = node.result.value()
            currentCodeContext.genReturn(declaration, result)
            return null
        }

        override fun visitUnreachable(node: Unreachable): LLVMValueRef? = emit { unreachable() }
        override fun visitHalt(node: Halt): LLVMValueRef? = emit { unreachable() }

        // ---------------------------------------------------------------
        // Values

        override fun visitParam(node: Param): LLVMValueRef = fgc.param(node.index)

        override fun visitPhi(node: Phi): LLVMValueRef =
                nodeValues[node]!! // already allocated by visitBlockEntry

        override fun visitConst(node: Const): LLVMValueRef = when (val value = node.value) {
            is Byte -> llvm.constInt8(value).llvm
            is Short -> llvm.constInt16(value).llvm
            is Int -> llvm.constInt32(value).llvm
            is Long -> llvm.constInt64(value).llvm
            is Float -> llvm.constFloat32(value).llvm
            is Double -> llvm.constFloat64(value).llvm
            else -> error("Unexpected constant value type: ${value}")
        }

        override fun visitConstBoolean(node: ConstBoolean): LLVMValueRef = llvm.constInt1(node.value).llvm
        override fun visitNull(node: Null): LLVMValueRef = llvm.kNull
        override fun visitUnitValue(node: UnitValue): LLVMValueRef = codegen.theUnitInstanceRef.llvm

        // ---------------------------------------------------------------
        // Arithmetic

        override fun visitAdd(node: Add): LLVMValueRef = emit {
            if (node.opType.isIntegral) add(node.lhs.value(), node.rhs.value())
            else fadd(node.lhs.value(), node.rhs.value())
        }

        override fun visitSub(node: Sub): LLVMValueRef = emit {
            if (node.opType.isIntegral) sub(node.lhs.value(), node.rhs.value())
            else fsub(node.lhs.value(), node.rhs.value())
        }

        override fun visitMul(node: Mul): LLVMValueRef = emit {
            // TODO use FGC helpers once mul/fmul are promoted
            if (node.opType.isIntegral) LLVMBuildMul(builder, node.lhs.value(), node.rhs.value(), "")!!
            else LLVMBuildFMul(builder, node.lhs.value(), node.rhs.value(), "")!!
        }

        override fun visitDiv(node: Div): LLVMValueRef = emit {
            if (node.opType.isIntegral) LLVMBuildSDiv(builder, node.lhs.value(), node.rhs.value(), "")!!
            else LLVMBuildFDiv(builder, node.lhs.value(), node.rhs.value(), "")!!
        }

        override fun visitRem(node: Rem): LLVMValueRef = emit {
            if (node.opType.isIntegral) LLVMBuildSRem(builder, node.lhs.value(), node.rhs.value(), "")!!
            else LLVMBuildFRem(builder, node.lhs.value(), node.rhs.value(), "")!!
        }

        override fun visitNeg(node: Neg): LLVMValueRef = emit { LLVMBuildNeg(builder, node.value(), "")!! }

        override fun visitAnd(node: And): LLVMValueRef = emit { and(node.lhs.value(), node.rhs.value()) }
        override fun visitOr(node: Or): LLVMValueRef = emit { or(node.lhs.value(), node.rhs.value()) }
        override fun visitXor(node: Xor): LLVMValueRef = emit { xor(node.lhs.value(), node.rhs.value()) }

        override fun visitShl(node: Shl): LLVMValueRef = emit { shift(LLVMOpcode.LLVMShl, node.lhs.value(), node.rhs.value()) }
        override fun visitShr(node: Shr): LLVMValueRef = emit { shift(LLVMOpcode.LLVMAShr, node.lhs.value(), node.rhs.value()) }
        override fun visitUshr(node: Ushr): LLVMValueRef = emit { shift(LLVMOpcode.LLVMLShr, node.lhs.value(), node.rhs.value()) }

        override fun visitInv(node: Inv): LLVMValueRef = emit { xor(node.value(), makeConstOfType(-1, node.value().type), "") }

        override fun visitNot(node: Not): LLVMValueRef =
                emit { not(node.operand.value()) }

        override fun visitCmp(node: Cmp): LLVMValueRef {
            val lhs = node.lhs.value()
            val rhs = node.rhs.value()
            return if (node.type.isIntegral) {
                emit {
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
            } else {
                emit {
                    when (node.op) {
                        CmpOp.EQ -> fcmpEq(lhs, rhs)
                        CmpOp.S_GT -> fcmpGt(lhs, rhs)
                        CmpOp.S_GE -> fcmpGe(lhs, rhs)
                        CmpOp.S_LT -> fcmpLt(lhs, rhs)
                        CmpOp.S_LE -> fcmpLe(lhs, rhs)
                        else -> error("Unexpected floating-point CmpOp: ${node.op}")
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
                RuntimeInterface.throwArrayIndexOutOfBounds ->
                    codegen.llvmFunction(context.symbols.throwArrayIndexOutOfBoundsException.owner)
                else -> error("Unexpected function $hairTarget")
            }
            // FIXME derive param types from the Hair type system, not from IrFunction
            val llvmParamTypes = when (hairTarget) {
                is HairFunctionImpl -> hairTarget.irFunction.parameters.map { it.type.toLLVMType(llvm) }
                RuntimeInterface.isSubtype -> listOf(llvm.pointerType, llvm.pointerType)
                RuntimeInterface.throwArrayIndexOutOfBounds -> emptyList()
                else -> error("Unexpected function $hairTarget")
            }
            // TODO there are more things to do around the function call (EH, thread-state, …)
            val args = node.callArgs.zip(llvmParamTypes).map { [arg, paramType] ->
                arg.value()
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
            } else res
        }

        // ---------------------------------------------------------------
        // Memory / fields

        override fun visitLoadGlobal(node: LoadGlobal): LLVMValueRef {
            val irField = (node.field as HairGlobalImpl).irField
            // TODO require(irField.correspondingPropertySymbol?.owner?.isConst != true)
            return fgc.loadIrField(irField, thisPtr = null, resultSlot = null)
        }

        override fun visitLoadField(node: LoadField): LLVMValueRef {
            val irField = (node.field as HairFieldImpl).irField
            // TODO require(irField.correspondingPropertySymbol?.owner?.isConst != true)
            // TODO return slot!!
            return fgc.loadIrField(irField, node.obj.value(), resultSlot = null)
        }

        override fun visitStoreGlobal(node: StoreGlobal): LLVMValueRef? {
            val irField = (node.field as HairGlobalImpl).irField
            irField.type.toLLVMType(llvm)
            fgc.storeIrField(irField, thisPtr = null, node.value.value())
            return null
        }

        override fun visitStoreField(node: StoreField): LLVMValueRef? {
            val irField = (node.field as HairFieldImpl).irField
            // TODO special handling for field initialization
            irField.type.toLLVMType(llvm)
            fgc.storeIrField(irField, node.obj.value(), node.value.value())
            return null
        }

        override fun visitArraySize(node: ArraySize): LLVMValueRef = emit {
            // TODO maybe move to lowering?
            val countPtr = structGep(runtime.arrayHeaderType, node.array.value(), 1, "count_")
            load(llvm.int32Type, countPtr)
        }

        private fun FunctionGenerationContext.arrayElementAddress(
                array: LLVMValueRef, index: LLVMValueRef, elementType: LLVMTypeRef
        ): LLVMValueRef {
            val arrayType = llvm.structType(runtime.arrayHeaderType, LLVMArrayType(elementType, 0)!!)
            val body = structGep(arrayType, array, 1, "arrayBody")
            return gep(elementType, body, index)
        }

        override fun visitLoadArrayElement(node: LoadArrayElement): LLVMValueRef = emit {
            val elementType = node.elementType.asLLVMType()
            val address = arrayElementAddress(node.array.value(), node.index.value(), elementType)
            loadSlot(
                    elementType,
                    isObjectType = node.elementType == HairType.REFERENCE,
                    address,
                    isVar = true,
                    resultSlot = null,
            )
        }

        override fun visitStoreArrayElement(node: StoreArrayElement): LLVMValueRef? = emit {
            val elementType = node.elementType.asLLVMType()
            val address = arrayElementAddress(node.array.value(), node.index.value(), elementType)
            storeAny(
                    node.value.value(),
                    address,
                    isObjectRef = node.elementType == HairType.REFERENCE,
                    onStack = false,
            )
            null
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
                // TODO move to global pipeline
                withValueTypes(hairComp) {
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
}
