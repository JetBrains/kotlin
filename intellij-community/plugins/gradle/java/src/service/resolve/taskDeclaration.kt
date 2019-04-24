// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.CommonClassNames.JAVA_LANG_STRING
import com.intellij.psi.PsiType
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_TASK
import org.jetbrains.plugins.groovy.lang.GroovyExpressionFilter
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrBinaryExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createType
import org.jetbrains.plugins.groovy.lang.typing.GrTypeCalculator

class GradleTaskDeclarationExpressionFilter : GroovyExpressionFilter {

  override fun isFake(expression: GrExpression): Boolean {
    return isFakeInner(expression) && expression.containingFile.isGradleScript()
  }
}

class GradleTaskDeclarationTypeCalculator : GrTypeCalculator<GrReferenceExpression> {

  override fun getType(expression: GrReferenceExpression): PsiType? = when {
    isTaskIdExpression(expression) -> createType(JAVA_LANG_STRING, expression.containingFile)
    isTaskIdInOperator(expression) -> createType(GRADLE_API_TASK, expression.containingFile)
    else -> null
  }
}

private fun isFakeInner(expression: GrExpression): Boolean {
  return when (expression) {
    is GrMethodCall -> isFakeTopTaskCall(expression) ||
                       isTaskIdCall(expression)
    is GrReferenceExpression -> isTaskIdExpression(expression) ||
                                isTaskIdInvoked(expression) ||
                                isTaskIdInOperator(expression)
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
  val expressions = expression.expressionArguments
  return when (expressions.size) {
    0 -> expression.closureArguments.size in 0..1
    1 -> expressions[0] is GrClosableBlock && !expression.hasClosureArguments()
    else -> false
  }
}

private fun isTaskIdOperatorDown(expression: GrExpression): Boolean {
  if (expression !is GrBinaryExpression) return false
  val left = expression.leftOperand
  return left is GrLiteral || left is GrReferenceExpression && !left.isQualified
}

/**
 * Matches `id` in:           `task id`
 * which gets transformed to: `task("id")`
 */
private fun isTaskIdExpression(expression: GrReferenceExpression): Boolean {
  val argumentList = expression.parent as? GrArgumentList ?: return false
  val methodCall = argumentList.parent as? GrMethodCall ?: return false
  val expressions = argumentList.expressionArguments
  if (expressions.isEmpty()) {
    return false
  }
  if (expression !== expressions[0]) {  // isFirstArgumentOfTaskMethod
    return false
  }
  val argsCount = expressions.size + methodCall.closureArguments.size + (if (argumentList.namedArguments.isEmpty()) 0 else 1)
  if (argsCount > 3) {
    return false
  }
  return checkTaskCall(methodCall) &&
         expression.isDynamicVariable()
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
  val argumentList = expression.parent as? GrArgumentList ?: return false
  if (expression != argumentList.expressionArguments[0]) return false
  val methodCall = argumentList.parent as? GrMethodCall ?: return false
  return checkTaskCall(methodCall)
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
