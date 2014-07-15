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

package org.jetbrains.jet.j2k.visitors

import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiExpression
import org.jetbrains.jet.j2k.Converter
import org.jetbrains.jet.j2k.ast.BinaryExpression
import org.jetbrains.jet.j2k.ast.Expression
import org.jetbrains.jet.j2k.ast.MethodCallExpression
import com.intellij.psi.CommonClassNames.JAVA_LANG_OBJECT
import com.intellij.psi.PsiSuperExpression
import org.jetbrains.jet.j2k.ast.QualifiedExpression
import org.jetbrains.jet.j2k.ast.Identifier
import org.jetbrains.jet.j2k.ast.assignNoPrototype
import org.jetbrains.jet.j2k.ast.Type

enum class SpecialMethod(val qualifiedClassName: String?, val methodName: String, val parameterCount: Int) {
    OBJECT_EQUALS: SpecialMethod(null, "equals", 1) {
        override fun matches(method: PsiMethod)
                = super.matches(method) && method.getParameterList().getParameters().single().getType().getCanonicalText() == JAVA_LANG_OBJECT

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, converter: Converter): Expression? {
            if (qualifier == null || qualifier is PsiSuperExpression) return null
            return BinaryExpression(converter.convertExpression(qualifier), converter.convertExpression(arguments.single()), "==")
        }
    }

    OBJECT_GET_CLASS: SpecialMethod("java.lang.Object", "getClass", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, converter: Converter): Expression {
            val identifier = Identifier("javaClass", false).assignNoPrototype()
            return if (qualifier != null) QualifiedExpression(converter.convertExpression(qualifier), identifier) else identifier
        }
    }

    OBJECTS_EQUALS: SpecialMethod("java.util.Objects", "equals", 2) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, converter: Converter)
                = BinaryExpression(converter.convertExpression(arguments[0]), converter.convertExpression(arguments[1]), "==")
    }

    COLLECTIONS_EMPTY_LIST: SpecialMethod("java.util.Collections", "emptyList", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, converter: Converter)
                = MethodCallExpression.build(null, "listOf", listOf(), typeArgumentsConverted, false)
    }

    COLLECTIONS_EMPTY_SET: SpecialMethod("java.util.Collections", "emptySet", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, converter: Converter)
                = MethodCallExpression.build(null, "setOf", listOf(), typeArgumentsConverted, false)
    }

    COLLECTIONS_EMPTY_MAP: SpecialMethod("java.util.Collections", "emptyMap", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, converter: Converter)
                = MethodCallExpression.build(null, "mapOf", listOf(), typeArgumentsConverted, false)
    }

    COLLECTIONS_SINGLETON_LIST: SpecialMethod("java.util.Collections", "singletonList", 1) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, converter: Converter)
                = MethodCallExpression.build(null, "listOf", listOf(converter.convertExpression(arguments.single())), listOf(), false)
    }

    COLLECTIONS_SINGLETON: SpecialMethod("java.util.Collections", "singleton", 1) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, converter: Converter)
                = MethodCallExpression.build(null, "setOf", listOf(converter.convertExpression(arguments.single())), listOf(), false)
    }

    open fun matches(method: PsiMethod): Boolean {
        if (method.getName() != methodName) return false
        if (qualifiedClassName != null && method.getContainingClass()?.getQualifiedName() != qualifiedClassName) return false
        return method.getParameterList().getParametersCount() == parameterCount
    }

    abstract fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, converter: Converter): Expression?
}