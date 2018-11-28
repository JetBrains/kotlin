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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.js.backend.ast.metadata.coroutineMetadata
import org.jetbrains.kotlin.js.backend.ast.metadata.forceStateMachine
import org.jetbrains.kotlin.js.backend.ast.metadata.isSuspend
import org.jetbrains.kotlin.js.backend.ast.metadata.synthetic
import org.jetbrains.kotlin.js.inline.clean.FunctionPostProcessor
import org.jetbrains.kotlin.js.inline.util.collectLocalVariables
import org.jetbrains.kotlin.js.inline.util.getInnerFunction
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils.*
import org.jetbrains.kotlin.js.translate.utils.finalElement

class CoroutineFunctionTransformer(private val function: JsFunction, name: String?) {
    private val innerFunction = function.getInnerFunction()
    private val functionWithBody = innerFunction ?: function
    private val body = functionWithBody.body
    private val localVariables = (function.collectLocalVariables() + functionWithBody.collectLocalVariables() -
                                  functionWithBody.parameters.last().name).toMutableSet()
    private val className = JsScope.declareTemporaryName("Coroutine\$${name ?: "anonymous"}")

    fun transform(): List<JsStatement> {
        if (isTailCall() && !function.forceStateMachine) {
            transformSimple()
            return emptyList()
        }

        val context = CoroutineTransformationContext(function.scope, function)
        val bodyTransformer = CoroutineBodyTransformer(context)
        bodyTransformer.preProcess(body)
        body.statements.forEach { it.accept(bodyTransformer) }
        val coroutineBlocks = bodyTransformer.postProcess()
        val globalCatchBlockIndex = coroutineBlocks.indexOf(context.globalCatchBlock)

        coroutineBlocks.forEach { it.jsBlock.collectAdditionalLocalVariables() }

        val survivingLocalVars = coroutineBlocks.collectVariablesSurvivingBetweenBlocks(
                localVariables, function.parameters.map { it.name }.toSet())
        coroutineBlocks.forEach { it.jsBlock.replaceLocalVariables(context, survivingLocalVars) }

        val additionalStatements = mutableListOf<JsStatement>()
        generateDoResume(coroutineBlocks, context, additionalStatements)
        generateContinuationConstructor(context, additionalStatements, globalCatchBlockIndex, survivingLocalVars)

        generateCoroutineInstantiation(context)

        return additionalStatements
    }

    private fun isTailCall(): Boolean {
        val suspendCalls = hashSetOf<JsExpression>()
        body.accept(object : RecursiveJsVisitor() {
            override fun visitElement(node: JsNode) {
                if (node is JsExpression && node.isSuspend) {
                    suspendCalls += node
                }
                super.visitElement(node)
            }
        })

        if (suspendCalls.isEmpty()) return true

        body.accept(object : RecursiveJsVisitor() {
            override fun visitBlock(x: JsBlock) {
                super.visitBlock(x)

                if (body.statements.size < 2) return

                val lastStatement = body.statements.last() as? JsReturn ?: return
                if (!lastStatement.expression.isStateMachineResult()) return

                val statementBeforeLast = body.statements[body.statements.lastIndex - 1] as? JsExpressionStatement ?: return
                val suspendExpression = statementBeforeLast.expression
                if (suspendExpression in suspendCalls) {
                    suspendCalls -= suspendExpression
                }
                else {
                    decomposeAssignment(suspendExpression)?.let { (lhs, rhs) ->
                        if (rhs in suspendCalls && lhs.isStateMachineResult()) {
                            suspendCalls -= rhs
                        }
                    }
                }
            }
        })

        return suspendCalls.isEmpty()
    }

    private fun transformSimple() {
        val continuationParam = function.parameters.last()
        val resultVar = JsScope.declareTemporaryName("\$result")
        body.replaceSpecialReferencesInSimpleFunction(continuationParam, resultVar)
        body.statements.add(0, newVar(resultVar, null).apply { synthetic = true })

        object : JsVisitorWithContextImpl() {
            override fun endVisit(x: JsExpressionStatement, ctx: JsContext<in JsStatement>) {
                if (x.expression.isSuspend) {
                    ctx.replaceMe(assignment(pureFqn(resultVar, null), x.expression).source(x.source).makeStmt())
                }
                super.endVisit(x, ctx)
            }
        }.accept(body)

        FunctionPostProcessor(functionWithBody).apply()
    }

    private fun generateContinuationConstructor(
            context: CoroutineTransformationContext,
            statements: MutableList<JsStatement>,
            globalCatchBlockIndex: Int,
            survivingLocalVars: Set<JsName>
    ) {
        val psiElement = context.metadata.psiElement

        val constructor = JsFunction(function.scope.parent, JsBlock(), "Continuation")
        constructor.source = psiElement?.finalElement
        constructor.name = className
        if (context.metadata.hasReceiver) {
            constructor.parameters += JsParameter(context.receiverFieldName)
        }
        val parameters = function.parameters + innerFunction?.parameters.orEmpty()
        constructor.parameters += parameters.map { JsParameter(it.name) }
        val lastParameter = parameters.lastOrNull()?.name

        val controllerName = if (context.metadata.hasController) {
            JsScope.declareTemporaryName("controller").apply {
                constructor.parameters.add(constructor.parameters.lastIndex, JsParameter(this))
            }
        }
        else {
            null
        }

        val interceptorRef = lastParameter!!.makeRef()
        val parameterNames = (function.parameters.map { it.name } + innerFunction?.parameters?.map { it.name }.orEmpty()).toSet()

        constructor.body.statements.run {
            val baseClass = context.metadata.baseClassRef.deepCopy()
            this += JsInvocation(Namer.getFunctionCallRef(baseClass), JsThisRef(), interceptorRef).source(psiElement).makeStmt()
            if (controllerName != null) {
                assignToField(context.controllerFieldName, controllerName.makeRef(), psiElement)
            }
            assignToField(context.metadata.exceptionStateName, JsIntLiteral(globalCatchBlockIndex), psiElement)
            if (context.metadata.hasReceiver) {
                assignToField(context.receiverFieldName, context.receiverFieldName.makeRef(), psiElement)
            }
            for (localVariable in survivingLocalVars) {
                val value = if (localVariable !in parameterNames) Namer.getUndefinedExpression() else localVariable.makeRef()
                assignToField(context.getFieldName(localVariable), value, psiElement)
            }
        }

        statements.addAll(0, listOf(constructor.makeStmt(), generateCoroutineMetadata(constructor.name)) +
                generateCoroutinePrototype(constructor.name))
    }

    private fun generateCoroutinePrototype(constructorName: JsName): List<JsStatement> {
        val prototype = prototypeOf(JsNameRef(constructorName))

        val baseClass = Namer.createObjectWithPrototypeFrom(function.coroutineMetadata!!.baseClassRef.deepCopy())
        val assignPrototype = assignment(prototype, baseClass)
        val assignConstructor = assignment(JsNameRef("constructor", prototype.deepCopy()), JsNameRef(constructorName))
        return listOf(assignPrototype.makeStmt(), assignConstructor.makeStmt())
    }

    private fun generateCoroutineMetadata(constructorName: JsName): JsStatement {
        val baseClassRefRef = function.coroutineMetadata!!.baseClassRef.deepCopy()

        val metadataObject = JsObjectLiteral(true).apply {
            propertyInitializers +=
                    JsPropertyInitializer(JsNameRef(Namer.METADATA_CLASS_KIND),
                                          JsNameRef(Namer.CLASS_KIND_CLASS, JsNameRef(Namer.CLASS_KIND_ENUM, Namer.KOTLIN_NAME)))
            propertyInitializers += JsPropertyInitializer(JsNameRef(Namer.METADATA_SIMPLE_NAME), JsNullLiteral())
            propertyInitializers += JsPropertyInitializer(JsNameRef(Namer.METADATA_SUPERTYPES), JsArrayLiteral(listOf(baseClassRefRef)))
        }

        return assignment(JsNameRef(Namer.METADATA, constructorName.makeRef()), metadataObject).makeStmt()
    }

    private fun generateDoResume(
            coroutineBlocks: List<CoroutineBlock>,
            context: CoroutineTransformationContext,
            statements: MutableList<JsStatement>
    ) {
        val resumeFunction = JsFunction(function.scope.parent, JsBlock(), "resume function")
        resumeFunction.source = context.metadata.psiElement?.finalElement

        val coroutineBody = generateCoroutineBody(context, coroutineBlocks)
        functionWithBody.body.statements.clear()

        resumeFunction.body.statements.apply {
            this += coroutineBody
        }

        val resumeName = context.metadata.doResumeName
        statements.apply {
            assignToPrototype(resumeName, resumeFunction)
        }

        FunctionPostProcessor(resumeFunction).apply()
    }

    private fun generateCoroutineInstantiation(context: CoroutineTransformationContext) {
        val psiElement = context.metadata.psiElement
        val instantiation = JsNew(className.makeRef()).apply { source = psiElement }
        if (context.metadata.hasReceiver) {
            instantiation.arguments += JsThisRef()
        }
        val parameters = function.parameters + innerFunction?.parameters.orEmpty()
        instantiation.arguments += parameters.dropLast(1).map { it.name.makeRef() }

        if (function.coroutineMetadata!!.hasController) {
            instantiation.arguments += JsThisRef()
        }

        instantiation.arguments += parameters.last().name.makeRef()

        val suspendedName = JsScope.declareTemporaryName("suspended")
        functionWithBody.parameters += JsParameter(suspendedName)

        val instanceName = JsScope.declareTemporaryName("instance")
        functionWithBody.body.statements += newVar(instanceName, instantiation)

        val invokeResume = JsReturn(JsInvocation(JsNameRef(context.metadata.doResumeName, instanceName.makeRef()), JsNullLiteral())
                                            .source(psiElement))

        functionWithBody.body.statements += JsIf(
                suspendedName.makeRef().source(psiElement),
                JsReturn(instanceName.makeRef().source(psiElement)),
                invokeResume)
    }

    private fun generateCoroutineBody(
            context: CoroutineTransformationContext,
            blocks: List<CoroutineBlock>
    ): List<JsStatement> {
        val indexOfGlobalCatch = blocks.indexOf(context.globalCatchBlock)
        val stateRef = JsNameRef(context.metadata.stateName, JsThisRef())
        val exceptionStateRef = JsNameRef(context.metadata.exceptionStateName, JsThisRef())

        val isFromGlobalCatch = equality(stateRef, JsIntLiteral(indexOfGlobalCatch))
        val catch = JsCatch(functionWithBody.scope, "e")
        val continueWithException = JsBlock(
                assignment(stateRef.deepCopy(), exceptionStateRef.deepCopy()).makeStmt(),
                assignment(JsNameRef(context.metadata.exceptionName, JsThisRef()),
                                      catch.parameter.name.makeRef()).makeStmt()
        )
        val adjustExceptionState = assignment(exceptionStateRef.deepCopy(), stateRef.deepCopy()).makeStmt()
        catch.body = JsBlock(JsIf(
                isFromGlobalCatch,
                JsBlock(adjustExceptionState, JsThrow(catch.parameter.name.makeRef())),
                continueWithException
        ))

        val throwResultRef = JsNameRef(context.metadata.exceptionName, JsThisRef())
        context.globalCatchBlock.statements += JsThrow(throwResultRef)

        val cases = blocks.withIndex().map { (index, block) ->
            JsCase().apply {
                caseExpression = JsIntLiteral(index)
                statements += block.statements
            }
        }

        // NOTE: temporary workaround to let tests run without hanging
        // TODO: probably default statement should be removed asap the issue about nested break & finally is fixed
        val defaultCase = JsDefault().apply {
            val block = JsBlock(
                assignment(stateRef, JsIntLiteral(indexOfGlobalCatch)).makeStmt(),
                JsThrow(JsNew(JsNameRef("Error"), listOf(JsStringLiteral("State Machine Unreachable execution"))))
            )
            statements += block
        }

        val switchStatement = JsSwitch(stateRef.deepCopy(), cases + defaultCase)
        val loop = JsDoWhile(JsBooleanLiteral(true), JsTry(JsBlock(switchStatement), catch, null))

        return listOf(loop)
    }

    private fun JsBlock.collectAdditionalLocalVariables() {
        accept(object : RecursiveJsVisitor() {
            override fun visit(x: JsVars.JsVar) {
                super.visit(x)
                localVariables += x.name
            }
        })
    }

    private fun MutableList<JsStatement>.assignToField(fieldName: JsName, value: JsExpression, psiElement: PsiElement?) {
        this += assignment(JsNameRef(fieldName, JsThisRef()), value).source(psiElement).makeStmt()
    }

    private fun MutableList<JsStatement>.assignToPrototype(fieldName: JsName, value: JsExpression) {
        this += assignment(JsNameRef(fieldName, prototypeOf(className.makeRef())), value).makeStmt()
    }
}