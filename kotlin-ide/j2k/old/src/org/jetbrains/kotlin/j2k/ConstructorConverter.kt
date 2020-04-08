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

package org.jetbrains.kotlin.j2k

import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import org.jetbrains.kotlin.j2k.ast.*
import java.util.*

class ConstructorConverter(
        private val psiClass: PsiClass,
        private val converter: Converter,
        private val fieldToPropertyInfo: (PsiField) -> PropertyInfo,
        private val overloadReducer: OverloadReducer
) {
    private val constructors = psiClass.constructors.asList()

    private val toTargetConstructorMap = buildToTargetConstructorMap()

    private val primaryConstructor: PsiMethod? = when (constructors.size) {
        0 -> null
        1 -> constructors.single()
        else -> choosePrimaryConstructor()
    }

    private fun choosePrimaryConstructor(): PsiMethod? {
        val candidates = constructors.filter { it !in toTargetConstructorMap }
        if (candidates.size != 1) return null // there should be only one constructor which does not call other constructor
        val primary = candidates.single()
        if (toTargetConstructorMap.values.any() { it != primary }) return null // all other constructors call our candidate (directly or indirectly)
        return primary
    }

    private fun buildToTargetConstructorMap(): Map<PsiMethod, PsiMethod> {
        val toTargetConstructorMap = HashMap<PsiMethod, PsiMethod>()
        for (constructor in constructors) {
            val firstStatement = constructor.body?.statements?.firstOrNull()
            val methodCall = (firstStatement as? PsiExpressionStatement)?.expression as? PsiMethodCallExpression
            if (methodCall != null) {
                val refExpr = methodCall.methodExpression
                if (refExpr.canonicalText == "this") {
                    val target = refExpr.resolve() as? PsiMethod
                    if (target != null && target.isConstructor) {
                        val finalTarget = toTargetConstructorMap[target] ?: target
                        toTargetConstructorMap[constructor] = finalTarget
                        for (entry in toTargetConstructorMap.entries) {
                            if (entry.value == constructor) {
                                entry.setValue(finalTarget)
                            }
                        }
                    }
                }
            }
        }
        return toTargetConstructorMap
    }

    var baseClassParams: List<DeferredElement<Expression>>? = if (constructors.isEmpty()) emptyList() else null
        private set

    fun convertConstructor(constructor: PsiMethod,
                                  annotations: Annotations,
                                  modifiers: Modifiers,
                                  fieldsToDrop: MutableSet<PsiField>,
                                  postProcessBody: (Block) -> Block): Constructor? {
        return if (constructor == primaryConstructor) {
            convertPrimaryConstructor(annotations, modifiers, fieldsToDrop, postProcessBody)
        }
        else {
            if (overloadReducer.shouldDropMethod(constructor)) return null

            val params = converter.convertParameterList(constructor, overloadReducer)

            val thisOrSuper = findThisOrSuperCall(constructor)
            val thisOrSuperDeferred = if (thisOrSuper != null)
                converter.deferredElement { it.convertExpression(thisOrSuper.expression) }
            else
                null

            fun convertBody(codeConverter: CodeConverter): Block {
                return postProcessBody(codeConverter.withSpecialExpressionConverter(object: SpecialExpressionConverter {
                    override fun convertExpression(expression: PsiExpression, codeConverter: CodeConverter): Expression? {
                        if (expression.isThisConstructorCall() || expression.isSuperConstructorCall()) {
                            return Expression.Empty // skip it
                        }
                        return null
                    }
                }).convertBlock(constructor.body))
            }

            SecondaryConstructor(annotations, modifiers, params, converter.deferredElement(::convertBody), thisOrSuperDeferred)
        }
    }

    private fun findThisOrSuperCall(constructor: PsiMethod): PsiExpressionStatement? {
        val statement = constructor.body?.statements?.firstOrNull() as? PsiExpressionStatement ?: return null
        val methodCall = statement.expression as? PsiMethodCallExpression ?: return null
        val text = methodCall.methodExpression.text
        return if (text == "this" || text == "super") statement else null
    }

    private fun convertPrimaryConstructor(annotations: Annotations,
                                          modifiers: Modifiers,
                                          fieldsToDrop: MutableSet<PsiField>,
                                          postProcessBody: (Block) -> Block): PrimaryConstructor {
        val params = primaryConstructor!!.parameterList.parameters
        val parameterToField = HashMap<PsiParameter, Pair<PsiField, Type>>()
        val body = primaryConstructor.body

        val parameterUsageReplacementMap = HashMap<String, String>()

        val bodyGenerator: (CodeConverter) -> Block = if (body != null) {
            val statementsToRemove = HashSet<PsiStatement>()
            for (parameter in params) {
                val (field, initializationStatement) = findBackingFieldForConstructorParameter(parameter, primaryConstructor) ?: continue

                val fieldType = converter.typeConverter.convertVariableType(field)
                val parameterType = converter.typeConverter.convertVariableType(parameter)
                // types can be different only in nullability
                val type = if (fieldType == parameterType) {
                    fieldType
                }
                else if (fieldType.toNotNullType() == parameterType.toNotNullType()) {
                    if (fieldType.isNullable) fieldType else parameterType // prefer nullable one
                }
                else {
                    continue
                }

                val propertyInfo = fieldToPropertyInfo(field)
                if (propertyInfo.needExplicitGetter || propertyInfo.needExplicitSetter) continue

                parameterToField.put(parameter, field to type)
                statementsToRemove.add(initializationStatement)
                fieldsToDrop.add(field)

                val fieldName = propertyInfo.name
                if (fieldName != parameter.name) {
                    parameterUsageReplacementMap.put(parameter.name!!, fieldName)
                }
            }

            { codeConverter ->
                val bodyConverter = codeConverter.withSpecialExpressionConverter(
                        object : ReplacingExpressionConverter(parameterUsageReplacementMap) {
                            override fun convertExpression(expression: PsiExpression, codeConverter: CodeConverter): Expression? {
                                if (expression.isSuperConstructorCall()) {
                                    return Expression.Empty // skip it
                                }
                                return super.convertExpression(expression, codeConverter)
                            }
                        })
                postProcessBody(bodyConverter.convertBlock(body, false, { !statementsToRemove.contains(it) }))
            }
        }
        else {
            { Block.Empty }
        }

        // we need to replace renamed parameter usages in base class constructor arguments and in default values

        fun CodeConverter.correct() = withSpecialExpressionConverter(ReplacingExpressionConverter(parameterUsageReplacementMap))

        val statement = primaryConstructor.body?.statements?.firstOrNull()
        val methodCall = (statement as? PsiExpressionStatement)?.expression as? PsiMethodCallExpression
        baseClassParams = if (methodCall != null && methodCall.isSuperConstructorCall()) {
            methodCall.argumentList.expressions.map {
                converter.deferredElement { codeConverter -> codeConverter.correct().convertExpression(it) }
            }
        }
        else {
            emptyList()
        }

        val parameterList = converter.convertParameterList(
                primaryConstructor,
                overloadReducer,
                { parameter, default ->
                    if (!parameterToField.containsKey(parameter)) {
                        converter.convertParameter(parameter, defaultValue = default)
                    }
                    else {
                        val (field, type) = parameterToField[parameter]!!
                        val propertyInfo = fieldToPropertyInfo(field)

                        var paramAnnotations = converter.convertAnnotations(parameter, AnnotationUseTarget.Param) +
                                               converter.convertAnnotations(field, AnnotationUseTarget.Field)
                        if (propertyInfo.getMethod != null) {
                            paramAnnotations += converter.convertAnnotations(propertyInfo.getMethod, AnnotationUseTarget.Get)
                        }
                        if (propertyInfo.setMethod != null) {
                            paramAnnotations += converter.convertAnnotations(propertyInfo.setMethod, AnnotationUseTarget.Set)
                        }
                        FunctionParameter(
                                propertyInfo.identifier,
                                type,
                                if (propertyInfo.isVar) FunctionParameter.VarValModifier.Var else FunctionParameter.VarValModifier.Val,
                                paramAnnotations,
                                propertyInfo.modifiers,
                                default
                        ).assignPrototypes(
                                PrototypeInfo(parameter, CommentsAndSpacesInheritance.LINE_BREAKS),
                                PrototypeInfo(field, CommentsAndSpacesInheritance.NO_SPACES)
                        )
                    }
                },
                correctCodeConverter = { correct() })

        return PrimaryConstructor(annotations, modifiers, parameterList, converter.deferredElement(bodyGenerator)).assignPrototype(primaryConstructor)
    }

    private fun findBackingFieldForConstructorParameter(parameter: PsiParameter, constructor: PsiMethod): Pair<PsiField, PsiStatement>? {
        val body = constructor.body ?: return null

        val refs = converter.referenceSearcher.findVariableUsages(parameter, body)

        if (refs.any { PsiUtil.isAccessedForWriting(it) }) return null

        for (ref in refs) {
            val assignment = ref.parent as? PsiAssignmentExpression ?: continue
            if (assignment.operationSign.tokenType != JavaTokenType.EQ) continue
            val assignee = assignment.lExpression as? PsiReferenceExpression ?: continue
            if (!assignee.isQualifierEmptyOrThis()) continue
            val field = assignee.resolve() as? PsiField ?: continue
            if (field.containingClass != constructor.containingClass) continue
            if (field.hasModifierProperty(PsiModifier.STATIC)) continue
            if (field.initializer != null) continue

            // assignment should be a top-level statement
            val statement = assignment.parent as? PsiExpressionStatement ?: continue
            if (statement.parent != body) continue

            // and no other assignments to field should exist in the constructor
            if (converter.referenceSearcher.findVariableUsages(field, body).any { it != assignee && PsiUtil.isAccessedForWriting(it) && it.isQualifierEmptyOrThis() }) continue
            //TODO: check access to field before assignment

            return field to statement
        }

        return null
    }

    private fun PsiExpression.isSuperConstructorCall() = (this as? PsiMethodCallExpression)?.methodExpression?.text == "super"
    private fun PsiExpression.isThisConstructorCall() = (this as? PsiMethodCallExpression)?.methodExpression?.text == "this"

    private inner open class ReplacingExpressionConverter(val parameterUsageReplacementMap: Map<String, String>) : SpecialExpressionConverter {
        override fun convertExpression(expression: PsiExpression, codeConverter: CodeConverter): Expression? {
            if (expression is PsiReferenceExpression && expression.qualifier == null) {
                val replacement = parameterUsageReplacementMap[expression.referenceName]
                if (replacement != null) {
                    val target = expression.resolve()
                    if (target is PsiParameter) {
                        val scope = target.declarationScope
                        // we do not check for exactly this constructor because default values reference parameters in other constructors
                        if (scope is PsiMember && scope.isConstructor() && scope.parent == psiClass) {
                            return Identifier(replacement, codeConverter.typeConverter.variableNullability(target).isNullable(codeConverter.settings))
                        }
                    }
                }
            }

            return null
        }
    }
}
