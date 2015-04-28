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
import java.util.ArrayList
import java.util.HashMap
import java.util.HashSet

class OverloadReducer(
        private val methods: Collection<PsiMethod>,
        private val isOpenClass: Boolean,
        private val referenceSearcher: ReferenceSearcher
) {
    private val methodToLastParameterDefaults = HashMap<PsiMethod, ArrayList<PsiExpression>>()
    private val methodsToDrop = HashSet<PsiMethod>()

    init {
        val map = buildOverloadEquivalenceMap()
        dropOverloadsForDefaultValues(map)
    }

    public fun shouldDropMethod(method: PsiMethod): Boolean = method in methodsToDrop

    public fun parameterDefault(method: PsiMethod, parameterIndex: Int): PsiExpression? {
        val defaults = methodToLastParameterDefaults[method] ?: return null
        val index = method.getParameterList().getParametersCount() - parameterIndex - 1
        return if (index < defaults.size()) defaults[index] else null
    }

    private class EquivalentOverloadInfo(
            /**
             * Target method (one which is finally invoked by this one)
             */
            val method: PsiMethod,
            /**
             * This method is equivalent to the target method with a few last parameters having default values
             */
            val parameterDefaults: List<PsiExpression>)

    private fun buildOverloadEquivalenceMap(): Map<PsiMethod, EquivalentOverloadInfo> {
        val overloadGroups = methods
                .groupBy { listOf(if (it.isConstructor()) null else it.getName(),
                                  it.accessModifier(),
                                  it.getReturnType(),
                                  it.hasModifierProperty(PsiModifier.STATIC),
                                  getAnnotationsFingerprint(it)) }
                .values()
                .filter { it.size() > 1 }
                .map { it.filterNot { shouldSkipOverload(it) } }
                .filter { it.size() > 1 }

        val map = HashMap<PsiMethod, EquivalentOverloadInfo>()
        for (group in overloadGroups) {
            for (method in group) {
                val overloadInfo = findEquivalentOverload(method, group) ?: continue

                val furtherOverloadInfo = map[overloadInfo.method]
                val resultOverloadInfo = if (furtherOverloadInfo != null) {
                    EquivalentOverloadInfo(furtherOverloadInfo.method, overloadInfo.parameterDefaults + furtherOverloadInfo.parameterDefaults)
                }
                else {
                    overloadInfo
                }

                map[method] = resultOverloadInfo
                for (entry in map.entrySet()) {
                    if (entry.value.method == method) {
                        val newParameterDefaults = entry.value.parameterDefaults + resultOverloadInfo.parameterDefaults
                        entry.setValue(EquivalentOverloadInfo(resultOverloadInfo.method, newParameterDefaults))
                    }
                }

            }
        }

        return map
    }

    private fun shouldSkipOverload(method: PsiMethod): Boolean {
        if (method.isConstructor()) return false
        if (method.hasModifierProperty(PsiModifier.ABSTRACT)) return true
        if (method.hasModifierProperty(PsiModifier.NATIVE)) return true
        if (method.hasModifierProperty(PsiModifier.SYNCHRONIZED)) return true
        if (method.getHierarchicalMethodSignature().getSuperSignatures().isNotEmpty()) return true
        if (isOpenClass && referenceSearcher.hasOverrides(method)) return true
        return false
    }

    private fun findEquivalentOverload(method: PsiMethod, overloads: Collection<PsiMethod>): EquivalentOverloadInfo? {
        val statement = method.getBody()?.getStatements()?.singleOrNull() ?: return null

        val methodCall = when (statement) {
            is PsiExpressionStatement -> statement.getExpression()
            is PsiReturnStatement -> statement.getReturnValue()
            else -> null
        } as? PsiMethodCallExpression ?: return null

        val expectedMethodName = if (method.isConstructor()) "this" else method.getName()
        val refExpr = methodCall.getMethodExpression()
        if (refExpr.isQualified() || refExpr.getReferenceName() != expectedMethodName) return null

        val target = refExpr.resolve() as? PsiMethod ?: return null
        if (target !in overloads) return null

        val parameterDefaults = calcTargetParameterDefaults(method, target, methodCall) ?: return null
        return EquivalentOverloadInfo(target, parameterDefaults)
    }

    private fun calcTargetParameterDefaults(method: PsiMethod, target: PsiMethod, targetCall: PsiMethodCallExpression): List<PsiExpression>? {
        val parameters = method.getParameterList().getParameters()
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

    private fun dropOverloadsForDefaultValues(equivalenceMap: Map<PsiMethod, EquivalentOverloadInfo>) {
        val dropCandidates = equivalenceMap
                .map { it.key }
                .sortBy { -it.getParameterList().getParametersCount() } // we will try to drop them starting from ones with more parameters

        DropCandidatesLoop@
        for (method in dropCandidates) {
            val paramCount = method.getParameterList().getParametersCount()
            val targetInfo = equivalenceMap[method]!!
            val targetParamCount = targetInfo.method.getParameterList().getParametersCount()
            assert(paramCount < targetParamCount)
            val defaults = targetInfo.parameterDefaults
            assert(defaults.size() == targetParamCount - paramCount)

            val targetDefaults = methodToLastParameterDefaults.getOrPut(targetInfo.method, { ArrayList() })

            for (i in defaults.indices) {
                val default = defaults[defaults.size() - i - 1]
                if (i < targetDefaults.size()) { // default for this parameter has already been assigned
                    if (targetDefaults[i].getText() != default.getText()) continue@DropCandidatesLoop
                }
                else {
                    targetDefaults.add(default)
                }
            }

            methodsToDrop.add(method)
        }
    }

    private fun getAnnotationsFingerprint(method: PsiMethod): Any {
        return method.getModifierList().getAnnotations().map { it.getText() }
    }
}

public fun Converter.convertParameterList(
        method: PsiMethod,
        overloadReducer: OverloadReducer?,
        convertParameter: (parameter: PsiParameter, default: DeferredElement<Expression>?) -> Parameter = { parameter, default -> convertParameter(parameter, defaultValue = default) },
        correctCodeConverter: CodeConverter.() -> CodeConverter = { this }
): ParameterList {
    val parameterList = method.getParameterList()
    val params = parameterList.getParameters()
    return ParameterList(params.indices.map { i ->
        val parameter = params[i]
        val defaultValue = overloadReducer?.parameterDefault(method, i)
        val defaultValueConverted = if (defaultValue != null)
            deferredElement { codeConverter -> codeConverter.correctCodeConverter().convertExpression(defaultValue, parameter.getType()) }
        else
            null
        convertParameter(parameter, defaultValueConverted)
    }).assignPrototype(parameterList)
}
