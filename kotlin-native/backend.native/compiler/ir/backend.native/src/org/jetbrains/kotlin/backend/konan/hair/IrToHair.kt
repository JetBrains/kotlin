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
import hair.sym.*
import hair.ir.*
import hair.ir.nodes.*
import hair.utils.*
import hair.transform.*
import org.jetbrains.kotlin.backend.common.ir.isUnconditional
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.backend.konan.llvm.*
import org.jetbrains.kotlin.ir.symbols.IrReturnableBlockSymbol
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.util.fileOrNull
import org.jetbrains.kotlin.ir.util.render


internal fun IrSimpleFunction.shouldGenerateBody(): Boolean = modality != Modality.ABSTRACT && !isExternal

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

class KNFunction(val irFunction: IrSimpleFunction) : HairFunction {
    override fun toString() = irFunction.name.toString()
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
                context.log { "# Successfully generated HaIR for ${container.name.toString()}" }
            } catch (e: Throwable) {
                context.reportWarning("Failed to generate HaIR for ${container.name.toString()}: $e", container.fileOrNull, container)
            }
        }
    }

    fun generateHair(f: IrSimpleFunction): FunctionCompilation {
        val hairFun = KNFunction(f)
        val funCompilation = FunctionCompilation(moduleCompilation, hairFun)
        context.log {"# Generating hair for ${f.name}, compilation = $funCompilation" }

        // TODO parse directly into SSA ignoing Vars?
        val vars = mutableMapOf<IrValueSymbol, IrValueSymbol>()
        fun getVar(sym: IrValueSymbol) = sym

        with (funCompilation.session) {
            buildInitialIR {
                // FIXME
                val unitConst by lazy { NoValue() }

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
                        }!!
                    }

                    override fun visitContainerExpression(expression: IrContainerExpression, data: Unit): Node {
                        return expression.statements.fold(null) { acc, st ->
                            st.accept(this, Unit)
                        }!!
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
                            IrConstKind.Int -> ConstI(expression.value as Int)
                            else -> TODO(expression.render())
                        }
                    }

                    override fun visitCall(expression: IrCall, data: Unit): Node {
                        val args = expression.arguments.map { it?.accept(this, Unit) }
                        return when (tryGetIntrinsicType(expression)) {
                            IntrinsicType.PLUS -> AddI(args[0]!!, args[1]!!)
                            // TODO non-static calls
                            else -> TODO(expression.render())
                                //StaticCall(callee.function)(callArgs = arrayOf(ConstInt(1), ConstInt(2), ConstInt(3)))
                        }
                    }

                    override fun visitWhen(expression: IrWhen, data: Unit): Node {
                        // FIXME reuse convenience builder somehow
                        val (values, exits) = expression.branches.map {
                            if (it.isUnconditional()) {
                                val value = it.result.accept(this, Unit)
                                val exit = if (controlBuilder.lastControl != null) Goto() else null
                                value to exit
                            } else {
                                val if_ = If(it.condition.accept(this, Unit))

                                BlockEntry(if_.trueExit).ensuring { controlBuilder.lastControl == it }
                                val trueValue = it.result.accept(this, Unit)
                                val trueExit = if (controlBuilder.lastControl != null) Goto() else null

                                BlockEntry(if_.falseExit).ensuring { controlBuilder.lastControl == it }

                                trueValue to trueExit
                            }
                        }.unzip()

                        val result = if (exits.any { it != null }) {
                            val merge = BlockEntry(*exits.filterNotNull().toTypedArray())
                            Phi(merge, *values.toTypedArray())
                        } else NoValue()

                        return result
                    }

                    val loopBreaks = mutableMapOf<IrLoop, MutableList<Goto>>()
                    val loopContinues = mutableMapOf<IrLoop, MutableList<Goto>>()

                    override fun visitWhileLoop(loop: IrWhileLoop, data: Unit): Node {
                        val condBlock = BlockEntry(Goto(), null)
                        val cond = loop.condition.accept(this, Unit)
                        val if_ = If(cond)

                        BlockEntry(if_.trueExit)
                        loop.body?.accept(this, Unit)
                        val trueExit = Goto()
                        condBlock.preds[1] = trueExit

                        val breakExits = loopBreaks[loop] ?: mutableListOf()
                        BlockEntry(if_.falseExit, *breakExits.toTypedArray())

                        val continueExits = loopContinues[loop] ?: mutableListOf()
                        if (continueExits.isNotEmpty()) {
                            val preds = condBlock.preds + continueExits
                            condBlock.replaceArgs(preds.toTypedArray())
                        }

                        return unitConst
                    }

                    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Unit): Node {
                        val goto = Goto()
                        val entryBlock = BlockEntry(goto, null)

                        loop.body?.accept(this, Unit)

                        val cond = loop.condition.accept(this, Unit)
                        val if_ = If(cond)

                        BlockEntry(if_.trueExit)
                        val trueExit = Goto()
                        entryBlock.preds[1] = trueExit

                        val breakExits = loopBreaks[loop] ?: mutableListOf()
                        val exitBlock = BlockEntry(if_.falseExit, *breakExits.toTypedArray())

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
                        // TODO why? BlockEntry()
                        return NoValue()
                    }

                    override fun visitContinue(jump: IrContinue, data: Unit): Node {
                        val goto = Goto()
                        loopContinues.getOrPut(jump.loop) { mutableListOf() } += goto
                        // TODO why? BlockEntry()
                        return NoValue()
                    }

                    val returns = mutableMapOf<IrReturnableBlockSymbol, MutableList<Pair<Goto, Node>>>()

                    override fun visitReturn(expression: IrReturn, data: Unit): Node {
                        val value = expression.value.accept(this, Unit)
                        val target = expression.returnTargetSymbol
                        if (target is IrReturnableBlockSymbol) {
                            val goto = Goto()
                            returns.getOrPut(target) { mutableListOf() } += goto to value
                            return goto
                        }
                        // FIXME what if return Unit?
                        return Return(value)
                    }

                    override fun visitReturnableBlock(expression: IrReturnableBlock, data: Unit): Node {
                        returns[expression.symbol] = mutableListOf()
                        val mainResult = super.visitReturnableBlock(expression, data)
                        val mainExit = Goto()
                        val (exits, results) = (returns[expression.symbol]!! + listOf(mainExit to mainResult)).unzip()
                        val exitBlock = BlockEntry(mainExit, *exits.toTypedArray())
                        return Phi(exitBlock, *results.toTypedArray())
                    }
                }, Unit)
            }

            buildSSA()

            printGraphviz()
        }
        return funCompilation
    }
}
