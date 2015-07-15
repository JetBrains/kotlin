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
import org.jetbrains.kotlin.j2k.AccessorKind
import org.jetbrains.kotlin.j2k.CodeConverter
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.psi.*

class AccessorToPropertyProcessing(val accessorMethod: PsiMethod, val accessorKind: AccessorKind, val propertyName: String) : UsageProcessing {
    override val targetElement: PsiElement get() = accessorMethod

    override val convertedCodeProcessor = object: ConvertedCodeProcessor {
        override fun convertMethodUsage(methodCall: PsiMethodCallExpression, codeConverter: CodeConverter): Expression? {
            val isNullable = codeConverter.typeConverter.methodNullability(accessorMethod).isNullable(codeConverter.settings)

            val methodExpr = methodCall.getMethodExpression()
            val arguments = methodCall.getArgumentList().getExpressions()

            val propertyName = Identifier(propertyName, isNullable).assignNoPrototype()
            val propertyAccess = QualifiedExpression(codeConverter.convertExpression(methodExpr.getQualifierExpression()), propertyName).assignNoPrototype()

            if (accessorKind == AccessorKind.GETTER) {
                if (arguments.size() != 0) return null // incorrect call
                return propertyAccess
            }
            else {
                if (arguments.size() != 1) return null // incorrect call
                val argument = codeConverter.convertExpression(arguments[0])
                return AssignmentExpression(propertyAccess, argument, "=")
            }
        }
    }

    override val javaCodeProcessor: ExternalCodeProcessor? = null

    override val kotlinCodeProcessor: ExternalCodeProcessor? = if (accessorMethod.hasModifierProperty(PsiModifier.PRIVATE))
        null
    else
        object : ExternalCodeProcessor {
            override fun processUsage(reference: PsiReference): Collection<PsiReference>? {
                val nameExpr = reference.getElement() as? JetSimpleNameExpression ?: return null
                val callExpr = nameExpr.getParent() as? JetCallExpression ?: return null

                val arguments = callExpr.getValueArguments()

                val factory = JetPsiFactory(nameExpr.getProject())
                var propertyNameExpr = factory.createSimpleName(propertyName)
                if (accessorKind == AccessorKind.GETTER) {
                    if (arguments.size() != 0) return null // incorrect call
                    propertyNameExpr = callExpr.replace(propertyNameExpr) as JetSimpleNameExpression
                    return listOf(propertyNameExpr.getReference()!!)
                }
                else {
                    val value = arguments.singleOrNull()?.getArgumentExpression() ?: return null
                    var assignment = factory.createExpression("a = b") as JetBinaryExpression
                    assignment.getRight()!!.replace(value)

                    val qualifiedExpression = callExpr.getParent() as? JetQualifiedExpression
                    if (qualifiedExpression != null && qualifiedExpression.getSelectorExpression() == callExpr) {
                        callExpr.replace(propertyNameExpr)
                        assignment.getLeft()!!.replace(qualifiedExpression)
                        assignment = qualifiedExpression.replace(assignment) as JetBinaryExpression
                        return listOf((assignment.getLeft() as JetQualifiedExpression).getSelectorExpression()!!.getReference()!!)
                    }
                    else {
                        assignment.getLeft()!!.replace(propertyNameExpr)
                        assignment = callExpr.replace(assignment) as JetBinaryExpression
                        return listOf(assignment.getLeft()!!.getReference()!!)
                    }
                }

            }
        }
}
