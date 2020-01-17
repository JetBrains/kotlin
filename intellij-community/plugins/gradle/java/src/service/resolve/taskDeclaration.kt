// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.CommonClassNames.JAVA_LANG_STRING
import com.intellij.psi.PsiType
import groovy.lang.Closure
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.*
import org.jetbrains.plugins.groovy.lang.GroovyElementFilter
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrBinaryExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.impl.signatures.GrClosureSignatureUtil.findCall
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createType
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.methodCall
import org.jetbrains.plugins.groovy.lang.psi.patterns.psiMethod
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil.unwrapClassType
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.GrDelegatesToProvider
import org.jetbrains.plugins.groovy.lang.typing.GrTypeCalculator

class GradleTaskDeclarationElementFilter : GroovyElementFilter {

  override fun isFake(element: GroovyPsiElement): Boolean {
    return isFakeInner(element) && element.containingFile.isGradleScript()
  }
}

class GradleTaskDeclarationTypeCalculator : GrTypeCalculator<GrReferenceExpression> {

  override fun getType(expression: GrReferenceExpression): PsiType? = when {
    isTaskIdExpression(expression) -> createType(JAVA_LANG_STRING, expression.containingFile)
    isTaskIdInOperator(expression) -> createType(GRADLE_API_TASK, expression.containingFile)
    else -> null
  }
}

class GradleTaskDeclarationClosureDelegateProvider : GrDelegatesToProvider {

  companion object {
    private val projectTaskMethod = methodCall().resolvesTo(psiMethod(GRADLE_API_PROJECT, "task"))
    private val taskContainerCreateMethod = methodCall().resolvesTo(psiMethod(GRADLE_API_TASK_CONTAINER, "create"))
  }

  override fun getDelegatesToInfo(expression: GrFunctionalExpression): DelegatesToInfo? {
    val methodCall = findCall(expression) ?: return null
    val taskType = if (isTaskIdCall(methodCall) || projectTaskMethod.accepts(methodCall)) {
      getFromNamedArgument(methodCall)
      ?: createType(GRADLE_API_TASK, expression)
    }
    else if (taskContainerCreateMethod.accepts(methodCall)) {
      getFromNamedArgument(methodCall)
      ?: getFromExpressionArgument(methodCall)
      ?: createType(GRADLE_API_TASK, expression)
    }
    else {
      return null
    }
    return DelegatesToInfo(taskType, Closure.DELEGATE_FIRST)
  }

  private fun getFromNamedArgument(methodCall: GrMethodCall): PsiType? {
    val namedArgument = getNamedArgument(methodCall)
    return unwrapClassType(namedArgument?.expression?.type)
  }

  private fun getNamedArgument(methodCall: GrMethodCall): GrNamedArgument? {
    val namedArgument = methodCall.argumentList.findNamedArgument("type")
    if (namedArgument != null) {
      return namedArgument
    }
    val mapExpression = methodCall.expressionArguments.firstOrNull() as? GrListOrMap
    if (mapExpression == null || !mapExpression.isMap) {
      return null
    }
    return mapExpression.findNamedArgument("type")
  }

  private fun getFromExpressionArgument(methodCall: GrMethodCall): PsiType? {
    val type = methodCall.expressionArguments.getOrNull(1)?.type
    return unwrapClassType(type)
  }
}

private fun isFakeInner(element: GroovyPsiElement): Boolean {
  return when (element) {
    is GrMethodCall -> isFakeTopTaskCall(element) ||
                       isTaskIdCall(element)
    is GrReferenceExpression -> isTaskIdExpression(element) ||
                                isTaskIdInvoked(element) ||
                                isTaskIdInOperator(element)
    else -> false
  }
}

private fun isFakeTopTaskCall(methodCall: GrMethodCall): Boolean {
  if (!checkTaskCall(methodCall)) return false
  if (methodCall.namedArguments.isNotEmpty()) return false
  if (methodCall.hasClosureArguments()) return false
  val singleArgument = methodCall.expressionArguments.singleOrNull() ?: return false
  return isTaskIdCallDown(singleArgument) ||
         isTaskIdOperatorDown(singleArgument)
}

/**
 * Matches:
 * - `id()`  in `task id()`;
 * - `id {}` in `task id {}`;
 * - `id(namedArg: 42)` in `task id(namedArg: 42)`.
 * - `id(namedArg: 42) {}` in `task id(namedArg: 42) {}`.
 */
private fun isTaskIdCall(methodCall: GrMethodCall): Boolean {
  return isFirstArgumentOfTaskMethod(methodCall) &&
         isTaskIdCallDown(methodCall)
}

private fun isTaskIdCallDown(expression: GrExpression): Boolean {
  return expression is GrMethodCall &&
         hasTaskIdCallArguments(expression) &&
         expression.isDynamicVariableInvoked()
}

private fun hasTaskIdCallArguments(expression: GrMethodCall): Boolean {
  val expressions = expression.expressionArguments + expression.closureArguments
  return when (expressions.size) {
    0 -> true
    1 -> expressions[0] is GrClosableBlock
    2 -> {
      val (first, second) = expressions
      first is GrListOrMap &&
      first.isMap &&
      second is GrClosableBlock &&
      expression.namedArguments.isEmpty()
    }
    else -> false
  }
}

private fun isTaskIdOperatorDown(expression: GrExpression): Boolean {
  if (expression !is GrBinaryExpression) return false
  val left = expression.leftOperand
  return left is GrLiteral || left is GrReferenceExpression && !left.isQualified
}

/**
 * Matches:
 * - `task id`
 * - `task(id) {}`
 * - `task(id, namedArg: 42)`
 * - `task(id, namedArg: 42) {}`
 */
private fun isTopTaskCall(methodCall: GrMethodCall): Boolean {
  if (!checkTaskCall(methodCall)) {
    return false
  }
  val expressions = methodCall.expressionArguments
  val expressionsCount = expressions.size
  if (expressionsCount == 0) {
    return false
  }
  val argsCount = expressionsCount + methodCall.closureArguments.size + (if (methodCall.namedArguments.isEmpty()) 0 else 1)
  if (argsCount > 3) {
    return false
  }
  val taskId = expressions[0] as? GrReferenceExpression ?: return false
  return taskId.isDynamicVariable()
}

/**
 * Matches:
 * - `id` in `task id`
 * - `id` in `task(id) {}`
 * - `id` in `task(id, namedArg: 42)`
 * - `id` in `task(id, namedArg: 42) {}`
 */
private fun isTaskIdExpression(expression: GrReferenceExpression): Boolean {
  val methodCall = findMethodCallByFirstArgument(expression) ?: return false
  return isTopTaskCall(methodCall)
}

/**
 * Matches:
 * - `id` in `task id()`
 * - `id` in `task id {}`
 * - `id` in `task id(namedArg: 42)`
 * - `id` in `task id(namedArg: 42) {}`
 */
private fun isTaskIdInvoked(expression: GrReferenceExpression): Boolean {
  val methodCall = expression.parent as? GrMethodCall ?: return false
  return methodCall.invokedExpression == expression && isTaskIdCall(methodCall)
}

/**
 * Matches `id` in:           `task id <op> right`
 * which gets transformed to: `task("id") <op> right`
 */
private fun isTaskIdInOperator(expression: GrReferenceExpression): Boolean {
  if (expression.isQualified) return false
  val binary = expression.parent as? GrBinaryExpression ?: return false
  if (expression != binary.leftOperand) return false
  return isFirstArgumentOfTaskMethod(binary)
}

private fun isFirstArgumentOfTaskMethod(expression: GrExpression): Boolean {
  val methodCall = findMethodCallByFirstArgument(expression) ?: return false
  return checkTaskCall(methodCall)
}

private fun findMethodCallByFirstArgument(expression: GrExpression): GrMethodCall? {
  val argumentList = expression.parent as? GrArgumentList ?: return null
  if (expression != argumentList.expressionArguments[0]) return null
  return argumentList.parent as? GrMethodCall
}

private fun checkTaskCall(methodCall: GrMethodCall): Boolean {
  val invokedExpression = methodCall.invokedExpression as? GrReferenceExpression ?: return false
  return invokedExpression.referenceName == "task" && !invokedExpression.isQualified
}

private fun GrMethodCall.isDynamicVariableInvoked(): Boolean {
  val invoked = invokedExpression
  return invoked is GrReferenceExpression && invoked.isDynamicVariable()
}

private fun GrReferenceExpression.isDynamicVariable(): Boolean {
  return !isQualified && staticReference.resolve() == null
}
