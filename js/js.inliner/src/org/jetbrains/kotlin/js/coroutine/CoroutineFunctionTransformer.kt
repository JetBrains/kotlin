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
import com.google.dart.compiler.backend.js.ast.metadata.*
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
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
        val throwFunction = controllerType.findFunction("handleException")
        val throwName = throwFunction?.let {
            val throwId = nameSuggestion.suggest(it)!!.names.last()
            function.scope.declareName(throwId)
        }

        val context = CoroutineTransformationContext(function.scope, function.suspendObjectRef != null)
        val bodyTransformer = CoroutineBodyTransformer(program, context, throwName)
        bodyTransformer.preProcess(body)
        body.statements.forEach { it.accept(bodyTransformer) }
        val coroutineBlocks = bodyTransformer.postProcess()
        val globalCatchBlockIndex = coroutineBlocks.indexOf(context.globalCatchBlock)

        coroutineBlocks.forEach { it.jsBlock.collectAdditionalLocalVariables() }
        coroutineBlocks.forEach { it.jsBlock.replaceLocalVariables(function.scope, context, localVariables) }

        val additionalStatements = mutableListOf<JsStatement>()
        val resumeName = generateDoResume(coroutineBlocks, context, additionalStatements, throwName)
        generateContinuationConstructor(context, additionalStatements, bodyTransformer.hasFinallyBlocks, globalCatchBlockIndex)
        generateContinuationMethods(resumeName, additionalStatements)

        generateCoroutineInstantiation()

        return additionalStatements
    }

    private fun generateContinuationConstructor(
            context: CoroutineTransformationContext,
            statements: MutableList<JsStatement>,
            hasFinallyBlocks: Boolean,
            globalCatchBlockIndex: Int
    ) {
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
            assignToField(context.stateFieldName, program.getNumberLiteral(0))
            assignToField(context.exceptionStateName, program.getNumberLiteral(globalCatchBlockIndex))
            if (hasFinallyBlocks) {
                assignToField(context.finallyPathFieldName, JsLiteral.NULL)
            }
            assignToField(context.controllerFieldName, controllerName.makeRef())
            for (localVariable in localVariables) {
                val value = if (localVariable !in parameterNames) JsLiteral.NULL else localVariable.makeRef()
                assignToField(function.scope.getFieldName(localVariable), value)
            }
        }

        statements.addAll(0, listOf(constructor.makeStmt(), generateCoroutineMetadata(constructor.name)))
    }

    private fun generateCoroutineMetadata(constructorName: JsName): JsStatement {
        val interfaceRef = function.continuationInterfaceRef!!.deepCopy()

        val metadataObject = JsObjectLiteral(true)
        metadataObject.propertyInitializers += JsPropertyInitializer(
                JsNameRef("type"), JsNameRef("CLASS", JsNameRef("TYPE", Namer.KOTLIN_NAME)))
        metadataObject.propertyInitializers += JsPropertyInitializer(
                JsNameRef("classIndex"), JsInvocation(JsNameRef("newClassIndex", Namer.KOTLIN_NAME)))
        metadataObject.propertyInitializers += JsPropertyInitializer(JsNameRef("simpleName"), JsLiteral.NULL)
        metadataObject.propertyInitializers += JsPropertyInitializer(JsNameRef("baseClasses"), JsArrayLiteral(listOf(interfaceRef)))

        return JsAstUtils.assignment(JsNameRef(Namer.METADATA, constructorName.makeRef()), metadataObject).makeStmt()
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
            context: CoroutineTransformationContext,
            statements: MutableList<JsStatement>,
            throwName: JsName?
    ): JsName {
        val resumeFunction = JsFunction(function.scope.parent, JsBlock(), "resume function")
        val resumeParameter = resumeFunction.scope.declareFreshName("data")
        val resumeException = resumeFunction.scope.declareFreshName("exception")
        resumeFunction.parameters += listOf(JsParameter(resumeParameter), JsParameter(resumeException))

        val coroutineBody = generateCoroutineBody(context, coroutineBlocks, throwName, resumeException)
        functionWithBody.body.statements.clear()

        resumeFunction.body.statements.apply {
            assignToField(context.resultFieldName, resumeParameter.makeRef())
            if (context.suspendObjectVar != null) {
                add(JsAstUtils.newVar(context.suspendObjectVar!!, function.suspendObjectRef!!.deepCopy()).apply {
                    synthetic = true
                })
            }
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
            context: CoroutineTransformationContext,
            blocks: List<CoroutineBlock>,
            throwName: JsName?,
            exceptionName: JsName
    ): List<JsStatement> {
        val indexOfGlobalCatch = blocks.indexOf(context.globalCatchBlock)
        val stateRef = JsNameRef(context.stateFieldName, JsLiteral.THIS)

        val isFromGlobalCatch = JsAstUtils.equality(stateRef, program.getNumberLiteral(indexOfGlobalCatch))
        val catch = JsCatch(functionWithBody.scope, "e")
        val continueWithException = JsBlock(
                JsAstUtils.assignment(stateRef.deepCopy(), JsNameRef(context.exceptionStateName, JsLiteral.THIS)).makeStmt(),
                JsAstUtils.assignment(JsNameRef(context.exceptionFieldName, JsLiteral.THIS), catch.parameter.name.makeRef()).makeStmt()
        )
        catch.body = JsBlock(JsIf(isFromGlobalCatch, JsThrow(catch.parameter.name.makeRef()), continueWithException))

        val throwResultRef = JsNameRef(context.exceptionFieldName, JsLiteral.THIS)
        if (throwName != null) {
            val throwMethodRef = JsNameRef(throwName, JsNameRef(context.controllerFieldName, JsLiteral.THIS))
            context.globalCatchBlock.statements += JsReturn(JsInvocation(throwMethodRef.deepCopy(), throwResultRef))
        }
        else {
            context.globalCatchBlock.statements += JsThrow(throwResultRef)
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
                JsNameRef(context.stateFieldName, JsLiteral.THIS),
                JsNameRef(context.exceptionStateName, JsLiteral.THIS))
        val exceptionToResult = JsAstUtils.assignment(JsNameRef(context.exceptionFieldName, JsLiteral.THIS), exceptionName.makeRef())
        val throwExceptionIfNeeded = JsIf(testExceptionPassed, JsBlock(stateToException.makeStmt(), exceptionToResult.makeStmt()))

        return listOf(throwExceptionIfNeeded, loop)
    }

    private fun JsBlock.collectAdditionalLocalVariables() {
        accept(object : RecursiveJsVisitor() {
            override fun visit(x: JsVars.JsVar) {
                super.visit(x)
                localVariables += x.name
            }
        })
    }


    private fun ClassDescriptor.findFunction(name: String): FunctionDescriptor? {
        val functions = unsubstitutedMemberScope.getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
                .filter { it.name.asString() == name }
        return functions.mapNotNull { it as? FunctionDescriptor }.firstOrNull { it.kind.isReal }
    }

    private fun MutableList<JsStatement>.assignToField(fieldName: JsName, value: JsExpression) {
        this += JsAstUtils.assignment(JsNameRef(fieldName, JsLiteral.THIS), value).makeStmt()
    }

    private fun MutableList<JsStatement>.assignToPrototype(fieldName: JsName, value: JsExpression) {
        this += JsAstUtils.assignment(JsNameRef(fieldName, JsAstUtils.prototypeOf(className.makeRef())), value).makeStmt()
    }
}