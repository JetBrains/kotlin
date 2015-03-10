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

import org.jetbrains.kotlin.j2k.ast.Type
import org.jetbrains.kotlin.j2k.ast.Nullability
import com.intellij.psi.*
import org.jetbrains.kotlin.j2k.ast.assignPrototype
import com.intellij.psi.CommonClassNames.JAVA_LANG_OBJECT
import org.jetbrains.kotlin.j2k.ast.assignNoPrototype
import org.jetbrains.kotlin.j2k.ast.ErrorType
import com.intellij.codeInsight.NullableNotNullManager
import org.jetbrains.kotlin.j2k.ast.ArrayType
import org.jetbrains.kotlin.j2k.ast.ClassType
import org.jetbrains.kotlin.j2k.ast.ReferenceElement
import org.jetbrains.kotlin.j2k.ast.Identifier
import java.util.HashMap
import org.jetbrains.kotlin.j2k.ast.Mutability
import java.util.HashSet
import org.jetbrains.kotlin.asJava.KotlinLightElement
import org.jetbrains.kotlin.psi.JetCallableDeclaration
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.descriptors.CallableDescriptor

class TypeConverter(val converter: Converter) {
    public fun convertType(type: PsiType?, nullability: Nullability = Nullability.Default, mutability: Mutability = Mutability.Default): Type {
        if (type == null) return ErrorType().assignNoPrototype()

        val result = type.accept<Type>(TypeVisitor(converter, type, mutability))!!.assignNoPrototype()
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
            convertType(variable.getType(), variableNullability(variable), variableMutability(variable))
        }
        return result.assignPrototype(variable.getTypeElement())
    }

    public fun convertMethodReturnType(method: PsiMethod): Type
            = convertType(method.getReturnType(), methodNullability(method), methodMutability(method)).assignPrototype(method.getReturnTypeElement())

    public fun variableNullability(variable: PsiVariable): Nullability
            = nullabilityFlavor.forVariableType(variable)

    public fun methodNullability(method: PsiMethod): Nullability
            = nullabilityFlavor.forMethodReturnType(method)

    public fun variableMutability(variable: PsiVariable): Mutability
            = mutabilityFlavor.forVariableType(variable)

    public fun methodMutability(method: PsiMethod): Mutability
            = mutabilityFlavor.forMethodReturnType(method)

    private fun PsiVariable.isMainMethodParameter() = this is PsiParameter && (getDeclarationScope() as? PsiMethod)?.isMainMethod() ?: false

    private fun searchScope(element: PsiElement): PsiElement? {
        return when(element) {
            is PsiParameter -> element.getDeclarationScope()
            is PsiField -> if (element.hasModifierProperty(PsiModifier.PRIVATE)) element.getContainingClass() else element.getContainingFile()
            is PsiMethod -> if (element.hasModifierProperty(PsiModifier.PRIVATE)) element.getContainingClass() else element.getContainingFile()
            is PsiLocalVariable -> element.getContainingMethod()
            else -> null
        }
    }

    private fun PsiVariable.isEffectivelyFinal(): Boolean {
        if (hasModifierProperty(PsiModifier.FINAL)) return true
        return when(this) {
            is PsiLocalVariable -> !hasWriteAccesses(converter.referenceSearcher, getContainingMethod())
            is PsiField -> if (hasModifierProperty(PsiModifier.PRIVATE)) !hasWriteAccesses(converter.referenceSearcher, getContainingClass()) else false
            else -> false
        }
    }

    private abstract inner class TypeFlavor<T>(val default: T) {
        private val cache = HashMap<PsiElement, T>()
        private val typesBeingCalculated = HashSet<PsiElement>()

        open val forEnumConstant: T = default
        open fun fromType(type: PsiType): T = default
        open fun fromAnnotations(owner: PsiModifierListOwner): T = default
        open fun fromTypeHeuristics(type: PsiType): T = default
        open fun forVariableTypeBeforeUsageSearch(variable: PsiVariable): T = default
        abstract fun fromUsage(usage: PsiExpression): T
        open fun forVariableTypeAfterUsageSearch(variable: PsiVariable): T = default
        open fun fromMethodBody(body: PsiCodeBlock): T = default

        fun forVariableType(variable: PsiVariable): T {
            val cached = cache[variable]
            if (cached != null) return cached
            val value = withRecursionPrevention(variable) { forVariableTypeNoCache(variable) }
            cache[variable] = value
            return value
        }

        private fun forVariableTypeNoCache(variable: PsiVariable): T {
            if (variable is PsiEnumConstant) return forEnumConstant

            val variableType = variable.getType()
            var value = fromType(variableType)
            if (value != default) return value

            value = fromAnnotations(variable)
            if (value != default) return value

            if (variable is PsiParameter) {
                val scope = variable.getDeclarationScope()
                if (scope is PsiMethod) {
                    val paramIndex = scope.getParameterList().getParameters().indexOf(variable)
                    assert(paramIndex >= 0)
                    val superSignatures = scope.getHierarchicalMethodSignature().getSuperSignatures()
                    value = superSignatures.map { signature ->
                        val params = signature.getMethod().getParameterList().getParameters()
                        if (paramIndex < params.size()) forVariableType(params[paramIndex]) else default
                    }.firstOrNull { it != default } ?: default
                    if (value != default) return value
                }
            }

            value = forVariableTypeBeforeUsageSearch(variable)
            if (value != default) return value

            value = fromTypeHeuristics(variableType)
            if (value != default) return value

            if (!converter.conversionScope.contains(variable)) return default // do not analyze usages of variables not in our conversion scope

            val scope = searchScope(variable)
            if (scope != null) {
                value = converter.referenceSearcher.findVariableUsages(variable, scope).map { fromUsage(it) }.firstOrNull { it != default } ?: default
                if (value != default) return value
            }

            value = forVariableTypeAfterUsageSearch(variable)
            if (value != default) return value

            return default
        }

        fun forMethodReturnType(method: PsiMethod): T {
            val cached = cache[method]
            if (cached != null) return cached
            val value = withRecursionPrevention(method) { forMethodReturnTypeNoCache(method) }
            cache[method] = value
            return value
        }

        private fun forMethodReturnTypeNoCache(method: PsiMethod): T {
            val returnType = method.getReturnType() ?: return default

            var value = fromType(returnType)
            if (value != default) return value

            value = fromAnnotations(method)
            if (value != default) return value

            val superSignatures = method.getHierarchicalMethodSignature().getSuperSignatures()
            value = superSignatures.map { forMethodReturnType(it.getMethod()) }.firstOrNull { it != default } ?: default
            if (value != default) return value

            value = fromTypeHeuristics(returnType)
            if (value != default) return value

            if (!converter.conversionScope.contains(method)) return default // do not analyze body and usages of methods out of our conversion scope

            val body = method.getBody()
            if (body != null) {
                value = fromMethodBody(body)
                if (value != default) return value
            }

            val scope = searchScope(method)
            if (scope != null) {
                value = converter.referenceSearcher.findMethodCalls(method, scope).map { fromUsage(it) }.firstOrNull { it != default } ?: default
                if (value != default) return value
            }

            return default
        }

        private fun withRecursionPrevention(element: PsiElement, calculator: () -> T): T {
            if (element in typesBeingCalculated) return default
            typesBeingCalculated.add(element)
            try {
                return calculator()
            }
            finally {
                typesBeingCalculated.remove(element)
            }
        }
    }

    private val nullabilityFlavor = object : TypeFlavor<Nullability>(Nullability.Default) {
        override val forEnumConstant: Nullability
            get() = Nullability.NotNull

        override fun fromType(type: PsiType) = if (type is PsiPrimitiveType) Nullability.NotNull else Nullability.Default

        override fun fromAnnotations(owner: PsiModifierListOwner): Nullability {
            val manager = NullableNotNullManager.getInstance(owner.getProject())
            return if (manager.isNotNull(owner, false/* we do not check bases because they are checked by callers of this method*/))
                Nullability.NotNull
            else if (manager.isNullable(owner, false))
                Nullability.Nullable
            else
                Nullability.Default
        }

        override fun forVariableTypeBeforeUsageSearch(variable: PsiVariable): Nullability {
            val initializer = variable.getInitializer()
            if (initializer != null) {
                val initializerNullability = initializer.nullability()
                if (initializerNullability != Nullability.Default) {
                    if (variable.isEffectivelyFinal()) {
                        return initializerNullability
                    }
                    else if (initializerNullability == Nullability.Nullable) { // if variable is not final then non-nullability of initializer does not mean that variable is non-null
                        return Nullability.Nullable
                    }
                }
            }

            if (variable.isMainMethodParameter() ) {
                return Nullability.NotNull
            }

            if (variable is PsiParameter) {
                // Object.equals corresponds to Any.equals which has nullable parameter:
                val scope = variable.getDeclarationScope()
                if (scope is PsiMethod && scope.getName() == "equals" && scope.getContainingClass()?.getQualifiedName() == JAVA_LANG_OBJECT) {
                    return Nullability.Nullable
                }
            }

            if (variable is PsiField
                    && variable.hasModifierProperty(PsiModifier.PRIVATE)
                    && converter.conversionScope.contains(variable)
                    && shouldGenerateDefaultInitializer(converter.referenceSearcher, variable)) {
                return Nullability.Nullable
            }

            return Nullability.Default
        }

        // variables of types like Integer are most likely nullable
        override fun fromTypeHeuristics(type: PsiType) = if (type.getCanonicalText() in boxingTypes) Nullability.Nullable else Nullability.Default

        override fun fromUsage(usage: PsiExpression): Nullability {
            return if (isNullableFromUsage(usage)) Nullability.Nullable else Nullability.Default
        }

        private fun isNullableFromUsage(usage: PsiExpression): Boolean {
            val parent = usage.getParent()
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

        override fun forVariableTypeAfterUsageSearch(variable: PsiVariable): Nullability {
            if (variable is PsiParameter) {
                val method = variable.getDeclarationScope() as? PsiMethod
                if (method != null) {
                    val scope = searchScope(method)
                    if (scope != null) {
                        val parameters = method.getParameterList().getParameters()
                        val parameterIndex = parameters.indexOf(variable)
                        for (call in converter.referenceSearcher.findMethodCalls(method, scope)) {
                            val args = call.getArgumentList().getExpressions()
                            if (args.size() == parameters.size()) {
                                if (args[parameterIndex].nullability() == Nullability.Nullable) {
                                    return Nullability.Nullable
                                }
                            }
                        }
                    }
                }
            }
            return Nullability.Default
        }

        override fun fromMethodBody(body: PsiCodeBlock): Nullability {
            var isNullable = false
            body.accept(object: JavaRecursiveElementVisitor() {
                override fun visitReturnStatement(statement: PsiReturnStatement) {
                    if (statement.getReturnValue()?.nullability() == Nullability.Nullable) {
                        isNullable = true
                    }
                }

                override fun visitMethod(method: PsiMethod) {
                    // do not go inside any other method (e.g. in anonymous class)
                }
            })
            return if (isNullable) Nullability.Nullable else Nullability.Default
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
    }

    private val mutabilityFlavor = object : TypeFlavor<Mutability>(Mutability.Default) {
        override val forEnumConstant: Mutability get() = Mutability.NonMutable

        override fun fromType(type: PsiType): Mutability {
            val target = (type as? PsiClassType)?.resolve() ?: return Mutability.NonMutable
            if (target.getQualifiedName() !in TypeVisitor.toKotlinMutableTypesMap.keySet()) return Mutability.NonMutable
            return Mutability.Default
        }

        override fun fromAnnotations(owner: PsiModifierListOwner): Mutability {
            if (owner is KotlinLightElement<*, *>) {
                val jetDeclaration = owner.getOrigin() as? JetCallableDeclaration ?: return Mutability.Default
                val descriptor = converter.resolverForConverter.resolveToDescriptor(jetDeclaration) as? CallableDescriptor ?: return Mutability.Default
                val type = descriptor.getReturnType() ?: return Mutability.Default
                val classDescriptor = TypeUtils.getClassDescriptor(type) ?: return Mutability.Default
                return if (DescriptorUtils.getFqName(classDescriptor).asString() in mutableKotlinClasses)
                    Mutability.Mutable
                else
                    Mutability.NonMutable
            }
            //TODO: Kotlin compiled elements

            return super<TypeFlavor>.fromAnnotations(owner) //TODO: ReadOnly annotation
        }

        override fun fromUsage(usage: PsiExpression)
                = if (isMutableFromUsage(usage)) Mutability.Mutable else Mutability.Default

        private fun isMutableFromUsage(usage: PsiExpression): Boolean {
            val parent = usage.getParent()
            if (parent is PsiReferenceExpression && usage == parent.getQualifierExpression() && parent.getParent() is PsiMethodCallExpression) {
                return parent.getReferenceName() in modificationMethodNames
            }
            else if (parent is PsiExpressionList) {
                val call = parent.getParent() as? PsiCall ?: return false
                val method = call.resolveMethod() ?: return false
                val paramIndex = parent.getExpressions().indexOf(usage)
                val parameterList = method.getParameterList()
                if (paramIndex >= parameterList.getParametersCount()) return false
                return variableMutability(parameterList.getParameters()[paramIndex]) == Mutability.Mutable
            }
            else if (parent is PsiVariable && usage == parent.getInitializer()) {
                return variableMutability(parent) == Mutability.Mutable
            }
            else if (parent is PsiAssignmentExpression && parent.getOperationTokenType() == JavaTokenType.EQ && usage == parent.getRExpression()) {
                val leftSideVar = (parent.getLExpression() as? PsiReferenceExpression)?.resolve() as? PsiVariable ?: return false
                return variableMutability(leftSideVar) == Mutability.Mutable
            }
            return false
        }
    }

    default object {
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

        private val modificationMethodNames = setOf(
                "add", "remove", "set", "addAll", "removeAll", "retainAll", "clear", "put", "putAll"
        )

        private val mutableKotlinClasses = TypeVisitor.toKotlinMutableTypesMap.values().toSet()
    }
}
