// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.gradle.service.resolve

import com.intellij.psi.CommonClassNames.JAVA_LANG_STRING
import com.intellij.psi.PsiType
import org.jetbrains.plugins.groovy.lang.GroovyExpressionFilter
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil.createType
import org.jetbrains.plugins.groovy.lang.typing.GrTypeCalculator

class GradleTaskIdExpressionFilter : GroovyExpressionFilter {

  override fun isFake(expression: GrExpression): Boolean {
    return expression is GrReferenceExpression && isTaskIdExpression(expression)
  }
}

class GradleTaskIdTypeCalculator : GrTypeCalculator<GrReferenceExpression> {

  override fun getType(expression: GrReferenceExpression): PsiType? {
    return if (isTaskIdExpression(expression)) createType(JAVA_LANG_STRING, expression.containingFile) else null
  }
}

private fun isTaskIdExpression(expression: GrReferenceExpression): Boolean {
  if (expression.isQualified) return false
  val argumentList = expression.parent as? GrArgumentList ?: return false
  if (expression != argumentList.expressionArguments[0]) return false
  val methodCall = argumentList.parent as? GrMethodCall ?: return false
  val invokedExpression = methodCall.invokedExpression as? GrReferenceExpression ?: return false
  if (invokedExpression.referenceName != "task") return false
  if (invokedExpression.isQualified) return false
  return expression.containingFile.isGradleScript() && expression.staticReference.resolve() == null
}
