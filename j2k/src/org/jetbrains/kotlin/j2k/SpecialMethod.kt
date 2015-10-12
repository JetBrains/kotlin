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
import com.intellij.psi.CommonClassNames.JAVA_LANG_OBJECT
import com.intellij.psi.CommonClassNames.JAVA_LANG_STRING
import com.intellij.psi.impl.PsiExpressionEvaluator
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

    STRING_TRIM(JAVA_LANG_STRING, "trim", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression? {
            val comparison = BinaryExpression(Identifier("it", isNullable = false).assignNoPrototype(), LiteralExpression("' '").assignNoPrototype(), "<=").assignNoPrototype()
            return MethodCallExpression.buildNotNull(
                    codeConverter.convertExpression(qualifier), "trim",
                    listOf(LambdaExpression(null, Block.of(comparison).assignNoPrototype())), emptyList())
        }
    },

    STRING_REPLACE_ALL(JAVA_LANG_STRING, "replaceAll", 2) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(codeConverter.convertExpression(qualifier), "replace",
                                             listOf(
                                                 codeConverter.convertToRegex(arguments[0]),
                                                 codeConverter.convertExpression(arguments[1])
                                             ), emptyList(), false)
    },

    STRING_REPLACE_FIRST(JAVA_LANG_STRING, "replaceFirst", 2) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(codeConverter.convertExpression(qualifier), "replaceFirst",
                                             listOf(
                                                     codeConverter.convertToRegex(arguments[0]),
                                                     codeConverter.convertExpression(arguments[1])
                                             ), emptyList(), false)
    },

    STRING_MATCHES(JAVA_LANG_STRING, "matches", 1) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(codeConverter.convertExpression(qualifier), "matches", listOf(codeConverter.convertToRegex(arguments.single())), emptyList(), false)
    },

    STRING_SPLIT(JAVA_LANG_STRING, "split", 1) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression? {
            val splitCall = MethodCallExpression.buildNotNull(codeConverter.convertExpression(qualifier), "split", listOf(codeConverter.convertToRegex(arguments.single())), emptyList()).assignNoPrototype()
            val isEmptyCall = MethodCallExpression.buildNotNull(Identifier("it", isNullable = false).assignNoPrototype(), "isEmpty", emptyList(), emptyList()).assignNoPrototype()
            val isEmptyCallBlock = Block.of(isEmptyCall).assignNoPrototype()
            val dropLastCall = MethodCallExpression.buildNotNull(splitCall, "dropLastWhile", listOf(LambdaExpression(null, isEmptyCallBlock).assignNoPrototype())).assignNoPrototype()
            return MethodCallExpression.buildNotNull(dropLastCall, "toTypedArray", emptyList(), emptyList())
        }
    },

    STRING_SPLIT_LIMIT(JAVA_LANG_STRING, "split", 2) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression?  {
            val patternArgument = codeConverter.convertToRegex(arguments[0])
            val limitArgument = codeConverter.convertExpression(arguments[1])
            val evaluator = PsiExpressionEvaluator()
            val limit = evaluator.computeConstantExpression(arguments[1], /* throwExceptionOnOverflow = */ false) as? Int
            val splitArguments = when {
                    limit == null ->  // not a constant
                        listOf(patternArgument, MethodCallExpression.buildNotNull(limitArgument, "coerceAtLeast", listOf(LiteralExpression("0").assignNoPrototype()), emptyList()).assignNoPrototype())
                    limit < 0 ->      // negative, same behavior as split(regex) in kotlin
                        listOf(patternArgument)
                    limit == 0 ->     // zero, same replacement as for split without limit
                        return STRING_SPLIT.convertCall(qualifier, arrayOf(arguments[0]), typeArgumentsConverted, codeConverter)
                    else ->           // positive, same behavior as split(regex, limit) in kotlin
                        listOf(patternArgument, limitArgument)
            }

            val splitCall = MethodCallExpression.buildNotNull(codeConverter.convertExpression(qualifier), "split", splitArguments, emptyList()).assignNoPrototype()
            return MethodCallExpression.buildNotNull(splitCall, "toTypedArray", emptyList(), emptyList())
        }
    },

    STRING_JOIN(JAVA_LANG_STRING, "join", 2) {
        override fun matches(method: PsiMethod)
                = super.matches(method) && method.parameterList.parameters.last().type.canonicalText == "java.lang.Iterable<? extends java.lang.CharSequence>"

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression?
                = MethodCallExpression.buildNotNull(codeConverter.convertExpression(arguments[1]), "joinToString", codeConverter.convertExpressions(arguments.take(1)), emptyList())
    },

    STRING_JOIN_VARARG(JAVA_LANG_STRING, "join", null) {
        override fun matches(method: PsiMethod): Boolean = super.matches(method) && method.parameterList.let { it.parametersCount == 2 && it.parameters.last().isVarArgs }

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression? {
            if (arguments.size() == 2 && arguments.last().isAssignableToCharSequenceArray()) {
                return STRING_JOIN.convertCall(qualifier, arguments, typeArgumentsConverted, codeConverter)
            }
            else {
                return MethodCallExpression.buildNotNull(
                        MethodCallExpression.buildNotNull(null, "arrayOf", codeConverter.convertExpressions(arguments.drop(1))).assignNoPrototype(),
                        "joinToString",
                        codeConverter.convertExpressions(arguments.take(1))
                )
            }
        }

        private fun PsiExpression.isAssignableToCharSequenceArray(): Boolean {
            val charSequenceType = PsiType.getTypeByName("java.lang.CharSequence", project, resolveScope)
            return (type as? PsiArrayType)?.componentType?.let { charSequenceType.isAssignableFrom(it) } ?: false
        }
    },


    STRING_COMPARE_TO_IGNORE_CASE(JAVA_LANG_STRING, "compareToIgnoreCase", 1) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = addIgnoreCaseArgument(qualifier, "compareTo", arguments, typeArgumentsConverted, codeConverter)
    },

    STRING_EQUALS_IGNORE_CASE(JAVA_LANG_STRING, "equalsIgnoreCase", 1) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = addIgnoreCaseArgument(qualifier, "equals", arguments, typeArgumentsConverted, codeConverter)
    },

    STRING_REGION_MATCHES(JAVA_LANG_STRING, "regionMatches", 5) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = addIgnoreCaseArgument(qualifier, "regionMatches", arguments.drop(1).toTypedArray(), typeArgumentsConverted, codeConverter, arguments.first())
    },

    STRING_GET_BYTES(JAVA_LANG_STRING, "getBytes", null) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(codeConverter.convertExpression(qualifier), "toByteArray", codeConverter.convertExpressions(arguments), emptyList(), false)
    },

    STRING_FORMAT_WITH_LOCALE(JAVA_LANG_STRING, "format", null) {
        override fun matches(method: PsiMethod)
                = super.matches(method) &&
                  method.parameterList.parametersCount == 3 &&
                  method.parameterList.parameters.let { it.first().type.canonicalText == "java.util.Locale" && it.last().isVarArgs }

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(codeConverter.convertExpression(arguments[1]), "format", codeConverter.convertExpressions(listOf(arguments[0]) + arguments.drop(2)), emptyList(), false)
    },

    STRING_FORMAT(JAVA_LANG_STRING, "format", null) {
        override fun matches(method: PsiMethod): Boolean {
            return super.matches(method) &&
                   method.parameterList.parametersCount == 2 &&
                   method.parameterList.parameters.last().isVarArgs
        }

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(codeConverter.convertExpression(arguments.first()), "format", codeConverter.convertExpressions(arguments.drop(1)), emptyList(), false)
    },

    STRING_VALUE_OF_CHAR_ARRAY(JAVA_LANG_STRING, "valueOf", null) {
        override fun matches(method: PsiMethod)
                = matchesClass(method) &&
                  (matchesName(method) || matchesName(method, "copyValueOf")) &&
                  method.parameterList.parametersCount.let { it == 1 || it == 3} &&
                  method.parameterList.parameters.first().type.canonicalText == "char[]"

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(null, "String", codeConverter.convertExpressions(arguments), emptyList(), false)
    },

    STRING_VALUE_OF(JAVA_LANG_STRING, "valueOf", 1) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(codeConverter.convertExpression(arguments.single()), "toString", emptyList(), emptyList(), false)
    },

    SYSTEM_OUT_PRINTLN("java.io.PrintStream", "println", null) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertSystemOutMethodCall(methodName, qualifier, arguments, typeArgumentsConverted, codeConverter)
    },

    SYSTEM_OUT_PRINT("java.io.PrintStream", "print", null) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertSystemOutMethodCall(methodName, qualifier, arguments, typeArgumentsConverted, codeConverter)
    };

    open fun matches(method: PsiMethod): Boolean = matchesName(method) && matchesClass(method) && matchesParameterCount(method)

    protected fun matchesName(method: PsiMethod, name: String? = null) = method.name == (name ?: methodName)
    protected fun matchesClass(method: PsiMethod) = qualifiedClassName == null || method.containingClass?.qualifiedName == qualifiedClassName
    protected fun matchesParameterCount(method: PsiMethod) = parameterCount == null || parameterCount == method.parameterList.parametersCount

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

private fun CodeConverter.convertToRegex(expression: PsiExpression?): Expression
        = MethodCallExpression.build(convertExpression(expression), "toRegex", emptyList(), emptyList(), false).assignNoPrototype()

private fun addIgnoreCaseArgument(
        qualifier: PsiExpression?,
        methodName: String,
        arguments: Array<PsiExpression>,
        typeArgumentsConverted: List<Type>,
        codeConverter: CodeConverter,
        ignoreCaseArgument: PsiExpression? = null
): Expression {
    val ignoreCaseExpression = ignoreCaseArgument?.let { codeConverter.convertExpression(it) } ?: LiteralExpression("true").assignNoPrototype()
    val ignoreCaseArgumentExpression = AssignmentExpression(Identifier("ignoreCase").assignNoPrototype(), ignoreCaseExpression, "=").assignNoPrototype()
    return MethodCallExpression.build(codeConverter.convertExpression(qualifier), methodName,
                                      codeConverter.convertExpressions(arguments) + ignoreCaseArgumentExpression,
                                      typeArgumentsConverted, false)
}

