/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(org.jetbrains.kotlin.config.MessageCollectorAccess::class)

package org.jetbrains.kotlin.backend.konan.hair

import org.jetbrains.kotlin.backend.konan.*
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.backend.common.phaser.*
import org.jetbrains.kotlin.backend.konan.driver.utilities.getDefaultIrActions
import org.jetbrains.kotlin.backend.common.*
import hair.compilation.*
import hair.ir.*
import hair.ir.nodes.*
import hair.opt.optimize
import hair.sym.CmpOp
import hair.sym.HairType
import hair.sym.asArithmeticType
import hair.utils.*
import hair.transform.*
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.konan.ir.isBuiltInOperator
import org.jetbrains.kotlin.backend.konan.ir.isComparisonFunction
import org.jetbrains.kotlin.backend.konan.ir.isTypedIntrinsic
import org.jetbrains.kotlin.backend.konan.ir.tryGetIntrinsicType
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.lower.StaticInitializersOrigins
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.config.messageCollector
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isChar
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isOverridable
import org.jetbrains.kotlin.ir.util.isReal
import org.jetbrains.kotlin.ir.util.render


internal fun IrSimpleFunction.shouldGenerateBody(): Boolean = modality != Modality.ABSTRACT && !isExternal

private fun IrCall.isVirtual(): Boolean = superQualifierSymbol?.owner == null && symbol.owner.isOverridable

internal val GenerateHairPhase = createSimpleNamedCompilerPhase<NativeGenerationState, IrModuleFragment, Map<IrFunction, FunctionCompilation>>(
        name = "GenerateHair",
        preactions = getDefaultIrActions(),
        postactions = getDefaultIrActions(),
        outputIfNotEnabled = { _, _, _, _ -> mapOf() },
        op = { generationState, module ->
            generateHair(generationState, module)
        }
)

internal fun generateHair(generationState: NativeGenerationState, irModule: IrModuleFragment): Map<IrFunction, FunctionCompilation> {
    val hairGenerator = HairGenerator(generationState.context, irModule)
    hairGenerator.lower(irModule)
    return hairGenerator.funCompilations.toMap()
}

// FIXME move to utils?
context(controlBuilder: ControlFlowBuilder)
val controlBuilder get() = controlBuilder

internal class HairGenerator(val context: Context, val module: IrModuleFragment) : BodyLoweringPass {
    val moduleCompilation = createHairCompilation(context, module)

    val funCompilations = mutableMapOf<IrFunction, FunctionCompilation>()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        if (context.config.enableHair) {
            if (container is IrSimpleFunction) {
                try {
                    funCompilations[container] = generateHair(container)
                    context.log { "# Successfully generated HaIR for ${container.computeFullName()}" }
                } catch (e: HairNotImplementedYet) {
                    context.configuration.messageCollector.report(CompilerMessageSeverity.WARNING, "Failed to generate HaIR for ${container.computeFullName()}: $e")
                } catch (e: Throwable) {
                    context.configuration.messageCollector.report(CompilerMessageSeverity.WARNING, "Failed to generate HaIR for ${container.computeFullName()}: $e\n${e.stackTraceToString()}")
                }
            } else {
                // TODO non-simple functions
            }
        }
    }

    fun generateHair(f: IrSimpleFunction): FunctionCompilation {
        val hairFun = HairFunctionImpl(f)
        val funCompilation = FunctionCompilation(moduleCompilation, hairFun)
        context.log { "# Generating hair for ${f.computeFullName()}, compilation = $funCompilation" }

        with (funCompilation.session) {
            buildInitialIR {
                // FIXME
                val unitConst by lazy { UnitValue() }

                for ([idx, param] in f.parameters.withIndex()) {
                    AssignVar(param.symbol)(Param(idx))
                }
                f.body?.accept(object : IrVisitor<Node, Unit>() {
                    override fun visitElement(element: IrElement, data: Unit): Node {
                        error("Should not reach here ${element.render()}")
                    }

                    override fun visitExpressionBody(body: IrExpressionBody, data: Unit): Node {
                        return body.expression.accept(this, Unit)
                    }

                    override fun visitBlockBody(body: IrBlockBody, data: Unit): Node {
                        return body.statements.fold(null) { acc, st ->
                            st.accept(this, Unit)
                        } ?: NoValue()
                    }

                    override fun visitContainerExpression(expression: IrContainerExpression, data: Unit): Node {
                        return expression.statements.fold(null) { acc, st ->
                            st.accept(this, Unit)
                        } ?: NoValue()
                    }

                    override fun visitSetValue(expression: IrSetValue, data: Unit): Node {
                        val value = expression.value.accept(this, Unit)
                        AssignVar(expression.symbol)(value)
                        return value // FIXME correct?
                    }

                    override fun visitGetValue(expression: IrGetValue, data: Unit): Node {
                        return ReadVar(expression.symbol)
                    }

                    override fun visitVariable(declaration: IrVariable, data: Unit): Node {
                        val variable = declaration.symbol
                        if (declaration.initializer != null) {
                            val value = declaration.initializer!!.accept(this, Unit)
                            AssignVar(variable)(value)
                        }
                        return NoValue()
                    }

                    override fun visitConst(expression: IrConst, data: Unit): Node {
                        return when (expression.kind) {
                            IrConstKind.Boolean -> if (expression.value as Boolean) True() else False()
                            IrConstKind.Byte -> ConstI((expression.value as Byte).toInt())
                            IrConstKind.Short -> ConstI((expression.value as Short).toInt())
                            IrConstKind.Char -> ConstI((expression.value as Char).code)
                            IrConstKind.Int -> ConstI(expression.value as Int)
                            IrConstKind.Long -> ConstL(expression.value as Long)
                            IrConstKind.Float -> ConstF(expression.value as Float)
                            IrConstKind.Double -> ConstD(expression.value as Double)
                            IrConstKind.Null -> Null()
                            IrConstKind.String -> notImplemented(HairTODO.STRING_LITERALS)
                        }
                    }

                    override fun visitCall(expression: IrCall, data: Unit): Node {
                        val resultType = expression.type.asHairType()
                        val args = expression.arguments.map { it?.accept(this, Unit) }

                        val function = expression.symbol.owner
                        require(!function.isSuspend) { "Suspend functions should be lowered out at this point"}

                        return when {
                            function.isTypedIntrinsic -> generateIntrinsic(expression, resultType, args)
                            function.isBuiltInOperator -> generateBuiltinOperator(expression, resultType, args)
                            function.origin == StaticInitializersOrigins.STATIC_GLOBAL_INITIALIZER -> GlobalInit()
                            function.origin == StaticInitializersOrigins.STATIC_THREAD_LOCAL_INITIALIZER -> ThreadLocalInit()
                            function.origin == StaticInitializersOrigins.STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER -> StandaloneThreadLocalInit()
                            !function.isReal -> notImplemented(HairTODO.FAKE_OVERRIDE_CALL)
                            else -> if (expression.isVirtual()) {
                                notImplemented(HairTODO.VIRTUAL_CALLS)
                            } else {
                                val call = InvokeStatic(HairFunctionImpl(function))(callArgs = args.toTypedArray())
                                // TODO insert Halt if function returns Notihing ?
                                if (function.returnType.isUnit()) UnitValue() else call
                            }
                        }
                    }

                    private fun generateIntrinsic(call: IrCall, resType: HairType, args: List<Node?>): Node = when (val iType = tryGetIntrinsicType(call)) {
                        IntrinsicType.PLUS -> Add(resType.asArithmeticType())(args[0]!!, args[1]!!)
                        IntrinsicType.MINUS -> Sub(resType.asArithmeticType())(args[0]!!, args[1]!!)
                        IntrinsicType.TIMES -> Mul(resType.asArithmeticType())(args[0]!!, args[1]!!)
                        // FIXME signed vs unsigned
                        // IntrinsicType.SIGNED_DIV -> Div(resType.asArithmeticType())(args[0]!!, args[1]!!)
                        // IntrinsicType.SIGNED_REM -> Rem(resType.asArithmeticType())(args[0]!!, args[1]!!)

                        IntrinsicType.AND -> And(resType.asArithmeticType())(args[0]!!, args[1]!!)
                        IntrinsicType.OR -> Or(resType.asArithmeticType())(args[0]!!, args[1]!!)
                        IntrinsicType.XOR -> Xor(resType.asArithmeticType())(args[0]!!, args[1]!!)
                        // IntrinsicType.SHL -> Shl(resType.asArithmeticType())(args[0]!!, args[1]!!)
                        // IntrinsicType.SHR -> Shr(resType.asArithmeticType())(args[0]!!, args[1]!!)
                        // IntrinsicType.USHR -> Ushr(resType.asArithmeticType())(args[0]!!, args[1]!!)

                        IntrinsicType.THE_UNIT_INSTANCE -> UnitValue()

                        IntrinsicType.CREATE_UNINITIALIZED_INSTANCE -> New(HairClassImpl(call.typeArguments[0]!!.getClass()!!))
                        IntrinsicType.CREATE_UNINITIALIZED_ARRAY -> NewArray(HairClassImpl(call.typeArguments[0]!!.getClass()!!))(args[0]!!)

                        IntrinsicType.SIGN_EXTEND -> SignExtend(resType)(args[0]!!)
                        IntrinsicType.ZERO_EXTEND -> ZeroExtend(resType)(args[0]!!)
                        IntrinsicType.INT_TRUNCATE -> Truncate(resType)(args[0]!!)
                        IntrinsicType.REINTERPRET -> Reinterpret(resType)(args[0]!!)

                        IntrinsicType.IDENTITY -> args[0]!!

                        IntrinsicType.INC -> Add(resType.asArithmeticType())(args[0]!!, Const(resType, 1))
                        IntrinsicType.DEC -> Sub(resType.asArithmeticType())(args[0]!!, Const(resType, 1))

                        IntrinsicType.ARE_EQUAL_BY_VALUE -> notImplemented(HairTODO.ARE_EQUAL_BY_VALUE)
                        IntrinsicType.FLOAT_TRUNCATE -> notImplemented(HairTODO.FLOAT_TRUNCATE)

                        else -> TODO("Intrinsic: $iType")
                    }

                    private fun generateBuiltinOperator(call: IrCall, resType: HairType, args: List<Node?>): Node {
                        val ib = context.irBuiltIns
                        val functionSymbol = call.symbol
                        val function = functionSymbol.owner
                        val argType = function.parameters[0].type.asHairType()
                        // Table mapping each comparison operator family to its signed and unsigned CmpOp.
                        // Floating-point comparisons also use S_xxx at the Hair level; HairToBitcode
                        // selects fcmp vs icmp based on the operand HairType, not the CmpOp.
                        val comparisonOpsByFamily = listOf(
                                ib.greaterFunByOperandType        to (CmpOp.S_GT to CmpOp.U_GT),
                                ib.greaterOrEqualFunByOperandType to (CmpOp.S_GE to CmpOp.U_GE),
                                ib.lessFunByOperandType           to (CmpOp.S_LT to CmpOp.U_LT),
                                ib.lessOrEqualFunByOperandType    to (CmpOp.S_LE to CmpOp.U_LE),
                        )
                        return when (functionSymbol) {
                            ib.eqeqeqSymbol -> Cmp(argType, CmpOp.EQ)(args[0]!!, args[1]!!)
                            ib.booleanNotSymbol -> Cmp(argType, CmpOp.NE)(args[0]!!, True())
                            else -> {
                                val shouldUseUnsignedComparison = functionSymbol.owner.parameters[0].type.isChar()
                                val ops = comparisonOpsByFamily
                                        .firstOrNull { functionSymbol.isComparisonFunction(it.first) }
                                        ?.second
                                        ?: when (functionSymbol) {
                                            context.irBuiltIns.illegalArgumentExceptionSymbol ->
                                                TODO("context.symbols.throwIllegalArgumentExceptionWithMessage")
                                            else -> TODO(functionSymbol.owner.name.toString())
                                        }
                                Cmp(argType, if (shouldUseUnsignedComparison) ops.second else ops.first)(args[0]!!, args[1]!!)
                            }
                        }
                    }

                    override fun visitWhen(expression: IrWhen, data: Unit): Node {
                        // FIXME reuse convenience builder somehow
                        var exhaustive = false
                        val pairs = expression.branches.map {
                            if (it.isUnconditional()) {
                                exhaustive = true
                                val value = it.result.accept(this, Unit)
                                // FIXME check for unreachable instead?
                                val exit = if (controlBuilder.lastControl != null) Goto() else null
                                value to exit
                            } else {
                                val [trueExit, falseExit] = IfExits(it.condition.accept(this, Unit))

                                BlockEntry(trueExit).ensuring { controlBuilder.lastControl == it }
                                val trueValue = it.result.accept(this, Unit)
                                val trueGoto = if (controlBuilder.lastControl != null) Goto() else null

                                BlockEntry(falseExit).ensuring { controlBuilder.lastControl == it }

                                trueValue to trueGoto
                            }
                        } + listOf(NoValue() to (if (exhaustive) null else Goto()))

                        val [values, exits] = pairs.filter { it.second != null }.unzip()

                        val result = if (exits.isNotEmpty()) {
                            require(exits.size == values.size)
                            val merge = BlockEntry(*exits.toTypedArray())
                            Phi( merge, *((exits.map { it!! }).zip(values)).toTypedArray())
                        } else NoValue()

                        return result
                    }

                    val loopBreaks = mutableMapOf<IrLoop, MutableList<BlockExit>>()
                    val loopContinues = mutableMapOf<IrLoop, MutableList<BlockExit>>()

                    override fun visitWhileLoop(loop: IrWhileLoop, data: Unit): Node {
                        val condBlock = BlockEntry(Goto(), null) as BlockEntry
                        val cond = loop.condition.accept(this, Unit)
                        val [trueExit, falseExit] = IfExits(cond)

                        BlockEntry(trueExit)
                        loop.body?.accept(this, Unit)
                        val trueGoto = Goto()
                        condBlock.preds[1] = trueGoto // FIXME sha t if no exit?

                        val breakExits = loopBreaks[loop] ?: mutableListOf()
                        BlockEntry(falseExit, *breakExits.toTypedArray())

                        val continueExits = loopContinues[loop] ?: mutableListOf()
                        if (continueExits.isNotEmpty()) {
                            val preds = condBlock.preds + continueExits
                            condBlock.replaceArgs(preds.toTypedArray())
                        }

                        return unitConst
                    }

                    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Unit): Node {
                        val goto = Goto()
                        val entryBlock = BlockEntry(goto, null) as BlockEntry

                        loop.body?.accept(this, Unit)

                        val cond = loop.condition.accept(this, Unit)
                        val [trueExit, falseExit] = IfExits(cond)

                        BlockEntry(trueExit)
                        entryBlock.preds[1] = Goto() // FIXME what if no exit

                        val breakExits = loopBreaks[loop] ?: mutableListOf()
                        val exitBlock = BlockEntry(falseExit, *breakExits.toTypedArray())

                        val continueExits = loopContinues[loop] ?: mutableListOf()
                        if (continueExits.isNotEmpty()) {
                            val preds = entryBlock.preds + continueExits
                            entryBlock.replaceArgs(preds.toTypedArray())
                        }

                        return unitConst
                    }

                    override fun visitBreak(jump: IrBreak, data: Unit): Node {
                        val goto = Goto()
                        loopBreaks.getOrPut(jump.loop) { mutableListOf() } += goto
                        unreachable()
                        return NoValue()
                    }

                    override fun visitContinue(jump: IrContinue, data: Unit): Node {
                        val goto = Goto()
                        loopContinues.getOrPut(jump.loop) { mutableListOf() } += goto
                        unreachable()
                        return NoValue()
                    }

                    val returns = mutableMapOf<IrReturnableBlockSymbol, MutableList<Pair<BlockExit, Node>>>()

                    override fun visitReturn(expression: IrReturn, data: Unit): Node {
                        val value = expression.value.accept(this, Unit)
                        val target = expression.returnTargetSymbol
                        if (target is IrReturnableBlockSymbol) {
                            val goto = Goto()
                            returns.getOrPut(target) { mutableListOf() } += goto to value
                        } else {
                            // FIXME what if return Unit?
                            Return(value)
                        }
                        unreachable()
                        return NoValue()
                    }

                    override fun visitReturnableBlock(expression: IrReturnableBlock, data: Unit): Node {
                        returns[expression.symbol] = mutableListOf()
                        val mainResult = super.visitReturnableBlock(expression, data)
                        val mainExit = if (controlBuilder.lastControl !is Unreachable) Goto() else null
                        @Suppress("UNCHECKED_CAST")
                        val results = (returns[expression.symbol]!! + listOf(mainExit to mainResult)).filter { it.first != null } as List<Pair<BlockExit, Node>>
                        val exitBlock = BlockEntry(*results.map { it.first }.toTypedArray())
                        return Phi(exitBlock, *results.toTypedArray())
                    }

                    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Unit): Node {
                        val cls = expression.typeOperand.type.getClass()?.let { HairClassImpl(it) }
                        val arg = expression.argument.accept(this, Unit)
                        return when (expression.operator) {
                            IrTypeOperator.CAST -> {
                                CheckCast(cls!!)(arg)
                            }
                            IrTypeOperator.IMPLICIT_CAST -> {
                                // FIXME should we generate it?
                                arg
                            }
//                            IrTypeOperator.IMPLICIT_NOTNULL -> TODO()
                            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                                UnitValue()
                            }
//                            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> TODO()

                            // TODO extract null checks
                            // TODO drop up-casts (in normalization?) (requires type system interface)
                            IrTypeOperator.INSTANCEOF -> IsInstanceOf(cls!!)(arg)
                            IrTypeOperator.NOT_INSTANCEOF -> Not(IsInstanceOf(cls!!)(arg))

                            IrTypeOperator.SAFE_CAST -> error("Should have beed lowered ${expression.operator}")

                            IrTypeOperator.SAM_CONVERSION -> error("Should not be here ${expression.operator}")
                            IrTypeOperator.IMPLICIT_DYNAMIC_CAST -> error("Should not be here ${expression.operator}")
                            IrTypeOperator.REINTERPRET_CAST -> error("Should not be here ${expression.operator}")

                            else -> TODO(expression.operator.toString())
                        }
                    }

                    override fun visitGetField(expression: IrGetField, data: Unit): Node {
                        val field = expression.symbol.owner
                        if (field.hasAnnotation(KonanFqNames.volatile)) notImplemented(HairTODO.VOLATILE)
                        return if (field.isStatic) {
                            // FIXME global vs field?
                            LoadGlobal(HairGlobalImpl(field))
                        } else {
                            val obj = expression.receiver!!.accept(this, Unit)
                            LoadField(HairFieldImpl(field))(obj)
                        }
                    }

                    override fun visitSetField(expression: IrSetField, data: Unit): Node {
                        val field = expression.symbol.owner
                        if (field.hasAnnotation(KonanFqNames.volatile)) notImplemented(HairTODO.VOLATILE)
                        val value = expression.value.accept(this, Unit)
                        return if (field.isStatic) {
                            // FIXME global vs field?
                            StoreGlobal(HairGlobalImpl(field))(value)
                        } else {
                            val obj = expression.receiver!!.accept(this, Unit)
                            StoreField(HairFieldImpl(field))(obj, value)
                        }
                    }

                    override fun visitTry(aTry: IrTry, data: Unit): Node {
                        notImplemented(HairTODO.EXCEPTIONS)
                    }

                    override fun visitThrow(expression: IrThrow, data: Unit): Node {
                        notImplemented(HairTODO.EXCEPTIONS)
                    }
                }, Unit)
            }

            funCompilation.dumpHair("initial_ir")

            buildSSA()
            funCompilation.dumpHair("initial_ir_after_SSA")

            optimize()
            // TODO log IR in optimize after each iteration
            funCompilation.dumpHair("after_optimize")

            lower()
            funCompilation.dumpHair("after_lowering")
        }

        return funCompilation
    }
}
