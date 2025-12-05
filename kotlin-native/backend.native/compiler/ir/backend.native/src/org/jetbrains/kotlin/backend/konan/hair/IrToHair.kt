/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

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
import hair.sym.ArithmeticType.*
import hair.sym.CmpOp
import hair.sym.HairType
import hair.utils.*
import hair.transform.*
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.backend.konan.ir.isBuiltInOperator
import org.jetbrains.kotlin.backend.konan.ir.isComparisonFunction
import org.jetbrains.kotlin.backend.konan.ir.isTypedIntrinsic
import org.jetbrains.kotlin.backend.konan.ir.tryGetIntrinsicType
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_STATIC_GLOBAL_INITIALIZER
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER
import org.jetbrains.kotlin.backend.konan.lower.DECLARATION_ORIGIN_STATIC_THREAD_LOCAL_INITIALIZER
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.isChar
import org.jetbrains.kotlin.ir.types.isDoubleOrFloatWithoutNullability
import org.jetbrains.kotlin.ir.types.isUnit
import org.jetbrains.kotlin.ir.util.fileOrNull
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
    val moduleCompilation = Compilation()

    val funCompilations = mutableMapOf<IrFunction, FunctionCompilation>()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        // TODO non-simple functions
        if (context.config.enableHair && container is IrSimpleFunction) {
            try {
                funCompilations[container] = generateHair(container)
                context.log { "# Successfully generated HaIR for ${container.computeFullName()}" }
            } catch (e: Throwable) {
                println("# Failed with $e")
                context.reportWarning("Failed to generate HaIR for ${container.computeFullName()}: $e\n${e.stackTraceToString()}", container.fileOrNull, container)
            }
        }
    }

    fun generateHair(f: IrSimpleFunction): FunctionCompilation {
        val hairFun = HairFunctionImpl(f)
        val funCompilation = FunctionCompilation(moduleCompilation, hairFun)
        context.log {"# Generating hair for ${f.computeFullName()}, compilation = $funCompilation" }
        println("# Generating hair for ${f.computeFullName()}")

        // TODO parse directly into SSA ignoing Vars?
        val vars = mutableMapOf<IrValueSymbol, IrValueSymbol>()
        fun getVar(sym: IrValueSymbol) = sym

        with (funCompilation.session) {
            buildInitialIR {
                fun Const(type: HairType, value: Number) = when (type) {
                    HairType.INT -> ConstI(value as Int)
                    HairType.LONG -> ConstL(value as Long)
                    HairType.FLOAT -> ConstF(value as Float)
                    HairType.DOUBLE -> ConstD(value as Double)
                    else -> error("Should not reach here $type")
                }

                // FIXME
                val unitConst by lazy { UnitValue() }

                for ((idx, param) in f.parameters.withIndex()) {
                    val v = getVar(param.symbol)
                    val node = Param(idx)
                    AssignVar(v)(node)
                }
                f.body?.accept(object : IrVisitor<Node, Unit>() {
                    override fun visitElement(element: IrElement, data: Unit): Node {
                        return error("Should not reach here $element")
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
                        AssignVar(getVar(expression.symbol))(value)
                        return value // FIXME correct?
                    }

                    override fun visitGetValue(expression: IrGetValue, data: Unit): Node {
                        return ReadVar(getVar(expression.symbol))
                    }

                    override fun visitVariable(declaration: IrVariable, data: Unit): Node {
                        val variable = getVar(declaration.symbol)
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
                            IrConstKind.Char -> ConstI((expression.value as Byte).toInt())
                            IrConstKind.Int -> ConstI(expression.value as Int)
                            IrConstKind.Long -> ConstL(expression.value as Long)
                            IrConstKind.Float -> ConstF(expression.value as Float)
                            IrConstKind.Double -> ConstD(expression.value as Double)
                            IrConstKind.Null -> Null()
                            IrConstKind.String -> TODO("String literals")
                        }
                    }

                    override fun visitCall(expression: IrCall, data: Unit): Node {
                        println("generating ${expression.render()} nodes: ${allNodes().toList()}")
                        val resultType = expression.type.asHairType()
                        val args = expression.arguments.map { it?.accept(this, Unit) }

                        val function = expression.symbol.owner
                        require(!function.isSuspend) { "Suspend functions should be lowered out at this point"}

                        return when {
                            function.isTypedIntrinsic -> generateIntrinsic(expression, resultType, args)
                            function.isBuiltInOperator -> generateBuiltinOperator(expression, resultType, args)
                            function.origin == DECLARATION_ORIGIN_STATIC_GLOBAL_INITIALIZER -> GlobalInit()
                            function.origin == DECLARATION_ORIGIN_STATIC_THREAD_LOCAL_INITIALIZER -> ThreadLocalInit()
                            function.origin == DECLARATION_ORIGIN_STATIC_STANDALONE_THREAD_LOCAL_INITIALIZER -> StandaloneThreadLocalInit()
                            !function.isReal -> TODO(expression.render())
                            else -> if (expression.isVirtual()) TODO("InvokeVirtual ${expression.render()}") else {
                                val call = InvokeStatic(HairFunctionImpl(function))(callArgs = args.toTypedArray())
                                // TODO insert Halt if function returns Notihing ?
                                if (function.returnType.isUnit()) UnitValue() else call
                            }
                        }
                    }

                    private fun generateIntrinsic(call: IrCall, resType: HairType, args: List<Node?>): Node = when (val iType = tryGetIntrinsicType(call)) {
                        IntrinsicType.PLUS -> Add(resType)(args[0]!!, args[1]!!)
                        IntrinsicType.MINUS -> Sub(resType)(args[0]!!, args[1]!!)
                        IntrinsicType.TIMES -> Mul(resType)(args[0]!!, args[1]!!)
                        // FIXME signed vs unsigned
                        // IntrinsicType.SIGNED_DIV -> Div(resType)(args[0]!!, args[1]!!)
                        // IntrinsicType.SIGNED_REM -> Rem(resType)(args[0]!!, args[1]!!)

                        IntrinsicType.AND -> And(resType)(args[0]!!, args[1]!!)
                        IntrinsicType.OR -> Or(resType)(args[0]!!, args[1]!!)
                        IntrinsicType.XOR -> Xor(resType)(args[0]!!, args[1]!!)
                        // IntrinsicType.SHL -> Shl(resType)(args[0]!!, args[1]!!)
                        // IntrinsicType.SHR -> Shr(resType)(args[0]!!, args[1]!!)
                        // IntrinsicType.USHR -> Ushr(resType)(args[0]!!, args[1]!!)

                        IntrinsicType.THE_UNIT_INSTANCE -> UnitValue()

                        IntrinsicType.CREATE_UNINITIALIZED_INSTANCE -> New(HairClassImpl(call.typeArguments[0]!!.getClass()!!))
                        IntrinsicType.CREATE_UNINITIALIZED_ARRAY -> NewArray(HairClassImpl(call.typeArguments[0]!!.getClass()!!))(args[0]!!)

                        IntrinsicType.SIGN_EXTEND -> SignExtend(resType)(args[0]!!)
                        IntrinsicType.ZERO_EXTEND -> ZeroExtend(resType)(args[0]!!)
                        IntrinsicType.INT_TRUNCATE -> Truncate(resType)(args[0]!!)
                        IntrinsicType.REINTERPRET -> Reinterpret(resType)(args[0]!!)

                        IntrinsicType.IDENTITY -> args[0]!!

                        IntrinsicType.INC -> Add(resType)(args[0]!!, Const(resType, 1))
                        IntrinsicType.DEC -> Sub(resType)(args[0]!!, Const(resType, 1))

                        else -> TODO("Intrinsic: $iType")
                    }

                    private fun generateBuiltinOperator(call: IrCall, resType: HairType, args: List<Node?>): Node {
                        val ib = context.irBuiltIns
                        val functionSymbol = call.symbol
                        val function = functionSymbol.owner
                        val argType = function.parameters[0].type.asHairType()
                        return when (functionSymbol) {
                            ib.eqeqeqSymbol -> {
                                Cmp(argType, CmpOp.EQ)(args[0]!!, args[1]!!)
                            }
                            ib.booleanNotSymbol -> Cmp(argType, CmpOp.NE)(args[0]!!, True())
                            else -> {
                                val isFloatingPoint = call.arguments[0]!!.type.isDoubleOrFloatWithoutNullability() // FIXME is this correct?
                                val shouldUseUnsignedComparison = functionSymbol.owner.parameters[0].type.isChar()
                                val op = when {
                                    functionSymbol.isComparisonFunction(ib.greaterFunByOperandType) -> {
                                        when {
                                            isFloatingPoint -> CmpOp.S_GT
                                            shouldUseUnsignedComparison -> CmpOp.U_GT
                                            else -> CmpOp.S_GT
                                        }
                                    }
                                    functionSymbol.isComparisonFunction(ib.greaterOrEqualFunByOperandType) -> {
                                        when {
                                            isFloatingPoint -> CmpOp.S_GE
                                            shouldUseUnsignedComparison -> CmpOp.U_GE
                                            else -> CmpOp.S_GE
                                        }
                                    }
                                    functionSymbol.isComparisonFunction(ib.lessFunByOperandType) -> {
                                        when {
                                            isFloatingPoint -> CmpOp.S_LT
                                            shouldUseUnsignedComparison -> CmpOp.U_LT
                                            else -> CmpOp.S_LT
                                        }
                                    }
                                    functionSymbol.isComparisonFunction(ib.lessOrEqualFunByOperandType) -> {
                                        when {
                                            isFloatingPoint -> CmpOp.S_LE
                                            shouldUseUnsignedComparison -> CmpOp.U_LE
                                            else -> CmpOp.S_LE
                                        }
                                    }
                                    functionSymbol == context.irBuiltIns.illegalArgumentExceptionSymbol -> {
                                        TODO("context.symbols.throwIllegalArgumentExceptionWithMessage")
                                    }
                                    else -> TODO(functionSymbol.owner.name.toString())
                                }
                                Cmp(argType, op)(args[0]!!, args[1]!!)
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
                                val (trueExit, falseExit) = IfExits(it.condition.accept(this, Unit))

                                BlockEntry(trueExit).ensuring { controlBuilder.lastControl == it }
                                val trueValue = it.result.accept(this, Unit)
                                val trueGoto = if (controlBuilder.lastControl != null) Goto() else null

                                BlockEntry(falseExit).ensuring { controlBuilder.lastControl == it }

                                trueValue to trueGoto
                            }
                        } + listOf(NoValue() to (if (exhaustive) null else Goto()))

                        val (values, exits) = pairs.filter { it.second != null }.unzip()

                        val result = if (exits.isNotEmpty()) {
                            require(exits.size == values.size)
                            val merge = BlockEntry(*exits.toTypedArray()) as BlockEntry
                            Phi(expression.type.asHairType())(merge, *values.toTypedArray())
                        } else NoValue()

                        return result
                    }

                    val loopBreaks = mutableMapOf<IrLoop, MutableList<BlockExit>>()
                    val loopContinues = mutableMapOf<IrLoop, MutableList<BlockExit>>()

                    override fun visitWhileLoop(loop: IrWhileLoop, data: Unit): Node {
                        val condBlock = BlockEntry(Goto(), null) as BlockEntry
                        val cond = loop.condition.accept(this, Unit)
                        val (trueExit, falseExit) = IfExits(cond)

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
                        val (trueExit, falseExit) = IfExits(cond)

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
                        Unreachable()
                        return NoValue()
                    }

                    override fun visitContinue(jump: IrContinue, data: Unit): Node {
                        val goto = Goto()
                        loopContinues.getOrPut(jump.loop) { mutableListOf() } += goto
                        Unreachable()
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
                        Unreachable()
                        return NoValue()
                    }

                    override fun visitReturnableBlock(expression: IrReturnableBlock, data: Unit): Node {
                        returns[expression.symbol] = mutableListOf()
                        val mainResult = super.visitReturnableBlock(expression, data)
                        val mainExit = if (controlBuilder.lastControl !is Unreachable) Goto() else null
                        val (exits, results) = (returns[expression.symbol]!! + listOf(mainExit to mainResult)).filter { it.first != null }.unzip()
                        require(exits.all { it != null })
                        val exitBlock = BlockEntry(*exits.toTypedArray()) as BlockEntry
                        return Phi(expression.type.asHairType())(exitBlock, *results.toTypedArray())
                    }

                    override fun visitTypeOperator(expression: IrTypeOperatorCall, data: Unit): Node {
                        val cls = expression.typeOperand.type.getClass()?.let { HairClassImpl(it) }
                        val arg = expression.argument.accept(this, Unit)
                        return when (expression.operator) {
                            IrTypeOperator.CAST -> {
                                CheckCast(cls!!)(arg)
                                arg
                            }
                            IrTypeOperator.IMPLICIT_CAST -> {
                                // FIXME shouuld we generate it?
                                CheckCast(cls!!)(arg)
                                arg
                            }
                            IrTypeOperator.IMPLICIT_NOTNULL -> TODO()
                            IrTypeOperator.IMPLICIT_COERCION_TO_UNIT -> {
                                arg
                                UnitValue()
                            }
                            IrTypeOperator.IMPLICIT_INTEGER_COERCION -> TODO()
                            IrTypeOperator.INSTANCEOF -> IsInstanceOf(cls!!)(arg)
                            IrTypeOperator.NOT_INSTANCEOF -> Not(IsInstanceOf(cls!!)(arg))

                            IrTypeOperator.SAFE_CAST -> error("Should have beed lowered ${expression.operator}")

                            IrTypeOperator.SAM_CONVERSION -> error("Should not be here ${expression.operator}")
                            IrTypeOperator.IMPLICIT_DYNAMIC_CAST -> error("Should not be here ${expression.operator}")
                            IrTypeOperator.REINTERPRET_CAST -> error("Should not be here ${expression.operator}")
                        }
                    }

                    override fun visitGetField(expression: IrGetField, data: Unit): Node {
                        val field = expression.symbol.owner
                        if (field.hasAnnotation(KonanFqNames.volatile)) TODO("Volatile field access")
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
                        if (field.hasAnnotation(KonanFqNames.volatile)) TODO("Volatile field access")
                        val value = expression.value.accept(this, Unit)
                        return if (field.isStatic) {
                            // FIXME global vs field?
                            StoreGlobal(HairGlobalImpl(field))(value)
                        } else {
                            val obj = expression.receiver!!.accept(this, Unit)
                            StoreField(HairFieldImpl(field))(obj, value)
                        }
                    }
                }, Unit)
            }

            println("HaIR of ${f.name} before SSA:")
            printGraphvizNoGCM()

            buildSSA {
                it as IrValueSymbol
                it.owner.type.asHairType()
            }

            println("HaIR of ${f.name} after SSA:")
            printGraphviz()
        }
        return funCompilation
    }
}
