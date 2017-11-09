/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.codeInsight.NullableNotNullManager
import com.intellij.psi.*
import com.intellij.psi.CommonClassNames.JAVA_LANG_OBJECT
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.j2k.ast.*
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.types.TypeUtils
import java.util.*

interface JavaDataFlowAnalyzerFacade {

    fun variableNullability(variable: PsiVariable, context: PsiElement): Nullability

    fun methodNullability(method: PsiMethod): Nullability

    object Default : JavaDataFlowAnalyzerFacade {
        override fun methodNullability(method: PsiMethod): Nullability = Nullability.Default

        override fun variableNullability(variable: PsiVariable, context: PsiElement): Nullability = Nullability.Default
    }
}

class TypeConverter(val converter: Converter) {
    private val typesBeingConverted = HashSet<PsiType>()

    fun convertType(
            type: PsiType?,
            nullability: Nullability = Nullability.Default,
            mutability: Mutability = Mutability.Default,
            inAnnotationType: Boolean = false
    ): Type {
        if (type == null) return ErrorType().assignNoPrototype()

        if (!typesBeingConverted.add(type)) { // recursion in conversion
            return ErrorType().assignNoPrototype()
        }

        val result = type.accept<Type>(TypeVisitor(converter, type, mutability, inAnnotationType))!!.assignNoPrototype()

        typesBeingConverted.remove(type)

        return when (nullability) {
            Nullability.NotNull -> result.toNotNullType()
            Nullability.Nullable -> result.toNullableType()
            Nullability.Default -> result
        }
    }

    fun convertTypes(types: Array<PsiType>): List<Type>
            = types.map { convertType(it) }

    fun convertVariableType(variable: PsiVariable): Type {
        val result = if (variable.isMainMethodParameter()) {
            ArrayType(ClassType(ReferenceElement(Identifier.withNoPrototype("String"), listOf()).assignNoPrototype(), Nullability.NotNull, converter.settings).assignNoPrototype(),
                      Nullability.NotNull,
                      converter.settings).assignNoPrototype()
        }
        else {
            convertType(variable.type, variableNullability(variable), variableMutability(variable))
        }
        return result.assignPrototype(variable.typeElement, CommentsAndSpacesInheritance.NO_SPACES)
    }

    fun convertMethodReturnType(method: PsiMethod): Type
            = convertType(method.returnType, methodNullability(method), methodMutability(method)).assignPrototype(method.returnTypeElement)

    fun variableNullability(variable: PsiVariable): Nullability
            = nullabilityFlavor.forVariableType(variable, true)

    fun variableReferenceNullability(variable: PsiVariable, reference: PsiReferenceExpression): Nullability
            = nullabilityFlavor.forVariableReference(variable, reference)

    fun methodNullability(method: PsiMethod): Nullability
            = nullabilityFlavor.forMethodReturnType(method)

    fun variableMutability(variable: PsiVariable): Mutability
            = mutabilityFlavor.forVariableType(variable, true)

    fun methodMutability(method: PsiMethod): Mutability
            = mutabilityFlavor.forMethodReturnType(method)

    private fun PsiVariable.isMainMethodParameter() = this is PsiParameter && (declarationScope as? PsiMethod)?.isMainMethod() ?: false

    private fun searchScope(element: PsiElement): PsiElement? {
        return when(element) {
            is PsiParameter -> element.declarationScope
            is PsiField -> if (element.hasModifierProperty(PsiModifier.PRIVATE)) element.containingClass else element.containingFile
            is PsiMethod -> if (element.hasModifierProperty(PsiModifier.PRIVATE)) element.containingClass else element.containingFile
            is PsiLocalVariable -> element.getContainingMethod()
            else -> null
        }
    }

    private fun PsiVariable.isEffectivelyFinal(): Boolean {
        if (hasModifierProperty(PsiModifier.FINAL)) return true
        return when(this) {
            is PsiLocalVariable -> !hasWriteAccesses(converter.referenceSearcher, getContainingMethod())
            is PsiField -> if (hasModifierProperty(PsiModifier.PRIVATE)) !hasWriteAccesses(converter.referenceSearcher, containingClass) else false
            else -> false
        }
    }

    private abstract inner class TypeFlavor<out T>(val default: T) {
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

        fun forVariableType(variable: PsiVariable, checkScope: Boolean): T {
            val cached = cache[variable]
            if (cached != null) return cached
            val value = withRecursionPrevention(variable) { forVariableTypeNoCache(variable, checkScope) }
            cache[variable] = value
            return value
        }

        private fun forVariableTypeNoCache(variable: PsiVariable, checkScope: Boolean): T {
            if (variable is PsiEnumConstant) return forEnumConstant

            val variableType = variable.type
            var value = fromType(variableType)
            if (value != default) return value

            value = fromAnnotations(variable)
            if (value != default) return value

            if (checkScope && !converter.inConversionScope(variable)) {
                return value
            }

            if (variable is PsiParameter) {
                val scope = variable.declarationScope
                if (scope is PsiMethod) {
                    val paramIndex = scope.parameterList.parameters.indexOf(variable)
                    assert(paramIndex >= 0)
                    val superSignatures = scope.hierarchicalMethodSignature.superSignatures
                    value = superSignatures.map { signature ->
                        val params = signature.method.parameterList.parameters
                        if (paramIndex < params.size) forVariableType(params[paramIndex], false) else default
                    }.firstOrNull { it != default } ?: default
                    if (value != default) return value
                }
            }

            value = forVariableTypeBeforeUsageSearch(variable)
            if (value != default) return value

            value = fromTypeHeuristics(variableType)
            if (value != default) return value

            if (!converter.inConversionScope(variable)) return default // do not analyze usages of variables not in our conversion scope

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

        abstract fun fromDataFlowForMethod(method: PsiMethod): T

        private fun forMethodReturnTypeNoCache(method: PsiMethod): T {
            val returnType = method.returnType ?: return default

            var value = fromType(returnType)
            if (value != default) return value

            value = fromAnnotations(method)
            if (value != default) return value

            val superSignatures = method.hierarchicalMethodSignature.superSignatures
            value = superSignatures.map { forMethodReturnType(it.method) }.firstOrNull { it != default } ?: default
            if (value != default) return value

            value = fromTypeHeuristics(returnType)
            if (value != default) return value

            value = fromDataFlowForMethod(method)
            if (value != default) return value

            if (!converter.inConversionScope(method)) return default // do not analyze body and usages of methods out of our conversion scope

            val body = method.body
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
        fun forVariableReference(variable: PsiVariable, reference: PsiReferenceExpression): Nullability {
            assert(reference.resolve() == variable)
            val dataFlowUtil = converter.services.javaDataFlowAnalyzerFacade

            return dataFlowUtil.variableNullability(variable, reference).takeIf { it != default } ?:
                   variableNullability(variable)
        }

        override fun fromDataFlowForMethod(method: PsiMethod): Nullability =
                converter.services.javaDataFlowAnalyzerFacade.methodNullability(method)

        override val forEnumConstant: Nullability
            get() = Nullability.NotNull

        override fun fromType(type: PsiType) = if (type is PsiPrimitiveType) Nullability.NotNull else Nullability.Default

        override fun fromAnnotations(owner: PsiModifierListOwner): Nullability {
            val manager = NullableNotNullManager.getInstance(owner.project)
            return when {
                manager.isNotNull(owner, false/* we do not check bases because they are checked by callers of this method*/) ->
                    Nullability.NotNull
                manager.isNullable(owner, false) ->
                    Nullability.Nullable
                else ->
                    Nullability.Default
            }
        }

        override fun forVariableTypeBeforeUsageSearch(variable: PsiVariable): Nullability {
            val initializer = variable.initializer
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
            else if (variable is PsiField && !variable.hasWriteAccesses(converter.referenceSearcher, variable.containingClass)) {
                return Nullability.Nullable
            }

            if (variable.isMainMethodParameter() ) {
                return Nullability.NotNull
            }

            if (variable is PsiParameter) {
                // Object.equals corresponds to Any.equals which has nullable parameter:
                val scope = variable.declarationScope
                if (scope is PsiMethod && scope.name == "equals" && scope.containingClass?.qualifiedName == JAVA_LANG_OBJECT) {
                    return Nullability.Nullable
                }
            }

            if (variable is PsiField
                    && variable.hasModifierProperty(PsiModifier.PRIVATE)
                    && converter.inConversionScope(variable)
                    && shouldGenerateDefaultInitializer(converter.referenceSearcher, variable)) {
                return Nullability.Nullable
            }

            return Nullability.Default
        }

        // variables of types like Integer are most likely nullable
        override fun fromTypeHeuristics(type: PsiType) = if (type.canonicalText in boxingTypes) Nullability.Nullable else Nullability.Default

        override fun fromUsage(usage: PsiExpression): Nullability {
            return if (isNullableFromUsage(usage)) Nullability.Nullable else Nullability.Default
        }

        private fun isNullableFromUsage(usage: PsiExpression): Boolean {
            val parent = usage.parent
            if (parent is PsiAssignmentExpression && parent.operationTokenType == JavaTokenType.EQ && usage == parent.lExpression) {
                return parent.rExpression?.nullability() == Nullability.Nullable
            }
            else if (parent is PsiBinaryExpression) {
                val operationType = parent.operationTokenType
                if (operationType == JavaTokenType.EQEQ || operationType == JavaTokenType.NE) {
                    val otherOperand = if (usage == parent.lOperand) parent.rOperand else parent.lOperand
                    return otherOperand?.isNullLiteral() ?: false
                }
            }
            else if (parent is PsiVariable && usage == parent.initializer && parent.isEffectivelyFinal()) {
                return variableNullability(parent) == Nullability.Nullable
            }
            return false
        }

        override fun forVariableTypeAfterUsageSearch(variable: PsiVariable): Nullability {
            if (variable is PsiParameter) {
                val method = variable.declarationScope as? PsiMethod
                if (method != null) {
                    val scope = searchScope(method)
                    if (scope != null) {
                        val parameters = method.parameterList.parameters
                        val parameterIndex = parameters.indexOf(variable)
                        for (call in converter.referenceSearcher.findMethodCalls(method, scope)) {
                            val args = call.argumentList.expressions
                            if (args.size == parameters.size) {
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
                    if (statement.returnValue?.nullability() == Nullability.Nullable) {
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
                is PsiLiteralExpression -> if (type != PsiType.NULL) Nullability.NotNull else Nullability.Nullable

                is PsiNewExpression -> Nullability.NotNull

                is PsiConditionalExpression -> {
                    val nullability1 = thenExpression?.nullability()
                    if (nullability1 == Nullability.Nullable) return Nullability.Nullable
                    val nullability2 = elseExpression?.nullability()
                    if (nullability2 == Nullability.Nullable) return Nullability.Nullable
                    if (nullability1 == Nullability.NotNull && nullability2 == Nullability.NotNull) return Nullability.NotNull
                    Nullability.Default
                }

                is PsiParenthesizedExpression -> expression?.nullability() ?: Nullability.Default

                is PsiCallExpression -> resolveMethod()?.let { methodNullability(it) } ?: Nullability.Default

                is PsiReferenceExpression -> (resolve() as? PsiVariable)?.let { variableReferenceNullability(it, this) } ?: Nullability.Default
            //TODO: some other cases

                else -> Nullability.Default
            }
        }
    }

    private val mutabilityFlavor = object : TypeFlavor<Mutability>(Mutability.Default) {
        override fun fromDataFlowForMethod(method: PsiMethod): Mutability = Mutability.Default

        override val forEnumConstant: Mutability get() = Mutability.NonMutable

        override fun fromType(type: PsiType): Mutability {
            val target = (type as? PsiClassType)?.resolve() ?: return Mutability.NonMutable
            if (target.qualifiedName !in toKotlinMutableTypesMap.keys) return Mutability.NonMutable
            return Mutability.Default
        }

        override fun fromAnnotations(owner: PsiModifierListOwner): Mutability {
            if (owner is KtLightElement<*, *>) {
                val jetDeclaration = owner.kotlinOrigin as? KtCallableDeclaration ?: return Mutability.Default
                val descriptor = converter.services.resolverForConverter.resolveToDescriptor(jetDeclaration) as? CallableDescriptor ?: return Mutability.Default
                val type = descriptor.returnType ?: return Mutability.Default
                val classDescriptor = TypeUtils.getClassDescriptor(type) ?: return Mutability.Default
                return if (DescriptorUtils.getFqName(classDescriptor).asString() in mutableKotlinClasses)
                    Mutability.Mutable
                else
                    Mutability.NonMutable
            }
            //TODO: Kotlin compiled elements

            return super.fromAnnotations(owner) //TODO: ReadOnly annotation
        }

        override fun fromUsage(usage: PsiExpression)
                = if (isMutableFromUsage(usage)) Mutability.Mutable else Mutability.Default

        private fun isMutableFromUsage(usage: PsiExpression): Boolean {
            val parent = usage.parent
            if (parent is PsiReferenceExpression && usage == parent.qualifierExpression && parent.parent is PsiMethodCallExpression) {
                return if (possibleModificationMethodNames.contains(parent.referenceName))
                    isMutableFromUsage(parent.parent as PsiExpression)
                else
                    modificationMethodNames.contains(parent.referenceName)
            }
            else if (parent is PsiExpressionList) {
                val call = parent.parent as? PsiCall ?: return false
                val method = call.resolveMethod() ?: return false
                val paramIndex = parent.expressions.indexOf(usage)
                val parameterList = method.parameterList
                if (paramIndex >= parameterList.parametersCount) return false
                return variableMutability(parameterList.parameters[paramIndex]) == Mutability.Mutable
            }
            else if (parent is PsiVariable && usage == parent.initializer) {
                return variableMutability(parent) == Mutability.Mutable
            }
            else if (parent is PsiAssignmentExpression && parent.operationTokenType == JavaTokenType.EQ && usage == parent.rExpression) {
                val leftSideVar = (parent.lExpression as? PsiReferenceExpression)?.resolve() as? PsiVariable ?: return false
                return variableMutability(leftSideVar) == Mutability.Mutable
            }
            return false
        }
    }

    companion object {
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
                "add", "remove", "set", "addAll", "removeAll", "retainAll", "clear", "put", "putAll", "putIfAbsent", "replace",
                "replaceAll", "merge", "compute", "computeIfAbsent", "computeIfPresent", "removeIf"
        )

        private val possibleModificationMethodNames = setOf(
                "iterator", "listIterator", "spliterator", "keySet", "entrySet", "values"
        )

        private val mutableKotlinClasses = toKotlinMutableTypesMap.values.toSet()
    }
}
