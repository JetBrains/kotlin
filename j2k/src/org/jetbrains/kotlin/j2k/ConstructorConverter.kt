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
import org.jetbrains.kotlin.j2k.ast.*
import com.intellij.psi.util.PsiUtil
import java.util.HashMap
import java.util.ArrayList
import java.util.HashSet

class ConstructorConverter(
        private val psiClass: PsiClass,
        private val converter: Converter,
        private val fieldCorrections: Map<PsiField, FieldCorrectionInfo>
) {
    private val constructors = psiClass.getConstructors()
    private val constructorsToDrop = HashSet<PsiMethod>()
    private val lastParamDefaults = ArrayList<PsiExpression>() // defaults for a few last parameters of primary constructor in reverse order
    private val primaryConstructor: PsiMethod? = when (constructors.size()) {
        0 -> null
        1 -> constructors.single()
        else -> choosePrimaryConstructor()
    }

    private class TargetConstructorInfo(
            /**
             * Target constructor (one which is finally invoked by this one)
             */
            val constructor: PsiMethod,
            /**
             * Is not null if this constructor is equivalent to the target constructor with a few last parameters having default values
             */
            val parameterDefaults: List<PsiExpression>?)

    private fun choosePrimaryConstructor(): PsiMethod? {
        val toTargetConstructorMap = buildToTargetConstructorMap()

        val candidates = constructors.filter { it !in toTargetConstructorMap }
        if (candidates.size() != 1) return null // there should be only one constructor which does not call other constructor
        val primary = candidates.single()
        if (toTargetConstructorMap.values().any() { it.constructor != primary }) return null // all other constructors call our candidate (directly or indirectly)

        dropConstructorsForDefaultValues(primary, toTargetConstructorMap)

        return primary
    }

    private fun buildToTargetConstructorMap(): Map<PsiMethod, TargetConstructorInfo> {
        val toTargetConstructorMap = HashMap<PsiMethod, TargetConstructorInfo>()
        for (constructor in constructors) {
            val firstStatement = constructor.getBody()?.getStatements()?.firstOrNull()
            val methodCall = (firstStatement as? PsiExpressionStatement)?.getExpression() as? PsiMethodCallExpression
            if (methodCall != null) {
                val refExpr = methodCall.getMethodExpression()
                if (refExpr.getCanonicalText() == "this") {
                    val target = refExpr.resolve() as? PsiMethod
                    if (target != null && target.isConstructor()) {
                        var parameterDefaults = calcTargetParameterDefaults(constructor, target, methodCall)

                        val finalTargetInfo = toTargetConstructorMap[target]
                        if (finalTargetInfo != null && parameterDefaults != null) {
                            parameterDefaults = if (finalTargetInfo.parameterDefaults != null)
                                parameterDefaults!! + finalTargetInfo.parameterDefaults
                            else
                                null
                        }
                        val finalTarget = finalTargetInfo?.constructor ?: target

                        toTargetConstructorMap[constructor] = TargetConstructorInfo(finalTarget, parameterDefaults)
                        for (entry in toTargetConstructorMap.entrySet()) {
                            if (entry.value.constructor == constructor) {
                                val newParameterDefaults = if (parameterDefaults != null)
                                    entry.value.parameterDefaults?.plus(parameterDefaults!!)
                                else
                                    null
                                entry.setValue(TargetConstructorInfo(finalTarget, newParameterDefaults))
                            }
                        }
                    }
                }
            }
        }
        return toTargetConstructorMap
    }

    private fun calcTargetParameterDefaults(constructor: PsiMethod, target: PsiMethod, targetCall: PsiMethodCallExpression): List<PsiExpression>? {
        if (constructor.getBody()!!.getStatements().size() != 1) return null // constructor's body should consist of only "this(...)"
        val parameters = constructor.getParameterList().getParameters()
        val targetParameters = target.getParameterList().getParameters()
        if (parameters.size() >= targetParameters.size()) return null
        val args = targetCall.getArgumentList().getExpressions()
        if (args.size() != targetParameters.size()) return null // incorrect code

        for (i in parameters.indices) {
            val parameter = parameters[i]
            val targetParameter = targetParameters[i]
            if (parameter.getName() != targetParameter.getName() || parameter.getType() != targetParameter.getType()) return null
            val arg = args[i]
            if (arg !is PsiReferenceExpression || arg.resolve() != parameter) return null
        }

        return args.drop(parameters.size())
    }

    private fun dropConstructorsForDefaultValues(primary: PsiMethod, toTargetConstructorMap: Map<PsiMethod, TargetConstructorInfo>) {
        val dropCandidates = toTargetConstructorMap
                .filter { it.value.parameterDefaults != null }
                .map { it.key }
                .filter { it.accessModifier() == primary.accessModifier() && it.getModifierList().getAnnotations().isEmpty() /* do not drop constructors with annotations */ }
                .sortBy { -it.getParameterList().getParametersCount() } // we will try to drop them starting from ones with more parameters
        val primaryParamCount = primary.getParameterList().getParametersCount()
        @DropCandidatesLoop
        for (constructor in dropCandidates) {
            val paramCount = constructor.getParameterList().getParametersCount()
            assert(paramCount < primaryParamCount)
            val defaults = toTargetConstructorMap[constructor]!!.parameterDefaults!!
            assert(defaults.size() == primaryParamCount - paramCount)

            for (i in defaults.indices) {
                val default = defaults[defaults.size() - i - 1]
                if (i < lastParamDefaults.size()) { // default for this parameter has already been assigned
                    if (lastParamDefaults[i].getText() != default.getText()) continue@DropCandidatesLoop
                }
                else {
                    lastParamDefaults.add(default)
                }
            }

            constructorsToDrop.add(constructor)
        }
    }

    public var baseClassParams: List<DeferredElement<Expression>> = listOf()
        private set

    public fun convertConstructor(constructor: PsiMethod,
                                  annotations: Annotations,
                                  modifiers: Modifiers,
                                  membersToRemove: MutableSet<PsiMember>,
                                  postProcessBody: (Block) -> Block): Member? {
        if (constructor == primaryConstructor) {
            return convertPrimaryConstructor(annotations, modifiers, membersToRemove, postProcessBody)
        }
        else {
            if (constructor in constructorsToDrop) return null

            val params = converter.convertParameterList(constructor.getParameterList())

            val thisOrSuper = findThisOrSuperCall(constructor)
            val thisOrSuperDeferred = if (thisOrSuper != null)
                converter.deferredElement { it.convertExpression(thisOrSuper.getExpression()) }
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
                }).convertBlock(constructor.getBody()))
            }

            return SecondaryConstructor(annotations, modifiers, params,
                                        converter.deferredElement(::convertBody), thisOrSuperDeferred)
        }
    }

    private fun findThisOrSuperCall(constructor: PsiMethod): PsiExpressionStatement? {
        val statement = constructor.getBody()?.getStatements()?.firstOrNull() as? PsiExpressionStatement ?: return null
        val methodCall = statement.getExpression() as? PsiMethodCallExpression ?: return null
        val text = methodCall.getMethodExpression().getText()
        return if (text == "this" || text == "super") statement else null
    }

    private fun convertPrimaryConstructor(annotations: Annotations,
                                          modifiers: Modifiers,
                                          membersToRemove: MutableSet<PsiMember>,
                                          postProcessBody: (Block) -> Block): PrimaryConstructor {
        val params = primaryConstructor!!.getParameterList().getParameters()
        val parameterToField = HashMap<PsiParameter, Pair<PsiField, Type>>()
        val body = primaryConstructor.getBody()

        val parameterUsageReplacementMap = HashMap<String, String>()
        val correctedTypeConverter = converter.withSpecialContext(psiClass).typeConverter /* to correct nested class references */

        val bodyGenerator: (CodeConverter) -> Block = if (body != null) {
            val statementsToRemove = HashSet<PsiStatement>()
            for (parameter in params) {
                val (field, initializationStatement) = findBackingFieldForConstructorParameter(parameter, primaryConstructor) ?: continue

                val fieldType = correctedTypeConverter.convertVariableType(field)
                val parameterType = correctedTypeConverter.convertVariableType(parameter)
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

                val fieldCorrection = fieldCorrections[field]
                // we cannot specify different setter access for constructor parameter
                if (fieldCorrection != null && !isVal(converter.referenceSearcher, field) && fieldCorrection.access != fieldCorrection.setterAccess) continue

                parameterToField.put(parameter, field to type)
                statementsToRemove.add(initializationStatement)
                membersToRemove.add(field)

                val fieldName = fieldCorrection?.name ?: field.getName()!!
                if (fieldName != parameter.getName()) {
                    parameterUsageReplacementMap.put(parameter.getName()!!, fieldName)
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
            { it -> Block.Empty }
        }

        // we need to replace renamed parameter usages in base class constructor arguments and in default values

        val correctedConverter = converter.withSpecialContext(psiClass) /* to correct nested class references */

        fun CodeConverter.correct() = withSpecialExpressionConverter(ReplacingExpressionConverter(parameterUsageReplacementMap))

        val statement = primaryConstructor.getBody()?.getStatements()?.firstOrNull()
        val methodCall = (statement as? PsiExpressionStatement)?.getExpression() as? PsiMethodCallExpression
        if (methodCall != null && methodCall.isSuperConstructorCall()) {
            baseClassParams = methodCall.getArgumentList().getExpressions().map {
                correctedConverter.deferredElement { codeConverter -> codeConverter.correct().convertExpression(it) }
            }
        }

        val parameterList = ParameterList(params.indices.map { i ->
            val parameter = params[i]
            val indexFromEnd = params.size() - i - 1
            val defaultValue = if (indexFromEnd < lastParamDefaults.size())
                correctedConverter.deferredElement { codeConverter -> codeConverter.correct().convertExpression(lastParamDefaults[indexFromEnd], parameter.getType()) }
            else
                null
            if (!parameterToField.containsKey(parameter)) {
                correctedConverter.convertParameter(parameter, defaultValue = defaultValue)
            }
            else {
                val (field, type) = parameterToField[parameter]!!
                val fieldCorrection = fieldCorrections[field]
                val name = fieldCorrection?.identifier ?: field.declarationIdentifier()
                val accessModifiers = if (fieldCorrection != null)
                    Modifiers(listOf()).with(fieldCorrection.access).assignNoPrototype()
                else
                    converter.convertModifiers(field).filter { it in ACCESS_MODIFIERS }
                Parameter(name,
                          type,
                          if (isVal(converter.referenceSearcher, field)) Parameter.VarValModifier.Val else Parameter.VarValModifier.Var,
                          converter.convertAnnotations(parameter) + converter.convertAnnotations(field),
                          accessModifiers,
                          defaultValue).assignPrototypes(listOf(parameter, field), CommentsAndSpacesInheritance(blankLinesBefore = false))
            }
        }).assignPrototype(primaryConstructor.getParameterList())
        return PrimaryConstructor(annotations, modifiers, parameterList, converter.deferredElement(bodyGenerator)).assignPrototype(primaryConstructor)
    }

    private fun findBackingFieldForConstructorParameter(parameter: PsiParameter, constructor: PsiMethod): Pair<PsiField, PsiStatement>? {
        val body = constructor.getBody() ?: return null

        val refs = converter.referenceSearcher.findVariableUsages(parameter, body)

        if (refs.any { PsiUtil.isAccessedForWriting(it) }) return null

        for (ref in refs) {
            val assignment = ref.getParent() as? PsiAssignmentExpression ?: continue
            if (assignment.getOperationSign().getTokenType() != JavaTokenType.EQ) continue
            val assignee = assignment.getLExpression() as? PsiReferenceExpression ?: continue
            if (!assignee.isQualifierEmptyOrThis()) continue
            val field = assignee.resolve() as? PsiField ?: continue
            if (field.getContainingClass() != constructor.getContainingClass()) continue
            if (field.hasModifierProperty(PsiModifier.STATIC)) continue
            if (field.getInitializer() != null) continue

            // assignment should be a top-level statement
            val statement = assignment.getParent() as? PsiExpressionStatement ?: continue
            if (statement.getParent() != body) continue

            // and no other assignments to field should exist in the constructor
            if (converter.referenceSearcher.findVariableUsages(field, body).any { it != assignee && PsiUtil.isAccessedForWriting(it) && it.isQualifierEmptyOrThis() }) continue
            //TODO: check access to field before assignment

            return field to statement
        }

        return null
    }

    private fun PsiExpression.isSuperConstructorCall() = (this as? PsiMethodCallExpression)?.getMethodExpression()?.getText() == "super"
    private fun PsiExpression.isThisConstructorCall() = (this as? PsiMethodCallExpression)?.getMethodExpression()?.getText() == "this"

    private inner open class ReplacingExpressionConverter(val parameterUsageReplacementMap: Map<String, String>) : SpecialExpressionConverter {
        override fun convertExpression(expression: PsiExpression, codeConverter: CodeConverter): Expression? {
            if (expression is PsiReferenceExpression && expression.getQualifier() == null) {
                val replacement = parameterUsageReplacementMap[expression.getReferenceName()]
                if (replacement != null) {
                    val target = expression.getReference()?.resolve()
                    if (target is PsiParameter) {
                        val scope = target.getDeclarationScope()
                        // we do not check for exactly this constructor because default values reference parameters in other constructors
                        if (scope.isConstructor() && scope.getParent() == psiClass) {
                            return Identifier(replacement, codeConverter.typeConverter.variableNullability(target).isNullable(codeConverter.settings))
                        }
                    }
                }
            }

            return null
        }
    }
}
