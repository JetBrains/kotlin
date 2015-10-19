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
import org.jetbrains.kotlin.utils.addToStdlib.check
import java.io.PrintStream
import java.util.*

enum class SpecialMethod(val qualifiedClassName: String?, val methodName: String, val parameterCount: Int?) {
    CHAR_SEQUENCE_LENGTH(CharSequence::class.java.name, "length", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertMethodCallToPropertyUse(codeConverter, qualifier)
    },

    COLLECTION_SIZE(Collection::class.java.name, "size", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertMethodCallToPropertyUse(codeConverter, qualifier)
    },

    MAP_SIZE(Map::class.java.name, "size", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertMethodCallToPropertyUse(codeConverter, qualifier)
    },

    MAP_KEY_SET(Map::class.java.name, "keySet", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertMethodCallToPropertyUse(codeConverter, qualifier, "keys")
    },

    MAP_VALUES(Map::class.java.name, "values", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertMethodCallToPropertyUse(codeConverter, qualifier)
    },

    MAP_ENTRY_SET(Map::class.java.name, "entrySet", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertMethodCallToPropertyUse(codeConverter, qualifier, "entries")
    },

    ENUM_NAME(Enum::class.java.name, "name", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertMethodCallToPropertyUse(codeConverter, qualifier, "name")
    },

    ENUM_ORDINAL(Enum::class.java.name, "ordinal", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertMethodCallToPropertyUse(codeConverter, qualifier)
    },

    CHAR_AT(CharSequence::class.java.name, "charAt", 1) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertWithChangedName("get", qualifier, arguments, typeArgumentsConverted, codeConverter)
    },

    NUMBER_BYTE_VALUE(Number::class.java.name, "byteValue", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertWithChangedName("toByte", qualifier, arguments, typeArgumentsConverted, codeConverter)
    },

    NUMBER_SHORT_VALUE(Number::class.java.name, "shortValue", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertWithChangedName("toShort", qualifier, arguments, typeArgumentsConverted, codeConverter)
    },

    NUMBER_INT_VALUE(Number::class.java.name, "intValue", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertWithChangedName("toInt", qualifier, arguments, typeArgumentsConverted, codeConverter)
    },

    NUMBER_LONG_VALUE(Number::class.java.name, "longValue", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertWithChangedName("toLong", qualifier, arguments, typeArgumentsConverted, codeConverter)
    },

    NUMBER_FLOAT_VALUE(Number::class.java.name, "floatValue", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertWithChangedName("toFloat", qualifier, arguments, typeArgumentsConverted, codeConverter)
    },

    NUMBER_DOUBLE_VALUE(Number::class.java.name, "doubleValue", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertWithChangedName("toDouble", qualifier, arguments, typeArgumentsConverted, codeConverter)
    },

    LIST_REMOVE(List::class.java.name, "remove", 1) {
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher)
                = super.matches(method, superMethodsSearcher) && method.parameterList.parameters.single().type.canonicalText == "int"

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertWithChangedName("removeAt", qualifier, arguments, typeArgumentsConverted, codeConverter)
    },

    OBJECT_EQUALS(null, "equals", 1) {
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean
                = super.matches(method, superMethodsSearcher) && method.getParameterList().getParameters().single().getType().getCanonicalText() == JAVA_LANG_OBJECT

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression? {
            if (qualifier == null || qualifier is PsiSuperExpression) return null
            return BinaryExpression(codeConverter.convertExpression(qualifier), codeConverter.convertExpression(arguments.single()), "==")
        }
    },

    OBJECT_GET_CLASS(JAVA_LANG_OBJECT, "getClass", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression {
            val identifier = Identifier("javaClass", false).assignNoPrototype()
            return if (qualifier != null) QualifiedExpression(codeConverter.convertExpression(qualifier), identifier) else identifier
        }
    },

    OBJECTS_EQUALS("java.util.Objects", "equals", 2) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = BinaryExpression(codeConverter.convertExpression(arguments[0]), codeConverter.convertExpression(arguments[1]), "==")
    },

    COLLECTIONS_EMPTY_LIST(Collections::class.java.name, "emptyList", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(null, "emptyList", listOf(), typeArgumentsConverted, false)
    },

    COLLECTIONS_EMPTY_SET(Collections::class.java.name, "emptySet", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(null, "emptySet", listOf(), typeArgumentsConverted, false)
    },

    COLLECTIONS_EMPTY_MAP(Collections::class.java.name, "emptyMap", 0) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(null, "emptyMap", listOf(), typeArgumentsConverted, false)
    },

    COLLECTIONS_SINGLETON_LIST(Collections::class.java.name, "singletonList", 1) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(null, "listOf", listOf(codeConverter.convertExpression(arguments.single())), typeArgumentsConverted, false)
    },

    COLLECTIONS_SINGLETON(Collections::class.java.name, "singleton", 1) {
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
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean
                = super.matches(method, superMethodsSearcher) && method.parameterList.parameters.last().type.canonicalText == "java.lang.Iterable<? extends java.lang.CharSequence>"

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression?
                = MethodCallExpression.buildNotNull(codeConverter.convertExpression(arguments[1]), "joinToString", codeConverter.convertExpressions(arguments.take(1)), emptyList())
    },

    STRING_JOIN_VARARG(JAVA_LANG_STRING, "join", null) {
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean
                = super.matches(method, superMethodsSearcher) && method.parameterList.let { it.parametersCount == 2 && it.parameters.last().isVarArgs }

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
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean
                = super.matches(method, superMethodsSearcher) &&
                  method.parameterList.parametersCount == 3 &&
                  method.parameterList.parameters.let { it.first().type.canonicalText == "java.util.Locale" && it.last().isVarArgs }

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression? {
            if (arguments.size() < 2) return null // incorrect call
            return MethodCallExpression.build(codeConverter.convertExpression(arguments[1]), "format", codeConverter.convertExpressions(listOf(arguments[0]) + arguments.drop(2)), emptyList(), false)
        }
    },

    STRING_FORMAT(JAVA_LANG_STRING, "format", null) {
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean {
            return super.matches(method, superMethodsSearcher) &&
                   method.parameterList.parametersCount == 2 &&
                   method.parameterList.parameters.last().isVarArgs
        }

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression? {
            if (arguments.isEmpty()) return null // incorrect call
            return MethodCallExpression.build(codeConverter.convertExpression(arguments.first()), "format", codeConverter.convertExpressions(arguments.drop(1)), emptyList(), false)
        }
    },

    STRING_VALUE_OF_CHAR_ARRAY(JAVA_LANG_STRING, "valueOf", null) {
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean {
            return super.matches(method, superMethodsSearcher)
                    && method.parameterList.parametersCount.let { it == 1 || it == 3}
                    && method.parameterList.parameters.first().type.canonicalText == "char[]"
        }

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(null, "String", codeConverter.convertExpressions(arguments), emptyList(), false)
    },

    STRING_COPY_VALUE_OF_CHAR_ARRAY(JAVA_LANG_STRING, "copyValueOf", null) {
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean {
            return super.matches(method, superMethodsSearcher)
                    && method.parameterList.parametersCount.let { it == 1 || it == 3 }
                    && method.parameterList.parameters.first().type.canonicalText == "char[]"
        }

        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = STRING_VALUE_OF_CHAR_ARRAY.convertCall(qualifier, arguments, typeArgumentsConverted, codeConverter)
    },

    STRING_VALUE_OF(JAVA_LANG_STRING, "valueOf", 1) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = MethodCallExpression.build(codeConverter.convertExpression(arguments.single()), "toString", emptyList(), emptyList(), false)
    },

    SYSTEM_OUT_PRINTLN(PrintStream::class.java.name, "println", null) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertSystemOutMethodCall(methodName, qualifier, arguments, typeArgumentsConverted, codeConverter)
    },

    SYSTEM_OUT_PRINT(PrintStream::class.java.name, "print", null) {
        override fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
                = convertSystemOutMethodCall(methodName, qualifier, arguments, typeArgumentsConverted, codeConverter)
    };

    open fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean
            = method.name == methodName && matchesClass(method, superMethodsSearcher) && matchesParameterCount(method)

    protected fun matchesClass(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean {
        if (qualifiedClassName == null) return true
        val superMethods = superMethodsSearcher.findDeepestSuperMethods(method)
        return if (superMethods.isEmpty())
            method.containingClass?.qualifiedName == qualifiedClassName
        else
            superMethods.any { it.containingClass?.qualifiedName == qualifiedClassName }
    }

    protected fun matchesParameterCount(method: PsiMethod) = parameterCount == null || parameterCount == method.parameterList.parametersCount

    abstract fun convertCall(qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter): Expression?

    protected fun convertMethodCallToPropertyUse(codeConverter: CodeConverter, qualifier: PsiExpression?, propertyName: String = methodName): Expression {
        val identifier = Identifier(propertyName, false).assignNoPrototype()
        return if (qualifier != null) QualifiedExpression(codeConverter.convertExpression(qualifier), identifier) else identifier
    }

    protected fun convertWithChangedName(name: String, qualifier: PsiExpression?, arguments: Array<PsiExpression>, typeArgumentsConverted: List<Type>, codeConverter: CodeConverter)
            = MethodCallExpression.buildNotNull(codeConverter.convertExpression(qualifier), name, codeConverter.convertExpressions(arguments), typeArgumentsConverted)

    companion object {
        private val valuesByName = values().groupBy { it.methodName }

        fun match(method: PsiMethod, argumentCount: Int, services: JavaToKotlinConverterServices): SpecialMethod? {
            val candidates = valuesByName[method.name] ?: return null
            return candidates
                    .firstOrNull { it.matches(method, services.superMethodsSearcher) }
                    ?.check { it.parameterCount == null || it.parameterCount == argumentCount } // if parameterCount is specified we should make sure that argument count is correct
        }
    }
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

