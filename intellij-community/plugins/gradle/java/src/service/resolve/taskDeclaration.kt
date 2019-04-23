// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.CommonClassNames.JAVA_LANG_STRING
import com.intellij.psi.PsiType
import org.jetbrains.plugins.gradle.service.resolve.GradleCommonClassNames.GRADLE_API_TASK
import org.jetbrains.plugins.groovy.lang.GroovyExpressionFilter
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrBinaryExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createType
import org.jetbrains.plugins.groovy.lang.typing.GrTypeCalculator

class GradleTaskDeclarationExpressionFilter : GroovyExpressionFilter {

  override fun isFake(expression: GrExpression): Boolean = when (expression) {
    is GrReferenceExpression -> isTaskIdExpression(expression) || isTaskIdExpressionInBinaryOp(expression)
    is GrMethodCall -> isFakeTaskCall(expression)
    else -> false
  }
}

class GradleTaskDeclarationTypeCalculator : GrTypeCalculator<GrReferenceExpression> {

  override fun getType(expression: GrReferenceExpression): PsiType? = when {
    isTaskIdExpression(expression) -> createType(JAVA_LANG_STRING, expression.containingFile)
    isTaskIdExpressionInBinaryOp(expression) -> createType(GRADLE_API_TASK, expression.containingFile)
    else -> null
  }
}

/**
 * Matches `id` in:           `task id`
 * which gets transformed to: `task("id")`
 */
private fun isTaskIdExpression(expression: GrReferenceExpression): Boolean {
  if (expression.isQualified) return false
  if (!isFirstArgumentOfTaskMethod(expression)) return false
  if (!expression.containingFile.isGradleScript()) return false
  if (expression.staticReference.resolve() != null) return false
  return true
}

/**
 * Matches `id` in:           `task id <op> right`
 * which gets transformed to: `task("id") <op> right`
 */
private fun isTaskIdExpressionInBinaryOp(expression: GrReferenceExpression): Boolean {
  if (expression.isQualified) return false
  val binary = expression.parent as? GrBinaryExpression ?: return false
  if (expression != binary.leftOperand) return false
  if (!isFirstArgumentOfTaskMethod(binary)) return false
  if (!expression.containingFile.isGradleScript()) return false
  return true
}

private fun isFirstArgumentOfTaskMethod(expression: GrExpression): Boolean {
  val argumentList = expression.parent as? GrArgumentList ?: return false
  if (expression != argumentList.expressionArguments[0]) return false
  val methodCall = argumentList.parent as? GrMethodCall ?: return false
  return isTaskCall(methodCall)
}

/**
 * Matches `task` in:         `task id <op> right`
 * which gets transformed to: `task("id") <op> right`
 */
private fun isFakeTaskCall(expression: GrMethodCall): Boolean {
  return isTaskCall(expression) && isCallWithSingleBinaryExpressionArgument(expression)
}

private fun isTaskCall(methodCall: GrMethodCall): Boolean {
  val invokedExpression = methodCall.invokedExpression as? GrReferenceExpression ?: return false
  return invokedExpression.referenceName == "task" && !invokedExpression.isQualified
}

private fun isCallWithSingleBinaryExpressionArgument(methodCall: GrMethodCall): Boolean =
  methodCall.namedArguments.isEmpty() &&
  methodCall.closureArguments.isEmpty() &&
  methodCall.expressionArguments.singleOrNull().let { argument ->
    argument is GrBinaryExpression && argument.leftOperand.let { left ->
      left is GrLiteral || left is GrReferenceExpression && !left.isQualified
    }
  }
