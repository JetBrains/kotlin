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
import com.google.dart.compiler.backend.js.ast.metadata.coroutineController
import com.google.dart.compiler.backend.js.ast.metadata.isSuspend
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.js.inline.ExpressionDecomposer
import org.jetbrains.kotlin.js.inline.clean.FunctionPostProcessor
import org.jetbrains.kotlin.js.inline.util.collectLocalVariables
import org.jetbrains.kotlin.js.inline.util.getInnerFunction
import org.jetbrains.kotlin.js.naming.NameSuggestion
import org.jetbrains.kotlin.js.translate.context.Namer
import org.jetbrains.kotlin.js.translate.utils.JsAstUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

class CoroutineFunctionTransformer(
        private val program: JsProgram,
        private val function: JsFunction,
        private val coroutineType: ClassDescriptor,
        private val controllerType: ClassDescriptor
) {
    private val innerFunction = function.getInnerFunction()
    private val functionWithBody = innerFunction ?: function
    private val body = functionWithBody.body
    private val localVariables = (function.collectLocalVariables() + functionWithBody.collectLocalVariables()).toMutableSet()
    private val nameSuggestion = NameSuggestion()
    private val className = function.scope.parent.declareFreshName("Coroutine\$${function.name}")

    fun transform(): List<JsStatement> {
        val visitor = object : JsVisitorWithContextImpl() {
            override fun <T : JsNode?> doTraverse(node: T, ctx: JsContext<in JsStatement>) {
                super.doTraverse(node, ctx)
                if (node is JsStatement) {
                    val statements = ExpressionDecomposer.preserveEvaluationOrder(function.scope, node) {
                        it is JsInvocation && it.isSuspend
                    }
                    ctx.addPrevious(statements)
                }
            }
        }
        visitor.accept(body)

        val throwFunction = controllerType.findFunction("handleException")
        val throwName = throwFunction?.let {
            val throwId = nameSuggestion.suggest(it)!!.names.last()
            function.scope.declareName(throwId)
        }

        val bodyTransformer = CoroutineBodyTransformer(program, function.scope, throwName)
        bodyTransformer.preProcess(body)
        body.statements.forEach { it.accept(bodyTransformer) }
        val coroutineBlocks = bodyTransformer.postProcess()

        for (coroutineBlock in coroutineBlocks) {
            coroutineBlock.jsBlock.replaceLocalVariables(function.scope, bodyTransformer)
        }

        val additionalStatements = mutableListOf<JsStatement>()
        val resumeName = generateDoResume(coroutineBlocks, bodyTransformer, additionalStatements, throwName)
        generateContinuationConstructor(bodyTransformer, additionalStatements)
        generateContinuationMethods(resumeName, additionalStatements)

        generateCoroutineInstantiation()

        return additionalStatements
    }

    private fun generateContinuationConstructor(bodyTransformer: CoroutineBodyTransformer,statements: MutableList<JsStatement>) {
        val constructor = JsFunction(function.scope.parent, JsBlock(), "Continuation")
        constructor.name = className
        constructor.parameters += function.parameters.map { JsParameter(it.name) }
        if (innerFunction != null) {
            constructor.parameters += innerFunction.parameters.map { JsParameter(it.name) }
        }

        val controllerName = function.scope.declareFreshName("controller")
        constructor.parameters += JsParameter(controllerName)

        val parameterNames = (function.parameters.map { it.name } + innerFunction?.parameters?.map { it.name }.orEmpty()).toSet()

        constructor.body.statements.run {
            assign(bodyTransformer.stateFieldName, program.getNumberLiteral(0))
            assign(bodyTransformer.exceptionStateName, program.getNumberLiteral(0))
            if (bodyTransformer.hasFinallyBlocks) {
                assign(bodyTransformer.finallyPathFieldName, JsLiteral.NULL)
            }
            assign(bodyTransformer.controllerFieldName, controllerName.makeRef())
            for (localVariable in localVariables) {
                val value = if (localVariable !in parameterNames) JsLiteral.NULL else localVariable.makeRef()
                assign(function.scope.getFieldName(localVariable), value)
            }
        }

        statements.add(0, constructor.makeStmt())
    }

    private fun generateContinuationMethods(doResumeName: JsName, statements: MutableList<JsStatement>) {
        generateResumeFunction(doResumeName, statements, "resume", "data", listOf())
        generateResumeFunction(doResumeName, statements, "resumeWithException", "exception", listOf(Namer.getUndefinedExpression()))
    }

    private fun generateResumeFunction(
            doResumeName: JsName,
            statements: MutableList<JsStatement>,
            name: String,
            parameterName: String,
            additionalArgs: List<JsExpression>
    ) {
        val resumeDescriptor = coroutineType.findFunction(name)!!
        val resumeId = nameSuggestion.suggest(resumeDescriptor)!!.names.last()
        val resumeName = function.scope.declareName(resumeId)

        val resumeFunction = JsFunction(function.scope.parent, JsBlock(), resumeDescriptor.toString())
        val resumeParameter = resumeFunction.scope.declareFreshName(parameterName)
        resumeFunction.parameters += JsParameter(resumeParameter)

        resumeFunction.body.statements.apply {
            val invocation = JsInvocation(JsNameRef(doResumeName, JsLiteral.THIS), additionalArgs + resumeParameter.makeRef())
            this += JsReturn(invocation)
        }

        statements.apply {
            assignToPrototype(resumeName, resumeFunction)
        }
    }

    private fun generateDoResume(
            coroutineBlocks: List<CoroutineBlock>,
            bodyTransformer: CoroutineBodyTransformer,
            statements: MutableList<JsStatement>,
            throwName: JsName?
    ): JsName {
        val resumeFunction = JsFunction(function.scope.parent, JsBlock(), "resume function")
        val resumeParameter = resumeFunction.scope.declareFreshName("data")
        val resumeException = resumeFunction.scope.declareFreshName("exception")
        resumeFunction.parameters += listOf(JsParameter(resumeParameter), JsParameter(resumeException))

        val coroutineBody = generateCoroutineBody(bodyTransformer, coroutineBlocks, throwName, resumeException)
        functionWithBody.body.statements.clear()

        resumeFunction.body.statements.apply {
            assign(bodyTransformer.resultFieldName, resumeParameter.makeRef())
            this += coroutineBody
        }

        val resumeName = function.scope.parent.declareFreshName("doResume")
        statements.apply {
            assignToPrototype(resumeName, resumeFunction)
        }

        FunctionPostProcessor(resumeFunction).apply()

        return resumeName
    }

    private fun generateCoroutineInstantiation() {
        val instantiation = JsNew(className.makeRef())
        instantiation.arguments += function.parameters.map { it.name.makeRef() }
        if (innerFunction != null) {
            instantiation.arguments += innerFunction.parameters.map { it.name.makeRef() }
        }

        instantiation.arguments += JsLiteral.THIS

        functionWithBody.body.statements += JsReturn(instantiation)
    }

    private fun generateCoroutineBody(
            transformer: CoroutineBodyTransformer,
            blocks: List<CoroutineBlock>,
            throwName: JsName?,
            exceptionName: JsName
    ): List<JsStatement> {
        val indexOfGlobalCatch = blocks.indexOf(transformer.globalCatchBlock)
        val stateRef = JsNameRef(transformer.stateFieldName, JsLiteral.THIS)

        val isFromGlobalCatch = JsAstUtils.equality(stateRef, program.getNumberLiteral(indexOfGlobalCatch))
        val catch = JsCatch(functionWithBody.scope, "e")
        val continueWithException = JsBlock(
                JsAstUtils.assignment(stateRef.deepCopy(), JsNameRef(transformer.exceptionStateName, JsLiteral.THIS)).makeStmt(),
                JsAstUtils.assignment(JsNameRef(transformer.exceptionFieldName, JsLiteral.THIS), catch.parameter.name.makeRef()).makeStmt()
        )
        catch.body = JsBlock(JsIf(isFromGlobalCatch, JsThrow(catch.parameter.name.makeRef()), continueWithException))

        val throwResultRef = JsNameRef(transformer.exceptionFieldName, JsLiteral.THIS)
        if (throwName != null) {
            val throwMethodRef = JsNameRef(throwName, JsNameRef(transformer.controllerFieldName, JsLiteral.THIS))
            transformer.globalCatchBlock.statements += JsReturn(JsInvocation(throwMethodRef.deepCopy(), throwResultRef))
        }
        else {
            transformer.globalCatchBlock.statements += JsThrow(throwResultRef)
        }

        val cases = blocks.withIndex().map { (index, block) ->
            JsCase().apply {
                caseExpression = program.getNumberLiteral(index)
                statements += block.statements
            }
        }
        val switchStatement = JsSwitch(stateRef.deepCopy(), cases)
        val loop = JsDoWhile(JsLiteral.TRUE, JsTry(JsBlock(switchStatement), catch, null))

        val testExceptionPassed = JsAstUtils.notOptimized(
                JsAstUtils.typeOfIs(exceptionName.makeRef(), program.getStringLiteral("undefined")))
        val stateToException = JsAstUtils.assignment(
                JsNameRef(transformer.stateFieldName, JsLiteral.THIS),
                JsNameRef(transformer.exceptionStateName, JsLiteral.THIS))
        val exceptionToResult = JsAstUtils.assignment(JsNameRef(transformer.exceptionFieldName, JsLiteral.THIS), exceptionName.makeRef())
        val throwExceptionIfNeeded = JsIf(testExceptionPassed, JsBlock(stateToException.makeStmt(), exceptionToResult.makeStmt()))

        return listOf(throwExceptionIfNeeded, loop)
    }

    private fun JsBlock.replaceLocalVariables(scope: JsScope, transformer: CoroutineBodyTransformer) {
        accept(object : RecursiveJsVisitor() {
            override fun visit(x: JsVars.JsVar) {
                super.visit(x)
                localVariables += x.name
            }
        })

        val visitor = object : JsVisitorWithContextImpl() {
            override fun endVisit(x: JsNameRef, ctx: JsContext<in JsNode>) {
                if (x.coroutineController) {
                    ctx.replaceMe(JsNameRef(transformer.controllerFieldName, x.qualifier))
                }
                if (x.qualifier == null && x.name in localVariables) {
                    val fieldName = scope.getFieldName(x.name!!)
                    ctx.replaceMe(JsNameRef(fieldName, JsLiteral.THIS))
                }
            }

            override fun endVisit(x: JsVars, ctx: JsContext<in JsStatement>) {
                val declaredNames = x.vars.map { it.name }
                val totalCount = declaredNames.size
                val localVarCount = declaredNames.count()

                when {
                    totalCount == localVarCount -> {
                        val assignments = x.vars.mapNotNull {
                            val fieldName = scope.getFieldName(it.name)
                            val initExpression = it.initExpression
                            if (initExpression != null) {
                                JsAstUtils.assignment(JsNameRef(fieldName, JsLiteral.THIS), it.initExpression)
                            }
                            else {
                                null
                            }
                        }
                        if (assignments.isNotEmpty()) {
                            ctx.replaceMe(JsExpressionStatement(JsAstUtils.newSequence(assignments)))
                        }
                        else {
                            ctx.removeMe()
                        }
                    }
                    localVarCount > 0 -> {
                        for (declaration in x.vars) {
                            if (declaration.name in localVariables) {
                                val fieldName = scope.getFieldName(declaration.name)
                                val assignment = JsAstUtils.assignment(JsNameRef(fieldName, JsLiteral.THIS), declaration.initExpression)
                                ctx.addPrevious(assignment.makeStmt())
                            }
                            else {
                                ctx.addPrevious(JsVars(declaration))
                            }
                        }
                        ctx.removeMe()
                    }
                }
                super.endVisit(x, ctx)
            }
        }
        visitor.accept(this)
    }

    private fun ClassDescriptor.findFunction(name: String): FunctionDescriptor? {
        val functions = unsubstitutedMemberScope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
                .filter { it.name.asString() == name }
        return functions.mapNotNull { it as? FunctionDescriptor }.firstOrNull { it.kind.isReal }
    }

    private fun JsScope.getFieldName(variableName: JsName) = declareName("local\$${variableName.ident}")

    private fun MutableList<JsStatement>.assign(fieldName: JsName, value: JsExpression) {
        this += JsAstUtils.assignment(JsNameRef(fieldName, JsLiteral.THIS), value).makeStmt()
    }

    private fun MutableList<JsStatement>.assignToPrototype(fieldName: JsName, value: JsExpression) {
        this += JsAstUtils.assignment(JsNameRef(fieldName, JsAstUtils.prototypeOf(className.makeRef())), value).makeStmt()
    }
}