/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k

import org.jetbrains.jet.j2k.ast.Type
import org.jetbrains.jet.j2k.ast.Nullability
import com.intellij.psi.*
import org.jetbrains.jet.j2k.visitors.TypeVisitor
import java.util.HashMap
import java.util.HashSet
import org.jetbrains.jet.j2k.ast.Import
import org.jetbrains.jet.j2k.ast.ImportList
import org.jetbrains.jet.j2k.ast.assignPrototype
import com.intellij.psi.CommonClassNames.JAVA_LANG_OBJECT
import org.jetbrains.jet.j2k.ast.assignNoPrototype
import org.jetbrains.jet.j2k.ast.ErrorType
import com.intellij.codeInsight.NullableNotNullManager
import org.jetbrains.jet.j2k.ast.ArrayType
import org.jetbrains.jet.j2k.ast.ClassType
import org.jetbrains.jet.j2k.ast.ReferenceElement
import org.jetbrains.jet.j2k.ast.Identifier

class TypeConverter(val converter: Converter) {
    private val nullabilityCache = HashMap<PsiElement, Nullability>()

    public fun convertType(`type`: PsiType?, nullability: Nullability = Nullability.Default): Type {
        if (`type` == null) return ErrorType().assignNoPrototype()

        val result = `type`.accept<Type>(TypeVisitor(converter))!!.assignNoPrototype()
        return when (nullability) {
            Nullability.NotNull -> result.toNotNullType()
            Nullability.Nullable -> result.toNullableType()
            Nullability.Default -> result
        }
    }

    public fun convertTypes(types: Array<PsiType>): List<Type>
            = types.map { convertType(it) }

    public fun convertVariableType(variable: PsiVariable): Type {
        val result = if (variable.isMainMethodParameter()) {
            ArrayType(ClassType(ReferenceElement(Identifier("String").assignNoPrototype(), listOf()).assignNoPrototype(), Nullability.NotNull, converter.settings).assignNoPrototype(),
                      Nullability.NotNull,
                      converter.settings).assignNoPrototype()
        }
        else {
            convertType(variable.getType(), variableNullability(variable))
        }
        return result.assignPrototype(variable.getTypeElement())
    }

    public fun variableNullability(variable: PsiVariable): Nullability {
        val cached = nullabilityCache[variable]
        if (cached != null) return cached
        val value = variableNullabilityNoCache(variable)
        nullabilityCache[variable] = value
        return value
    }

    private fun variableNullabilityNoCache(variable: PsiVariable): Nullability {
        if (variable is PsiEnumConstant) return Nullability.NotNull
        val variableType = variable.getType()
        if (variableType is PsiPrimitiveType) return Nullability.NotNull

        var nullability = variable.nullabilityFromAnnotations()

        if (nullability == Nullability.Default && variable is PsiParameter) {
            val scope = variable.getDeclarationScope()
            if (scope is PsiMethod) {
                val paramIndex = scope.getParameterList().getParameters().indexOf(variable)
                assert(paramIndex >= 0)
                val superSignatures = scope.getHierarchicalMethodSignature().getSuperSignatures()
                nullability = superSignatures.map { signature ->
                    val params = signature.getMethod().getParameterList().getParameters()
                    if (paramIndex < params.size) variableNullability(params[paramIndex]) else Nullability.Default
                }.firstOrNull { it != Nullability.Default } ?: Nullability.Default
            }
        }

        if (nullability == Nullability.Default) {
            val initializer = variable.getInitializer()
            if (initializer != null) {
                val initializerNullability = initializer.nullability()
                if (variable.isEffectivelyFinal()) {
                    nullability = initializerNullability
                }
                else if (initializerNullability == Nullability.Nullable) { // if variable is not final then non-nullability of initializer does not mean that variable is non-null
                    nullability = Nullability.Nullable
                }
            }
        }

        // variables of types like Integer are most likely nullable
        if (nullability == Nullability.Default && variableType.getCanonicalText() in boxingTypes) {
            return Nullability.Nullable
        }

        if (nullability == Nullability.Default && variable.isMainMethodParameter() ) {
            return Nullability.NotNull
        }

        if (!converter.conversionScope.contains(variable)) { // do not analyze usages out of our conversion scope
            if (variable is PsiParameter) {
                // Object.equals corresponds to Any.equals which has nullable parameter:
                val scope = variable.getDeclarationScope()
                if (scope is PsiMethod && scope.getName() == "equals" && scope.getContainingClass()?.getQualifiedName() == JAVA_LANG_OBJECT) {
                    return Nullability.Nullable
                }
            }

            return nullability
        }

        if (nullability == Nullability.Default) {
            if (variable is PsiField && variable.hasModifierProperty(PsiModifier.PRIVATE) && shouldGenerateDefaultInitializer(variable)) {
                return Nullability.Nullable
            }
        }

        if (nullability == Nullability.Default) {
            val scope = searchScope(variable)
            if (scope != null) {
                if (findVariableUsages(variable, scope).any { isNullableFromUsage(it) }) {
                    nullability = Nullability.Nullable
                }
            }
        }

        if (nullability == Nullability.Default && variable is PsiParameter) {
            val method = variable.getDeclarationScope() as? PsiMethod
            if (method != null) {
                val scope = searchScope(method)
                if (scope != null) {
                    val parameters = method.getParameterList().getParameters()
                    val parameterIndex = parameters.indexOf(variable)
                    for (call in findMethodCalls(method, scope)) {
                        val args = call.getArgumentList().getExpressions()
                        if (args.size == parameters.size) {
                            if (args[parameterIndex].nullability() == Nullability.Nullable) {
                                nullability = Nullability.Nullable
                                break
                            }
                        }
                    }
                }
            }
        }

        return nullability
    }

    private fun PsiVariable.isMainMethodParameter() = this is PsiParameter && (getDeclarationScope() as? PsiMethod)?.isMainMethod() ?: false

    public fun convertMethodReturnType(method: PsiMethod): Type
            = convertType(method.getReturnType(), methodNullability(method)).assignPrototype(method.getReturnTypeElement())

    public fun methodNullability(method: PsiMethod): Nullability {
        val cached = nullabilityCache[method]
        if (cached != null) return cached
        val value = methodNullabilityNoCache(method)
        nullabilityCache[method] = value
        return value
    }

    private fun methodNullabilityNoCache(method: PsiMethod): Nullability {
        val returnType = method.getReturnType()
        if (returnType is PsiPrimitiveType) return Nullability.NotNull

        var nullability = method.nullabilityFromAnnotations()

        if (nullability == Nullability.Default) {
            val superSignatures = method.getHierarchicalMethodSignature().getSuperSignatures()
            nullability = superSignatures.map { methodNullability(it.getMethod()) }.firstOrNull { it != Nullability.Default } ?: Nullability.Default
        }

        // methods of types like Integer are most likely nullable
        if (nullability == Nullability.Default && returnType?.getCanonicalText() in boxingTypes) {
            return Nullability.Nullable
        }

        if (!converter.conversionScope.contains(method)) return nullability // do not analyze body and usages of methods out of our conversion scope

        if (nullability == Nullability.Default) {
            method.getBody()?.accept(object: JavaRecursiveElementVisitor() {
                override fun visitReturnStatement(statement: PsiReturnStatement) {
                    if (statement.getReturnValue()?.nullability() == Nullability.Nullable) {
                        nullability = Nullability.Nullable
                    }
                }

                override fun visitMethod(method: PsiMethod) {
                    // do not go inside any other method (e.g. in anonymous class)
                }
            })
        }

        if (nullability == Nullability.Default) {
            val scope = searchScope(method)
            if (scope != null) {
                if (findMethodCalls(method, scope).any { isNullableFromUsage(it) }) {
                    nullability = Nullability.Nullable
                }
            }
        }

        return nullability
    }

    private fun searchScope(element: PsiElement): PsiElement? {
        return when(element) {
            is PsiParameter -> element.getDeclarationScope()
            is PsiField -> if (element.hasModifierProperty(PsiModifier.PRIVATE)) element.getContainingClass() else element.getContainingFile()
            is PsiMethod -> if (element.hasModifierProperty(PsiModifier.PRIVATE)) element.getContainingClass() else element.getContainingFile()
            is PsiLocalVariable -> element.getContainingMethod()
            else -> null
        }
    }

    private fun PsiExpression.nullability(): Nullability {
        return when (this) {
            is PsiLiteralExpression -> if (getType() != PsiType.NULL) Nullability.NotNull else Nullability.Nullable

            is PsiNewExpression -> Nullability.NotNull

            is PsiConditionalExpression -> {
                val nullability1 = getThenExpression()?.nullability()
                if (nullability1 == Nullability.Nullable) return Nullability.Nullable
                val nullability2 = getElseExpression()?.nullability()
                if (nullability2 == Nullability.Nullable) return Nullability.Nullable
                if (nullability1 == Nullability.NotNull && nullability2 == Nullability.NotNull) return Nullability.NotNull
                Nullability.Default
            }

            is PsiParenthesizedExpression -> getExpression()?.nullability() ?: Nullability.Default


        //TODO: some other cases

            else -> Nullability.Default
        }
    }

    private fun isNullableFromUsage(usage: PsiExpression): Boolean {
        val parent = usage.getParent() ?: return false
        if (parent is PsiAssignmentExpression && parent.getOperationTokenType() == JavaTokenType.EQ && usage == parent.getLExpression()) {
            return parent.getRExpression()?.nullability() == Nullability.Nullable
        }
        else if (parent is PsiBinaryExpression) {
            val operationType = parent.getOperationTokenType()
            if (operationType == JavaTokenType.EQEQ || operationType == JavaTokenType.NE) {
                val otherOperand = if (usage == parent.getLOperand()) parent.getROperand() else parent.getLOperand()
                return otherOperand is PsiLiteralExpression && otherOperand.getType() == PsiType.NULL
            }
        }
        else if (parent is PsiVariable && usage == parent.getInitializer() && parent.isEffectivelyFinal()) {
            return variableNullability(parent) == Nullability.Nullable
        }
        return false
    }

    private fun PsiVariable.isEffectivelyFinal(): Boolean {
        if (hasModifierProperty(PsiModifier.FINAL)) return true
        return when(this) {
            is PsiLocalVariable -> !hasWriteAccesses(getContainingMethod())
            is PsiField -> if (hasModifierProperty(PsiModifier.PRIVATE)) !hasWriteAccesses(getContainingClass()) else false
            else -> false
        }
    }

    private fun PsiModifierListOwner.nullabilityFromAnnotations(): Nullability {
        val manager = NullableNotNullManager.getInstance(getProject())
        return if (manager.isNotNull(this, false/* we do not check bases because they are checked by callers of this method*/))
            Nullability.NotNull
        else if (manager.isNullable(this, false))
            Nullability.Nullable
        else
            Nullability.Default
    }

    class object {
        private val boxingTypes: Set<String> = setOf(
                CommonClassNames.JAVA_LANG_BYTE,
                CommonClassNames.JAVA_LANG_CHARACTER,
                CommonClassNames.JAVA_LANG_DOUBLE,
                CommonClassNames.JAVA_LANG_FLOAT,
                CommonClassNames.JAVA_LANG_INTEGER,
                CommonClassNames.JAVA_LANG_LONG,
                CommonClassNames.JAVA_LANG_SHORT,
                CommonClassNames.JAVA_LANG_BOOLEAN
        )
    }
}
