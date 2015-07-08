/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.j2k.usageProcessing

import com.intellij.psi.*
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.j2k.AccessorKind
import org.jetbrains.kotlin.j2k.CodeConverter
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.psi.JetPsiFactory
import org.jetbrains.kotlin.psi.JetSimpleNameExpression

class FieldToPropertyProcessing(val field: PsiField, val propertyName: String, val isNullable: Boolean) : UsageProcessing {
    override val targetElement: PsiElement get() = field

    override val convertedCodeProcessor = if (field.getName() != propertyName) MyConvertedCodeProcessor() else null

    override var javaCodeProcessor = if (field.hasModifierProperty(PsiModifier.PRIVATE))
        null
     else if (!field.hasModifierProperty(PsiModifier.STATIC))
        UseAccessorsJavaCodeProcessor()
    else if (field.getName() != propertyName)
        ElementRenamedCodeProcessor(propertyName)
    else
        null

    override val kotlinCodeProcessor = if (field.getName() != propertyName) ElementRenamedCodeProcessor(propertyName) else null

    private inner class MyConvertedCodeProcessor : ConvertedCodeProcessor {
        override fun convertVariableUsage(expression: PsiReferenceExpression, codeConverter: CodeConverter): Expression? {
            val identifier = Identifier(propertyName, isNullable).assignNoPrototype()

            val qualifier = expression.getQualifierExpression()
            if (qualifier != null) {
                return QualifiedExpression(codeConverter.convertExpression(qualifier), identifier)
            }
            else {
                // check if field name is shadowed
                val elementFactory = PsiElementFactory.SERVICE.getInstance(expression.getProject())
                val refExpr = try {
                    elementFactory.createExpressionFromText(propertyName, expression) as? PsiReferenceExpression ?: return identifier
                }
                catch(e: IncorrectOperationException) {
                    return identifier
                }
                return if (refExpr.resolve() == null)
                    identifier
                else
                    QualifiedExpression(ThisExpression(Identifier.Empty).assignNoPrototype(), identifier) //TODO: this is not correct in case of nested/anonymous classes
            }
        }
    }

    private inner class UseAccessorsJavaCodeProcessor : ExternalCodeProcessor {
        private val factory = PsiElementFactory.SERVICE.getInstance(field.getProject())

        override fun processUsage(reference: PsiReference): Collection<PsiReference>? {
            val refExpr = reference.getElement() as? PsiReferenceExpression ?: return null
            val qualifier = refExpr.getQualifierExpression()

            val parent = refExpr.getParent()
            when (parent) {
                is PsiAssignmentExpression -> {
                    if (refExpr == parent.getLExpression()) {
                        if (parent.getOperationTokenType() == JavaTokenType.EQ) {
                            val callExpr = parent.replace(generateSetterCall(qualifier, parent.getRExpression() ?: return null)) as PsiMethodCallExpression
                            return listOf(callExpr.getMethodExpression())
                        }
                        else {
                            val assignmentOpText = parent.getOperationSign().getText()
                            assert(assignmentOpText.endsWith("="))
                            val opText = assignmentOpText.substring(0, assignmentOpText.length() - 1)
                            return parent.replaceWithModificationCalls(qualifier, opText, parent.getRExpression() ?: return null)
                        }
                    }
                }

                is PsiPrefixExpression, is PsiPostfixExpression -> {
                    //TODO: what if it's used as value?
                    val operationType = if (parent is PsiPrefixExpression)
                        parent.getOperationTokenType()
                    else
                        (parent as PsiPostfixExpression).getOperationTokenType()
                    val opText = when (operationType) {
                        JavaTokenType.PLUSPLUS -> "+"
                        JavaTokenType.MINUSMINUS -> "-"
                        else -> null
                    }
                    if (opText != null) {
                        return (parent as PsiExpression).replaceWithModificationCalls(qualifier, opText, factory.createExpressionFromText("1", null))
                    }
                }
            }

            val callExpr = refExpr.replace(generateGetterCall(qualifier)) as PsiMethodCallExpression
            return listOf(callExpr.getMethodExpression())
        }

        //TODO: what if qualifier has side effects?
        private fun PsiExpression.replaceWithModificationCalls(qualifier: PsiExpression?, op: String, value: PsiExpression): Collection<PsiReference> {
            var getCall = generateGetterCall(qualifier)

            var binary = factory.createExpressionFromText("x $op y", null) as PsiBinaryExpression
            binary.getLOperand().replace(getCall)
            binary.getROperand()!!.replace(value)

            var setCall = generateSetterCall(qualifier, binary) as PsiMethodCallExpression
            setCall = this.replace(setCall) as PsiMethodCallExpression

            binary = setCall.getArgumentList().getExpressions().single() as PsiBinaryExpression
            getCall = binary.getLOperand() as PsiMethodCallExpression

            return listOf(getCall.getMethodExpression().getReference()!!, setCall.getMethodExpression().getReference()!!)
        }

        private fun generateGetterCall(qualifier: PsiExpression?): PsiMethodCallExpression {
            val text = accessorName(AccessorKind.GETTER) + "()"
            val expressionText = if (qualifier != null)
                "${qualifier.getText()}.$text"
            else
                text
            return factory.createExpressionFromText(expressionText, null) as PsiMethodCallExpression
        }

        private fun generateSetterCall(qualifier: PsiExpression?, value: PsiExpression): PsiExpression {
            val text = accessorName(AccessorKind.SETTER) + "(" + value.getText() + ")"
            val expressionText = if (qualifier != null)
                "${qualifier.getText()}.$text"
            else
                text
            return factory.createExpressionFromText(expressionText, null)
        }
    }

    private fun accessorName(kind: AccessorKind)
            = (if (kind == AccessorKind.GETTER) "get" else "set") + propertyName.capitalize()
}
