/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.google.dart.compiler.backend.js.ast.*
import com.google.dart.compiler.backend.js.ast.metadata.MetadataProperty
import com.google.dart.compiler.backend.js.ast.metadata.isSuspend
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.utils.DFS
import org.jetbrains.kotlin.utils.singletonOrEmptyList

class CoroutineBodyTransformer(val program: JsProgram, val scope: JsScope, val throwFunctionName: JsName?) : RecursiveJsVisitor() {
    val entryBlock = CoroutineBlock()
    val globalCatchBlock = CoroutineBlock()
    private var currentBlock = entryBlock
    private val currentStatements: MutableList<JsStatement>
        get() = currentBlock.statements
    val resultFieldName by lazy { scope.declareFreshName("\$result") }
    val exceptionFieldName by lazy { scope.declareFreshName("\$exception") }
    val stateFieldName by lazy { scope.declareFreshName("\$state") }
    val controllerFieldName by lazy { scope.declareFreshName("\$controller") }
    val exceptionStateName by lazy { scope.declareFreshName("\$exceptionState") }
    val finallyPathFieldName by lazy { scope.declareFreshName("\$finallyPath") }
    private val breakTargets = mutableMapOf<JsName, JumpTarget>()
    private val continueTargets = mutableMapOf<JsName, JumpTarget>()
    private var defaultBreakTarget: JumpTarget = JumpTarget(JsEmpty, 0)
    private var defaultContinueTarget: JumpTarget = JumpTarget(JsEmpty, 0)
    private val referencedBlocks = mutableSetOf<CoroutineBlock>()
    private val breakBlocks = mutableMapOf<JsStatement, CoroutineBlock>()
    private val continueBlocks = mutableMapOf<JsStatement, CoroutineBlock>()
    private lateinit var nodesToSplit: Set<JsNode>
    private var currentCatchBlock = globalCatchBlock
    private val tryStack = mutableListOf(TryBlock(globalCatchBlock, null))

    var hasFinallyBlocks = false
        get
        private set

    private val currentTryDepth = tryStack.lastIndex

    fun preProcess(node: JsNode) {
        val nodes = mutableSetOf<JsNode>()

        val visitor = object : RecursiveJsVisitor() {
            var childrenInSet = false

            override fun visitInvocation(invocation: JsInvocation) {
                super.visitInvocation(invocation)
                if (invocation.isSuspend) {
                    nodes += invocation
                    childrenInSet = true
                }
            }

            override fun visitReturn(x: JsReturn) {
                super.visitReturn(x)
                nodes += x
                childrenInSet = true
            }

            override fun visitBreak(x: JsBreak) {
                super.visitBreak(x)

                // It's a simplification
                // TODO: don't split break and continue statements when possible
                nodes += x
                childrenInSet = true
            }

            override fun visitContinue(x: JsContinue) {
                super.visitContinue(x)
                nodes += x
                childrenInSet = true
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

        visitor.accept(node)
        visitor.accept(node)

        nodesToSplit = nodes

        currentStatements += exceptionState(currentCatchBlock)
    }

    fun postProcess(): List<CoroutineBlock> {
        currentBlock.statements += JsReturn()
        val graph = buildBlockGraph()
        val orderedBlocks = DFS.topologicalOrder(listOf(entryBlock)) { graph[it].orEmpty() }
        val blockIndexes = orderedBlocks.withIndex().associate { (index, block) -> Pair(block, index) }

        val blockReplacementVisitor = object : JsVisitorWithContextImpl() {
            override fun endVisit(x: JsDebugger, ctx: JsContext<in JsStatement>) {
                val target = x.targetBlock
                if (target != null) {
                    val lhs = JsNameRef(stateFieldName, JsLiteral.THIS)
                    val rhs = program.getNumberLiteral(blockIndexes[target]!!)
                    ctx.replaceMe(JsAstUtils.assignment(lhs, rhs).makeStmt())
                }

                val exceptionTarget = x.targetExceptionBlock
                if (exceptionTarget != null) {
                    val lhs = JsNameRef(exceptionStateName, JsLiteral.THIS)
                    val rhs = program.getNumberLiteral(blockIndexes[exceptionTarget]!!)
                    ctx.replaceMe(JsAstUtils.assignment(lhs, rhs).makeStmt())
                }

                val finallyPath = x.finallyPath
                if (finallyPath != null) {
                    if (finallyPath.isNotEmpty()) {
                        val lhs = JsNameRef(finallyPathFieldName, JsLiteral.THIS)
                        val rhs = JsArrayLiteral(finallyPath.map { program.getNumberLiteral(blockIndexes[it]!!) })
                        ctx.replaceMe(JsAstUtils.assignment(lhs, rhs).makeStmt())
                    }
                    else {
                        ctx.removeMe()
                    }
                }
            }
        }
        for (block in orderedBlocks) {
            blockReplacementVisitor.accept(block.jsBlock)
        }

        return orderedBlocks
    }

    private fun buildBlockGraph(): Map<CoroutineBlock, Set<CoroutineBlock>> {
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
            successors.forEach(::visitBlock)
        }

        visitBlock(entryBlock)

        return graph
    }

    override fun visitBlock(x: JsBlock) = splitIfNecessary(x) {
        for (statement in x.statements) {
            statement.accept(this)
        }
    }

    override fun visitIf(x: JsIf) = splitIfNecessary(x) {
        x.ifExpression = handleExpression(x.ifExpression)

        val ifBlock = currentBlock

        val thenEntryBlock = CoroutineBlock()
        currentBlock = thenEntryBlock
        x.thenStatement?.accept(this)
        val thenExitBlock = currentBlock

        val elseEntryBlock = CoroutineBlock()
        currentBlock = elseEntryBlock
        x.elseStatement?.accept(this)
        val elseExitBlock = currentBlock

        x.thenStatement = JsBlock(thenEntryBlock.statements)
        x.elseStatement = JsBlock(elseEntryBlock.statements)
        ifBlock.statements += x

        val jointBlock = CoroutineBlock()
        thenExitBlock.statements += stateAndJump(jointBlock)
        elseExitBlock.statements += stateAndJump(jointBlock)
        currentBlock = jointBlock
    }

    override fun visitLabel(x: JsLabel) {
        val inner = x.statement
        when (inner) {
            is JsDoWhile -> handleDoWhile(inner, x.name)

            is JsWhile -> handleWhile(inner, x.name)

            is JsFor -> handleFor(inner, x.name)

            else -> splitIfNecessary(x) {
                val successor = CoroutineBlock()
                withBreakAndContinue(x.name, x.statement, successor, null) {
                    accept(inner)
                }
                if (successor in referencedBlocks) {
                    currentBlock.statements += stateAndJump(successor)
                    currentBlock = successor
                }
            }
        }
    }

    override fun visitWhile(x: JsWhile) = handleWhile(x, null)

    private fun handleWhile(x: JsWhile, label: JsName?) = splitIfNecessary(x) {
        val predecessor = currentBlock
        val successor = CoroutineBlock()

        val bodyEntryBlock = CoroutineBlock()
        currentBlock = bodyEntryBlock
        withBreakAndContinue(label, x, successor, bodyEntryBlock) {
            x.body.accept(this)
        }

        if (x.condition != JsLiteral.TRUE) {
            val jsIf = JsIf(JsAstUtils.notOptimized(x.condition), JsBlock(stateAndJump(successor))).apply { source = x.source }
            bodyEntryBlock.statements.add(0, jsIf)
        }
        currentBlock.statements += stateAndJump(bodyEntryBlock)
        predecessor.statements += stateAndJump(bodyEntryBlock)
        currentBlock = successor
    }

    override fun visitDoWhile(x: JsDoWhile) = handleDoWhile(x, null)

    private fun handleDoWhile(x: JsDoWhile, label: JsName?) = splitIfNecessary(x) {
        val predecessor = currentBlock
        val successor = CoroutineBlock()

        val bodyEntryBlock = CoroutineBlock()
        currentBlock = bodyEntryBlock
        withBreakAndContinue(label, x, successor, bodyEntryBlock) {
            x.body.accept(this)
        }

        if (x.condition != JsLiteral.TRUE) {
            val jsIf = JsIf(JsAstUtils.notOptimized(x.condition), JsBlock(stateAndJump(successor))).apply { source = x.source }
            currentBlock.statements.add(jsIf)
        }
        currentBlock.statements += stateAndJump(bodyEntryBlock)
        predecessor.statements += stateAndJump(bodyEntryBlock)
        currentBlock = successor
    }

    override fun visitFor(x: JsFor) = handleFor(x, null)

    private fun handleFor(x: JsFor, label: JsName?) = splitIfNecessary(x) {
        x.initExpression?.let {
            JsExpressionStatement(it).accept(this)
        }
        x.initVars?.let { initVars ->
            if (initVars.vars.isNotEmpty()) {
                initVars.accept(this)
            }
        }

        val predecessor = currentBlock
        val increment = CoroutineBlock()
        val successor = CoroutineBlock()

        val bodyEntryBlock = CoroutineBlock()
        currentBlock = bodyEntryBlock
        withBreakAndContinue(label, x, successor, predecessor) {
            x.body.accept(this)
        }
        val bodyExitBlock = currentBlock

        if (x.condition != null && x.condition != JsLiteral.TRUE) {
            val jsIf = JsIf(JsAstUtils.notOptimized(x.condition), JsBlock(stateAndJump(successor))).apply { source = x.source }
            bodyEntryBlock.statements.add(0, jsIf)
        }

        bodyExitBlock.statements += stateAndJump(increment)
        currentBlock = increment

        x.incrementExpression?.let { JsExpressionStatement(it).accept(this) }
        currentStatements += stateAndJump(bodyEntryBlock)

        predecessor.statements += stateAndJump(bodyEntryBlock)
        currentBlock = successor
    }

    override fun visitBreak(x: JsBreak) {
        val targetLabel = x.label?.name
        val (targetStatement, targetTryDepth) = if (targetLabel == null) {
            defaultBreakTarget
        }
        else {
            breakTargets[targetLabel]!!
        }

        val targetBlock = breakBlocks[targetStatement]!!
        referencedBlocks += targetBlock
        jumpWithFinally(targetTryDepth, targetBlock)
        currentStatements += jump()
    }

    override fun visitContinue(x: JsContinue) {
        val targetLabel = x.label?.name
        val (targetStatement, targetTryDepth) = if (targetLabel == null) {
            defaultContinueTarget
        }
        else {
            continueTargets[targetLabel]!!
        }

        val targetBlock = continueBlocks[targetStatement]!!
        referencedBlocks += targetBlock

        jumpWithFinally(targetTryDepth, targetBlock)
        currentStatements += jump()
    }

    private fun jumpWithFinally(targetTryDepth: Int, successor: CoroutineBlock) {
        if (targetTryDepth == tryStack.size) return

        val tryBlock = tryStack[targetTryDepth]
        currentStatements += exceptionState(tryBlock.catchBlock)

        val relativeFinallyPath = relativeFinallyPath(targetTryDepth)
        val fullPath = relativeFinallyPath + successor
        if (fullPath.size > 1) {
            currentStatements += updateFinallyPath(fullPath.drop(1))
        }
        currentStatements += state(fullPath[0])
    }

    override fun visitTry(x: JsTry) = splitIfNecessary(x) {
        val catchNode = x.catches.firstOrNull()
        val finallyNode = x.finallyBlock
        val successor = CoroutineBlock()

        val catchBlock = CoroutineBlock()
        val finallyBlock = CoroutineBlock()

        val tryBlock = TryBlock(catchBlock, if (finallyNode != null) finallyBlock else null)
        tryStack += tryBlock

        val oldCatchBlock = currentCatchBlock
        currentCatchBlock = catchBlock
        currentStatements += exceptionState(catchBlock)

        x.tryBlock.statements.forEach { it.accept(this) }
        currentStatements += exceptionState(oldCatchBlock)
        if (finallyNode != null) {
            currentStatements += updateFinallyPath(listOf(successor))
            currentStatements += stateAndJump(finallyBlock)
        }
        else {
            currentStatements += stateAndJump(successor)
        }

        currentCatchBlock = oldCatchBlock

        // Handle catch node
        currentBlock = catchBlock

        if (finallyNode != null) {
            currentStatements += updateFinallyPath(listOf(oldCatchBlock))
            currentStatements += exceptionState(finallyBlock)
        }
        else {
            currentStatements += exceptionState(oldCatchBlock)
        }

        if (catchNode != null) {
            currentStatements += JsAstUtils.newVar(catchNode.parameter.name, JsNameRef(exceptionFieldName, JsLiteral.THIS))
            catchNode.body.statements.forEach { it.accept(this) }

            if (finallyNode == null) {
                currentStatements += stateAndJump(successor)
            }
            else {
                currentStatements += updateFinallyPath(listOf(successor))
                currentStatements += stateAndJump(finallyBlock)
            }
        }

        // Handle finally node
        if (finallyNode != null) {
            currentBlock = finallyBlock
            finallyNode.statements.forEach { it.accept(this) }
            generateFinallyExit()
            hasFinallyBlocks = true
        }

        tryStack.removeAt(tryStack.lastIndex)

        currentBlock = successor
    }

    private fun generateFinallyExit() {
        val finallyPathRef = JsNameRef(finallyPathFieldName, JsLiteral.THIS)
        val stateRef = JsNameRef(stateFieldName, JsLiteral.THIS)
        val nextState = JsInvocation(JsNameRef("shift", finallyPathRef))
        currentStatements += JsAstUtils.assignment(stateRef, nextState).makeStmt()
        currentStatements += jump()
    }

    override fun visitExpressionStatement(x: JsExpressionStatement) {
        val expression = x.expression
        if (expression is JsInvocation && expression.isSuspend) {
             handleSuspend(expression)
        }
        else {
            val splitExpression = handleExpression(x.expression)
            currentStatements += if (splitExpression == expression) x else JsExpressionStatement(expression)
        }
    }

    override fun visitVars(x: JsVars) {
        super.visitVars(x)
        currentStatements += x
    }

    override fun visit(x: JsVars.JsVar) {
        val initExpression = x.initExpression
        if (initExpression != null) {
            x.initExpression = handleExpression(initExpression)
        }
    }

    override fun visitReturn(x: JsReturn) {
        val returnBlock = CoroutineBlock()
        val isInFinally = hasEnclosingFinallyBlock()
        if (isInFinally) {
            jumpWithFinally(0, returnBlock)
        }

        val returnExpression = x.expression
        if (returnExpression != null) {
            x.expression = handleExpression(returnExpression)
        }

        if (isInFinally) {
            currentStatements += x.expression?.makeStmt().singletonOrEmptyList()
            currentStatements += jump()
            currentBlock = returnBlock
            currentStatements += JsReturn()
        }
        else {
            currentStatements += x
        }
    }

    override fun visitThrow(x: JsThrow) {
        if (throwFunctionName != null) {
            val exception = handleExpression(x.expression)
            val methodRef = JsNameRef(throwFunctionName, JsNameRef(controllerFieldName, JsLiteral.THIS))
            val invocation = JsInvocation(methodRef, exception).apply {
                source = x.source
            }
            currentStatements += JsReturn(invocation)
        }
        else {
            currentStatements += x
        }
    }

    private fun handleExpression(expression: JsExpression): JsExpression {
        if (expression !in nodesToSplit) return expression

        val visitor = object : JsVisitorWithContextImpl() {
            override fun endVisit(x: JsInvocation, ctx: JsContext<in JsExpression>) {
                if (x.isSuspend) {
                    ctx.replaceMe(handleSuspend(x))
                }
                super.endVisit(x, ctx)
            }
        }

        return visitor.accept(expression)
    }

    private fun handleSuspend(invocation: JsInvocation): JsExpression {
        val methodRef = invocation.qualifier as JsNameRef
        methodRef.qualifier = JsNameRef(controllerFieldName, methodRef.qualifier)

        invocation.arguments += JsLiteral.THIS
        val nextBlock = CoroutineBlock()
        currentStatements += state(nextBlock)
        currentStatements += JsReturn(invocation)
        currentBlock = nextBlock

        return JsNameRef(resultFieldName, JsLiteral.THIS)
    }

    private fun state(target: CoroutineBlock): List<JsStatement> {
        val placeholder = JsDebugger()
        placeholder.targetBlock = target

        return listOf(placeholder)
    }

    private fun jump() = JsContinue()

    private fun stateAndJump(target: CoroutineBlock): List<JsStatement> {
        return state(target) + jump()
    }

    private fun exceptionState(target: CoroutineBlock): List<JsStatement> {
        val placeholder = JsDebugger()
        placeholder.targetExceptionBlock = target

        return listOf(placeholder)
    }

    private fun updateFinallyPath(path: List<CoroutineBlock>): List<JsStatement> {
        val placeholder = JsDebugger()
        placeholder.finallyPath = path
        return listOf(placeholder)
    }

    private inline fun splitIfNecessary(statement: JsStatement, action: () -> Unit) {
        if (statement in nodesToSplit) {
            action()
        }
        else {
            currentStatements += statement
        }
    }

    private fun withBreakAndContinue(
            label: JsName?,
            statement: JsStatement,
            breakBlock: CoroutineBlock,
            continueBlock: CoroutineBlock? = null,
            action: () -> Unit
    ) {
        breakBlocks[statement] = breakBlock
        if (continueBlock != null) {
            continueBlocks[statement] = continueBlock
        }

        val oldDefaultBreakTarget = defaultBreakTarget
        val oldDefaultContinueTarget = defaultContinueTarget
        val (oldBreakTarget, oldContinueTarget) = if (label != null) {
            Pair(breakTargets[label], continueTargets[label])
        }
        else {
            Pair(null, null)
        }

        defaultBreakTarget = JumpTarget(statement, currentTryDepth)
        if (label != null) {
            breakTargets[label] = JumpTarget(statement, currentTryDepth)
            if (continueBlock != null) {
                continueTargets[label] = JumpTarget(statement, currentTryDepth)
            }
        }
        if (continueBlock != null) {
            defaultContinueTarget = JumpTarget(statement, currentTryDepth)
        }

        action()

        defaultBreakTarget = oldDefaultBreakTarget
        defaultContinueTarget = oldDefaultContinueTarget
        if (label != null) {
            if (oldBreakTarget == null) {
                breakTargets.keys -= label
            }
            else {
                breakTargets[label] = oldBreakTarget
            }
            if (oldContinueTarget == null) {
                continueTargets.keys -= label
            }
            else {
                continueTargets[label] = oldContinueTarget
            }
        }
    }

    private fun relativeFinallyPath(targetTryDepth: Int) = tryStack.subList(targetTryDepth, tryStack.size).mapNotNull { it.finallyBlock }

    private fun hasEnclosingFinallyBlock() = tryStack.any { it.finallyBlock != null }
}

private data class JumpTarget(val statement: JsStatement, val tryDepth: Int)

private class TryBlock(val catchBlock: CoroutineBlock, val finallyBlock: CoroutineBlock?)

private fun CoroutineBlock.collectTargetBlocks(): Set<CoroutineBlock> {
    val targetBlocks = mutableSetOf<CoroutineBlock>()
    jsBlock.accept(object : RecursiveJsVisitor() {
        override fun visitDebugger(x: JsDebugger) {
            targetBlocks += x.targetExceptionBlock.singletonOrEmptyList() + x.targetBlock.singletonOrEmptyList()
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

private var JsDebugger.targetBlock: CoroutineBlock? by MetadataProperty(default = null)
private var JsDebugger.targetExceptionBlock: CoroutineBlock? by MetadataProperty(default = null)
private var JsDebugger.finallyPath: List<CoroutineBlock>? by MetadataProperty(default = null)