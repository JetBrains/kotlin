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

internal fun IrSimpleFunction.shouldGenerateHair(): Boolean = name.toString().startsWith("toHair")

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

internal class HairGenerator(val context: Context, val module: IrModuleFragment) : BodyLoweringPass {
    val moduleCompilation = Compilation()

    val funCompilations = mutableMapOf<IrFunction, FunctionCompilation>()

    override fun lower(irBody: IrBody, container: IrDeclaration) {
        // TODO non-simple functions
        if (container is IrSimpleFunction && container.shouldGenerateHair()) {
            try {
                funCompilations[container] = generateHair(container)
            } catch (e: Throwable) {
                context.reportWarning("Failed to generate HaIR for ${container.name.toString()}: $e", container.fileOrNull, container)
            }
            context.log { "# Successfully generated HaIR for ${container.name.toString()}" }
        }
    }

    fun generateHair(f: IrSimpleFunction): FunctionCompilation {
        val hairFun = KNFunction(f)
        val funCompilation = FunctionCompilation(moduleCompilation, hairFun)
        context.log {"# Generating hair for ${f.name}, compilation = $funCompilation" }

        // TODO replace Var with wrapper aroung IRValueSymbol?
        // TODO parse directly into SSA ignoing Vars?
        val vars = mutableMapOf<IrValueSymbol, Var>()
        fun getVar(sym: IrValueSymbol): Var {
            return vars.getOrPut(sym) { Var(sym.owner) }
        }

        with (funCompilation.session) {
            buildInitialIR<Unit> {
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

                    override fun visitReturn(expression: IrReturn, data: Unit): Node {
                        return Return(expression.value.accept(this, Unit))
                    }

                    override fun visitConst(expression: IrConst, data: Unit): Node {
                        return when (expression.kind) {
                            IrConstKind.Int -> ConstInt(expression.value as Int)
                            else -> TODO(expression.render())
                        }
                    }

                    override fun visitCall(expression: IrCall, data: Unit): Node {
                        val args = expression.arguments.map { it?.accept(this, Unit) }
                        return when (tryGetIntrinsicType(expression)) {
                            IntrinsicType.PLUS -> Add(Type.Primitive.INT)(args[0]!!, args[1]!!)
                            // TODO non-static calls
                            else -> TODO(expression.render())
                                //StaticCall(callee.function)(callArgs = arrayOf(ConstInt(1), ConstInt(2), ConstInt(3)))
                        }
                    }

                    override fun visitWhen(expression: IrWhen, data: Unit): Node {
                        val (values, exits) = expression.branches.map {
                            if (it.isUnconditional()) {
                                val value = it.result.accept(this, Unit)
                                val exit = lastControl?.let { Goto() }
                                Pair(value, exit)
                            } else {
                                val cond = it.condition.accept(this, Unit)
                                val if_ = If(cond)

                                Block().also { if_.trueExit = it }
                                require(lastControl is Block)
                                val trueValue = it.result.accept(this, Unit)
                                val trueExit = lastControl?.let { Goto() }

                                Block().also { if_.falseExit = it }
                                require(lastControl is Block)

                                Pair(trueValue, trueExit)
                            }
                        }.unzip()

                        val result = if (exits.any { it != null }) {
                            val merge = Block()
                            for (exit in exits.filterNotNull()) {
                                exit.exit = merge
                            }
                            Phi(merge, *values.toTypedArray())
                        } else NoValue()
                        return result
                    }

                    val loopHeaders = mutableMapOf<IrLoop, Block>()
                    val loopBreaks = mutableMapOf<IrLoop, MutableList<Goto>>()

                    override fun visitWhileLoop(loop: IrWhileLoop, data: Unit): Node {
                        val goto = Goto()
                        val condBlock = Block()
                        loopHeaders[loop] = condBlock
                        goto.exit = condBlock
                        val cond = loop.condition.accept(this, Unit)
                        val if_ = If(cond)

                        Block().also { if_.trueExit = it }
                        loop.body?.accept(this, Unit)
                        val trueExit = Goto()
                        trueExit.exit = condBlock

                        val exitBlock = Block()
                        if_.falseExit = exitBlock
                        for (breakExit in loopBreaks[loop] ?: emptyList()) {
                            breakExit.exit = exitBlock
                        }

                        return unitConst
                    }

                    override fun visitDoWhileLoop(loop: IrDoWhileLoop, data: Unit): Node {
                        val goto = Goto()
                        val entryBlock = Block()
                        loopHeaders[loop] = entryBlock
                        goto.exit = entryBlock

                        loop.body?.accept(this, Unit)

                        val cond = loop.condition.accept(this, Unit)
                        val if_ = If(cond)

                        Block().also { if_.trueExit = it }
                        val trueExit = Goto()
                        trueExit.exit = entryBlock

                        val exitBlock = Block()
                        if_.falseExit = exitBlock
                        for (breakExit in loopBreaks[loop] ?: emptyList()) {
                            breakExit.exit = exitBlock
                        }

                        return unitConst
                    }

                    override fun visitBreak(jump: IrBreak, data: Unit): Node {
                        val goto = Goto()
                        loopBreaks.getOrPut(jump.loop) { mutableListOf() } += goto
                        Block()
                        return NoValue()
                    }

                    override fun visitContinue(jump: IrContinue, data: Unit): Node {
                        val goto = Goto()
                        val header = loopHeaders[jump.loop]!!
                        goto.exit = header
                        Block()
                        return NoValue()
                    }

                    val returns = mutableMapOf<IrReturnableBlockSymbol, MutableList<Pair<Goto, Node>>>()

                    override fun visitReturnableBlock(expression: IrReturnableBlock, data: Unit): Node {
                        returns[expression.symbol] = mutableListOf()
                        val mainResult = super.visitReturnableBlock(expression, data)
                        val mainExit = Goto()
                        val exitBlock = Block()
                        val joinedValues = mutableListOf<Node>()
                        for ((goto, value) in returns[expression.symbol]!! + listOf(Pair(mainExit, mainResult))) {
                            goto.exit = exitBlock
                            joinedValues += value
                        }
                        return Phi(exitBlock, *joinedValues.toTypedArray())
                    }
                }, Unit)
            }

            buildSSA()

            printGraphviz()
        }
        return funCompilation
    }
}
