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

import com.intellij.psi.CommonClassNames.JAVA_LANG_OBJECT
import com.intellij.psi.PsiExpression
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.PsiSuperExpression
import org.jetbrains.kotlin.j2k.ast.*

enum class SpecialMethod(val qualifiedClassName: String?, val methodName: String, val parameterCount: Int?) {
    OBJECT_EQUALS(null, "equals", 1) {
        override fun matches(method: PsiMethod)
                = super.matches(method) && method.getParameterList().getParameters().single().getType().getCanonicalText() == JAVA_LANG_OBJECT

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression? {
            if (qualifier == null || qualifier is PsiSuperExpression) return null
            return BinaryExpression(codeConverter.convertExpression(qualifier), codeConverter.convertExpression(arguments.single()), "==")
        }
    },

    OBJECT_GET_CLASS("java.lang.Object", "getClass", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression {
            val identifier = Identifier("javaClass", false).assignNoPrototype()
            return if (qualifier != null) QualifiedExpression(codeConverter.convertExpression(qualifier), identifier) else identifier
        }
    },

    OBJECTS_EQUALS("java.util.Objects", "equals", 2) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = BinaryExpression(codeConverter.convertExpression(arguments[0]), codeConverter.convertExpression(arguments[1]), "==")
    },

    COLLECTIONS_EMPTY_LIST("java.util.Collections", "emptyList", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(null, "emptyList", listOf(), typeArgumentsConverted, false)
    },

    COLLECTIONS_EMPTY_SET("java.util.Collections", "emptySet", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(null, "emptySet", listOf(), typeArgumentsConverted, false)
    },

    COLLECTIONS_EMPTY_MAP("java.util.Collections", "emptyMap", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(null, "emptyMap", listOf(), typeArgumentsConverted, false)
    },

    COLLECTIONS_SINGLETON_LIST("java.util.Collections", "singletonList", 1) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(null, "listOf", listOf(codeConverter.convertExpression(arguments.single())), typeArgumentsConverted, false)
    },

    COLLECTIONS_SINGLETON("java.util.Collections", "singleton", 1) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(null, "setOf", listOf(codeConverter.convertExpression(arguments.single())), typeArgumentsConverted, false)
    },

    SYSTEM_OUT_PRINTLN("java.io.PrintStream", "println", null) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertSystemOutMethodCall(methodName, qualifier, arguments, typeArgumentsConverted, codeConverter)
    },

    SYSTEM_OUT_PRINT("java.io.PrintStream", "print", null) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertSystemOutMethodCall(methodName, qualifier, arguments, typeArgumentsConverted, codeConverter)
    };

    open fun matches(method: PsiMethod): Boolean {
        if (method.getName() != methodName) return false
        if (qualifiedClassName != null && method.getContainingClass()?.getQualifiedName() != qualifiedClassName) return false
        return parameterCount == null || parameterCount == method.getParameterList().getParametersCount()
    }

    abstract fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression?
}

private fun convertSystemOutMethodCall(
        methodName: String,
        qualifier: PsiExpression?,
        arguments: Array<PsiExpression>,
        typeArgumentsConverted: List<Type>,
        codeConverter: CodeConverter
): Expression? {
    if (qualifier !is PsiReferenceExpression) return null
    val qqualifier = qualifier.getQualifierExpression() as? PsiReferenceExpression ?: return null
    if (qqualifier.getCanonicalText() != "java.lang.System") return null
    if (qualifier.getReferenceName() != "out") return null
    if (typeArgumentsConverted.isNotEmpty()) return null
    return MethodCallExpression.build(null, methodName, arguments.map { codeConverter.convertExpression(it) }, emptyList(), false)
}
