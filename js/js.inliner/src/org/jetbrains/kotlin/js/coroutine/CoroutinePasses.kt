/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.coroutine

import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.*
import org.jetbrains.kotlin.js.inline.util.collectFreeVariables
import org.jetbrains.kotlin.js.inline.util.replaceNames
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.pureFqn
import org.jetbrains.kotlin.js.translate.utils.splitToRanges

fun JsNode.collectNodesToSplit(breakContinueTargets: Map<JsContinue, JsStatement>): Set<JsNode> {
    val root = this
    val nodes = mutableSetOf<JsNode>()

    val visitor = object : RecursiveJsVisitor() {
        var childrenInSet = false
        var finallyLevel = 0

        override fun visitExpressionStatement(x: JsExpressionStatement) {
            super.visitExpressionStatement(x)
            if (x.expression.isSuspend) {
                nodes += x.expression
                childrenInSet = true
            }
            else {
                val assignment = JsAstUtils.decomposeAssignment(x.expression)
                if (assignment != null && assignment.second.isSuspend) {
                    nodes += assignment.second
                    childrenInSet = true
                }
            }
        }

        override fun visitReturn(x: JsReturn) {
            super.visitReturn(x)

            if (root in nodes || finallyLevel > 0) {
                nodes += x
                childrenInSet = true
            }
        }

        // We don't handle JsThrow case here the same way as we do for JsReturn.
        // Exception will be caught by the surrounding catch and then dispatched to a corresponding $exceptionState.
        // Even if there's no `catch` clause, we generate a fake one that dispatches to a finally block.

        override fun visitBreak(x: JsBreak) {
            super.visitBreak(x)

            val breakTarget = breakContinueTargets[x]!!
            if (breakTarget in nodes) {
                nodes += x
                childrenInSet = true
            }
        }

        override fun visitContinue(x: JsContinue) {
            super.visitContinue(x)

            val continueTarget = breakContinueTargets[x]!!
            if (continueTarget in nodes) {
                nodes += x
                childrenInSet = true
            }
        }

        override fun visitTry(x: JsTry) {
            if (x.finallyBlock != null) {
                finallyLevel++
            }
            super.visitTry(x)
            if (x.finallyBlock != null) {
                finallyLevel--
            }
        }

        override fun visitElement(node: JsNode) {
            val oldChildrenInSet = childrenInSet
            childrenInSet = false

            node.acceptChildren(this)

            if (childrenInSet) {
                nodes += node
            }
            else {
                childrenInSet = oldChildrenInSet
            }
        }
    }

    while (true) {
        val countBefore = nodes.size
        visitor.accept(this)
        val countAfter = nodes.size
        if (countAfter == countBefore) break
    }

    return nodes
}

fun List<CoroutineBlock>.replaceCoroutineFlowStatements(context: CoroutineTransformationContext) {
    val blockIndexes = withIndex().associate { (index, block) -> Pair(block, index) }

    val blockReplacementVisitor = object : JsVisitorWithContextImpl() {
        override fun endVisit(x: JsDebugger, ctx: JsContext<in JsStatement>) {
            val target = x.targetBlock
            if (target != null) {
                val lhs = JsNameRef(context.metadata.stateName, JsAstUtils.stateMachineReceiver())
                val rhs = JsIntLiteral(blockIndexes[target]!!)
                ctx.replaceMe(JsExpressionStatement(JsAstUtils.assignment(lhs, rhs).source(x.source)).apply {
                    targetBlock = true
                })
            }

            val exceptionTarget = x.targetExceptionBlock
            if (exceptionTarget != null) {
                val lhs = JsNameRef(context.metadata.exceptionStateName, JsAstUtils.stateMachineReceiver())
                val rhs = JsIntLiteral(blockIndexes[exceptionTarget]!!)
                ctx.replaceMe(JsExpressionStatement(JsAstUtils.assignment(lhs, rhs).source(x.source)).apply {
                    targetExceptionBlock = true
                })
            }

            val finallyPath = x.finallyPath
            if (finallyPath != null) {
                if (finallyPath.isNotEmpty()) {
                    val lhs = JsNameRef(context.metadata.finallyPathName, JsAstUtils.stateMachineReceiver())
                    val rhs = JsArrayLiteral(finallyPath.map { JsIntLiteral(blockIndexes[it]!!) })
                    ctx.replaceMe(JsExpressionStatement(JsAstUtils.assignment(lhs, rhs).source(x.source)).apply {
                        this.finallyPath = true
                    })
                }
                else {
                    ctx.removeMe()
                }
            }
        }
    }
    return forEach { blockReplacementVisitor.accept(it.jsBlock) }
}

fun CoroutineBlock.buildGraph(globalCatchBlock: CoroutineBlock?): Map<CoroutineBlock, Set<CoroutineBlock>> {
    // That's a little more than DFS due to need of tracking finally paths

    val visitedBlocks = mutableSetOf<CoroutineBlock>()
    val graph = mutableMapOf<CoroutineBlock, MutableSet<CoroutineBlock>>()

    fun visitBlock(block: CoroutineBlock) {
        if (block in visitedBlocks) return

        for (finallyPath in block.collectFinallyPaths()) {
            for ((finallySource, finallyTarget) in (listOf(block) + finallyPath).zip(finallyPath)) {
                if (graph.getOrPut(finallySource) { mutableSetOf() }.add(finallyTarget)) {
                    visitedBlocks -= finallySource
                }
            }
        }

        visitedBlocks += block

        val successors = graph.getOrPut(block) { mutableSetOf() }
        successors += block.collectTargetBlocks()
        if (block == this && globalCatchBlock != null) {
            successors += globalCatchBlock
        }
        successors.forEach(::visitBlock)
    }

    visitBlock(this)

    return graph
}

private fun CoroutineBlock.collectTargetBlocks(): Set<CoroutineBlock> {
    val targetBlocks = mutableSetOf<CoroutineBlock>()
    jsBlock.accept(object : RecursiveJsVisitor() {
        override fun visitDebugger(x: JsDebugger) {
            targetBlocks += listOfNotNull(x.targetExceptionBlock) + listOfNotNull(x.targetBlock)
        }
    })
    return targetBlocks
}

private fun CoroutineBlock.collectFinallyPaths(): List<List<CoroutineBlock>> {
    val finallyPaths = mutableListOf<List<CoroutineBlock>>()
    jsBlock.accept(object : RecursiveJsVisitor() {
        override fun visitDebugger(x: JsDebugger) {
            x.finallyPath?.let { finallyPaths += it }
        }
    })
    return finallyPaths
}

fun JsBlock.replaceSpecialReferences(context: CoroutineTransformationContext) {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun endVisit(x: JsThisRef, ctx: JsContext<in JsNode>) {
            ctx.replaceMe(JsNameRef(context.receiverFieldName, JsThisRef()))
        }

        override fun visit(x: JsFunction, ctx: JsContext<*>) = false

        override fun endVisit(x: JsNameRef, ctx: JsContext<in JsNode>) {
            when {
                x.coroutineReceiver -> {
                    ctx.replaceMe(JsThisRef())
                }

                x.coroutineController -> {
                    ctx.replaceMe(JsNameRef(context.controllerFieldName, x.qualifier).apply {
                        source = x.source
                        sideEffects = SideEffectKind.PURE
                    })
                }

                x.coroutineResult -> {
                    ctx.replaceMe(JsNameRef(context.metadata.resultName, x.qualifier).apply {
                        source = x.source
                        sideEffects = SideEffectKind.DEPENDS_ON_STATE
                    })
                }
            }
        }
    }
    visitor.accept(this)
}

fun JsBlock.replaceSpecialReferencesInSimpleFunction(continuationParam: JsParameter, resultVar: JsName) {
    val visitor = object : JsVisitorWithContextImpl() {
        override fun visit(x: JsFunction, ctx: JsContext<*>) = false

        override fun endVisit(x: JsNameRef, ctx: JsContext<in JsNode>) {
            when {
                x.coroutineReceiver -> {
                    ctx.replaceMe(pureFqn(continuationParam.name, null).source(x.source))
                }

                x.coroutineController -> {
                    ctx.replaceMe(JsThisRef().apply {
                        source = x.source
                    })
                }

                x.coroutineResult && x.qualifier.let { it is JsNameRef && it.name == continuationParam.name } -> {
                    ctx.replaceMe(pureFqn(resultVar, null).source(x.source))
                }
            }
        }
    }
    visitor.accept(this)
}

fun List<CoroutineBlock>.collectVariablesSurvivingBetweenBlocks(localVariables: Set<JsName>, parameters: Set<JsName>): Set<JsName> {
    val varDefinedIn = localVariables.associate { it to mutableSetOf<Int>() }
    val varDeclaredIn = localVariables.associate { it to mutableSetOf<Int>() }
    val varUsedIn = localVariables.associate { it to mutableSetOf<Int>() }

    for ((blockIndex, block) in withIndex()) {
        for (statement in block.statements) {
            statement.accept(object : RecursiveJsVisitor() {
                override fun visitNameRef(nameRef: JsNameRef) {
                    super.visitNameRef(nameRef)
                    varUsedIn[nameRef.name]?.add(blockIndex)
                }

                override fun visit(x: JsVars.JsVar) {
                    varDeclaredIn[x.name]?.add(blockIndex)
                    if (x.initExpression != null) {
                        varDefinedIn[x.name]?.add(blockIndex)
                    }
                    super.visit(x)

                }

                override fun visitParameter(x: JsParameter) {
                    varDeclaredIn[x.name]?.add(blockIndex)
                    varDefinedIn[x.name]?.add(blockIndex)
                    super.visitParameter(x)
                }

                override fun visitBinaryExpression(x: JsBinaryOperation) {
                    val lhs = x.arg1
                    if (x.operator.isAssignment && lhs is JsNameRef) {
                        varDefinedIn[lhs.name]?.add(blockIndex)?.let {
                            accept(x.arg2)
                            return
                        }
                    }
                    super.visitBinaryExpression(x)
                }

                override fun visitFunction(x: JsFunction) {
                    x.name?.let {
                        varDefinedIn[it]?.add(blockIndex)
                    }
                }

                override fun visitLabel(x: JsLabel) {
                    accept(x.statement)
                }

                override fun visitBreak(x: JsBreak) {}

                override fun visitContinue(x: JsContinue) {}
            })
        }
    }

    fun JsName.isLocalInBlock(): Boolean {
        val def = varDefinedIn[this]!!
        val use = varUsedIn[this]!!
        val decl = varDeclaredIn[this]!!
        if (def.size == 1 && use.size == 1) {
            val singleDef = def.single()
            val singleUse = use.single()
            return singleDef == singleUse && decl.isNotEmpty()
        }
        return use.isEmpty()
    }

    return localVariables.filterNot { localVar ->
        if (localVar in parameters) {
            varUsedIn[localVar]!!.isEmpty() && varDefinedIn[localVar]!!.isEmpty() && varDeclaredIn[localVar]!!.isEmpty()
        }
        else {
            localVar.isLocalInBlock()
        }
    }.toSet()

}

fun JsBlock.replaceLocalVariables(context: CoroutineTransformationContext, localVariables: Set<JsName>) {
    replaceSpecialReferences(context)

    val visitor = object : JsVisitorWithContextImpl() {
        override fun visit(x: JsFunction, ctx: JsContext<*>): Boolean = false

        override fun endVisit(x: JsFunction, ctx: JsContext<in JsNode>) {
            val freeVars = x.collectFreeVariables().intersect(localVariables)
            if (freeVars.isNotEmpty()) {
                val wrapperFunction = JsFunction(x.scope.parent, JsBlock(), "")
                val wrapperInvocation = JsInvocation(wrapperFunction)
                wrapperFunction.body.statements += JsReturn(x)
                val nameMap = freeVars.associate { it to JsScope.declareTemporaryName(it.ident) }
                for (freeVar in freeVars) {
                    wrapperFunction.parameters += JsParameter(nameMap[freeVar]!!)
                    wrapperInvocation.arguments += JsNameRef(context.getFieldName(freeVar), JsThisRef())
                }
                x.body = replaceNames(x.body, nameMap.mapValues { it.value.makeRef() })
                ctx.replaceMe(wrapperInvocation)
            }
        }

        override fun endVisit(x: JsNameRef, ctx: JsContext<in JsNode>) {
            if (x.qualifier == null && x.name in localVariables) {
                val fieldName = context.getFieldName(x.name!!)
                ctx.replaceMe(JsNameRef(fieldName, JsThisRef()).source(x.source))
            }
        }

        override fun endVisit(x: JsVars, ctx: JsContext<in JsStatement>) {
            if (x.vars.none { it.name in localVariables }) return

            val statements = mutableListOf<JsStatement>()
            for ((range, shouldReplace) in x.vars.splitToRanges { it.name in localVariables }) {
                if (shouldReplace) {
                    val assignments = x.vars.mapNotNull {
                        val fieldName = context.getFieldName(it.name)
                        val initExpression = it.initExpression
                        if (initExpression != null) {
                            JsAstUtils.assignment(JsNameRef(fieldName, JsThisRef()), it.initExpression)
                        }
                        else {
                            null
                        }
                    }
                    if (assignments.isNotEmpty()) {
                        statements += JsExpressionStatement(JsAstUtils.newSequence(assignments))
                    }
                }
                else {
                    statements += JsVars(*range.toTypedArray())
                }
            }

            if (statements.size == 1) {
                ctx.replaceMe(statements[0])
            }
            else {
                ctx.removeMe()
                ctx.addPrevious(statements)
            }
        }
    }
    visitor.accept(this)
}

internal fun JsExpression?.isStateMachineResult() =
        this is JsNameRef && this.coroutineResult && qualifier.let { it is JsNameRef && it.coroutineReceiver && it.qualifier == null }