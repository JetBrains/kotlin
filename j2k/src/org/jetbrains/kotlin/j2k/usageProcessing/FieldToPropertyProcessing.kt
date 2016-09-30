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
import com.intellij.psi.util.PsiUtil
import com.intellij.util.IncorrectOperationException
import org.jetbrains.kotlin.j2k.AccessorKind
import org.jetbrains.kotlin.j2k.CodeConverter
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.j2k.dot
import org.jetbrains.kotlin.utils.addToStdlib.singletonList

class FieldToPropertyProcessing(
        private val field: PsiField,
        private val propertyName: String,
        private val isNullable: Boolean,
        private val replaceReadWithFieldReference: Boolean,
        private val replaceWriteWithFieldReference: Boolean
) : UsageProcessing {
    override val targetElement: PsiElement get() = this.field

    override val convertedCodeProcessor: ConvertedCodeProcessor? =
            if (field.name != propertyName || replaceReadWithFieldReference || replaceWriteWithFieldReference) MyConvertedCodeProcessor() else null

    override var javaCodeProcessors =
            if (field.hasModifierProperty(PsiModifier.PRIVATE))
                emptyList()
            else if (field.name != propertyName)
                listOf(ElementRenamedCodeProcessor(propertyName), UseAccessorsJavaCodeProcessor())
            else
                UseAccessorsJavaCodeProcessor().singletonList()

    override val kotlinCodeProcessors =
            if (field.name != propertyName)
                ElementRenamedCodeProcessor(propertyName).singletonList()
            else
                emptyList()

    private inner class MyConvertedCodeProcessor : ConvertedCodeProcessor {
        override fun convertVariableUsage(expression: PsiReferenceExpression, codeConverter: CodeConverter): Expression? {
            val useFieldReference = replaceReadWithFieldReference && PsiUtil.isAccessedForReading(expression)
                                    || replaceWriteWithFieldReference && PsiUtil.isAccessedForWriting(expression)

            //TODO: what if local "field" is declared? Should be rare case though
            val identifier = Identifier.withNoPrototype(if (useFieldReference) "field" else propertyName, isNullable)

            val qualifier = expression.qualifierExpression
            if (qualifier != null && !useFieldReference) {
                return QualifiedExpression(codeConverter.convertExpression(qualifier), identifier, expression.dot())
            }
            else {
                // check if field name is shadowed
                val elementFactory = PsiElementFactory.SERVICE.getInstance(expression.project)
                val refExpr = try {
                    elementFactory.createExpressionFromText(identifier.name, expression) as? PsiReferenceExpression ?: return identifier
                }
                catch(e: IncorrectOperationException) {
                    return identifier
                }
                return if (refExpr.resolve() == null)
                    identifier
                else
                    QualifiedExpression(ThisExpression(Identifier.Empty).assignNoPrototype(), identifier, null) //TODO: this is not correct in case of nested/anonymous classes
            }
        }
    }

    private inner class UseAccessorsJavaCodeProcessor : ExternalCodeProcessor {
        private val factory = PsiElementFactory.SERVICE.getInstance(field.project)

        override fun processUsage(reference: PsiReference): Array<PsiReference>? {
            val refExpr = reference.element as? PsiReferenceExpression ?: return null
            val qualifier = refExpr.qualifierExpression

            val parent = refExpr.parent
            when (parent) {
                is PsiAssignmentExpression -> {
                    if (refExpr == parent.lExpression) {
                        if (parent.operationTokenType == JavaTokenType.EQ) {
                            val callExpr = parent.replace(generateSetterCall(qualifier, parent.rExpression ?: return null)) as PsiMethodCallExpression
                            return arrayOf(callExpr.methodExpression)
                        }
                        else {
                            val assignmentOpText = parent.operationSign.text
                            assert(assignmentOpText.endsWith("="))
                            val opText = assignmentOpText.substring(0, assignmentOpText.length - 1)
                            return parent.replaceWithModificationCalls(qualifier, opText, parent.rExpression ?: return null)
                        }
                    }
                }

                is PsiPrefixExpression, is PsiPostfixExpression -> {
                    //TODO: what if it's used as value?
                    val operationType = if (parent is PsiPrefixExpression)
                        parent.operationTokenType
                    else
                        (parent as PsiPostfixExpression).operationTokenType
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
            return arrayOf(callExpr.methodExpression)
        }

        //TODO: what if qualifier has side effects?
        private fun PsiExpression.replaceWithModificationCalls(qualifier: PsiExpression?, op: String, value: PsiExpression): Array<PsiReference> {
            var getCall = generateGetterCall(qualifier)

            var binary = factory.createExpressionFromText("x $op y", null) as PsiBinaryExpression
            binary.lOperand.replace(getCall)
            binary.rOperand!!.replace(value)

            var setCall = generateSetterCall(qualifier, binary) as PsiMethodCallExpression
            setCall = this.replace(setCall) as PsiMethodCallExpression

            binary = setCall.argumentList.expressions.single() as PsiBinaryExpression
            getCall = binary.lOperand as PsiMethodCallExpression

            return arrayOf(getCall.methodExpression, setCall.methodExpression)
        }

        private fun generateGetterCall(qualifier: PsiExpression?): PsiMethodCallExpression {
            val text = accessorName(AccessorKind.GETTER) + "()"
            val expressionText = if (qualifier != null)
                "${qualifier.text}.$text"
            else
                text
            return factory.createExpressionFromText(expressionText, null) as PsiMethodCallExpression
        }

        private fun generateSetterCall(qualifier: PsiExpression?, value: PsiExpression): PsiExpression {
            val text = accessorName(AccessorKind.SETTER) + "(" + value.text + ")"
            val expressionText = if (qualifier != null)
                "${qualifier.text}.$text"
            else
                text
            return factory.createExpressionFromText(expressionText, null)
        }
    }

    private fun accessorName(kind: AccessorKind)
            = (if (kind == AccessorKind.GETTER) "get" else "set") + propertyName.capitalize()
}
