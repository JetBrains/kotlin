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
        override fun ConvertCallData.convertCall() = convertMethodCallToPropertyUse()
    },

    COLLECTION_SIZE(Collection::class.java.name, "size", 0) {
        override fun ConvertCallData.convertCall() = convertMethodCallToPropertyUse()
    },

    COLLECTION_TO_ARRAY(Collection::class.java.name, "toArray", 0) {
        override fun ConvertCallData.convertCall() = convertWithChangedName("toTypedArray", argumentsNotNull())
    },

    COLLECTION_TO_ARRAY_WITH_ARG(Collection::class.java.name, "toArray", 1) {
        override fun ConvertCallData.convertCall() = copy(arguments = emptyList()).convertWithChangedName("toTypedArray", emptyList())
    },

    MAP_SIZE(Map::class.java.name, "size", 0) {
        override fun ConvertCallData.convertCall() = convertMethodCallToPropertyUse()
    },

    MAP_KEY_SET(Map::class.java.name, "keySet", 0) {
        override fun ConvertCallData.convertCall() = convertMethodCallToPropertyUse("keys")
    },

    MAP_PUT_IF_ABSENT(Map::class.java.name, "putIfAbsent", 2) {
        override fun ConvertCallData.convertCall() = convertWithReceiverCast()
    },

    MAP_REMOVE(Map::class.java.name, "remove", 2) {
        override fun ConvertCallData.convertCall() = convertWithReceiverCast()
    },

    MAP_REPLACE(Map::class.java.name, "replace", 3) {
        override fun ConvertCallData.convertCall() = convertWithReceiverCast()
    },

    MAP_REPLACE_ALL(Map::class.java.name, "replaceAll", 1) {
        override fun ConvertCallData.convertCall() = convertWithReceiverCast()
    },

    MAP_COMPUTE(Map::class.java.name, "compute", 2) {
        override fun ConvertCallData.convertCall() = convertWithReceiverCast()
    },

    MAP_COMPUTE_IF_ABSENT(Map::class.java.name, "computeIfAbsent", 2) {
        override fun ConvertCallData.convertCall() = convertWithReceiverCast()
    },

    MAP_COMPUTE_IF_PRESENT(Map::class.java.name, "computeIfPresent", 2) {
        override fun ConvertCallData.convertCall() = convertWithReceiverCast()
    },

    MAP_MERGE(Map::class.java.name, "merge", 3) {
        override fun ConvertCallData.convertCall() = convertWithReceiverCast()
    },

    MAP_GET_OR_DEFAULT(Map::class.java.name, "getOrDefault", 2) {
        override fun ConvertCallData.convertCall() = convertWithReceiverCast()
    },

    MAP_VALUES(Map::class.java.name, "values", 0) {
        override fun ConvertCallData.convertCall() = convertMethodCallToPropertyUse()
    },

    MAP_ENTRY_SET(Map::class.java.name, "entrySet", 0) {
        override fun ConvertCallData.convertCall() = convertMethodCallToPropertyUse("entries")
    },

    ENUM_NAME(Enum::class.java.name, "name", 0) {
        override fun ConvertCallData.convertCall() = convertMethodCallToPropertyUse("name")
    },

    ENUM_ORDINAL(Enum::class.java.name, "ordinal", 0) {
        override fun ConvertCallData.convertCall() = convertMethodCallToPropertyUse()
    },

    CHAR_AT(CharSequence::class.java.name, "charAt", 1) {
        override fun ConvertCallData.convertCall() = convertWithChangedName("get", argumentsNotNull())
    },

    NUMBER_BYTE_VALUE(Number::class.java.name, "byteValue", 0) {
        override fun ConvertCallData.convertCall() = convertWithChangedName("toByte", argumentsNotNull())
    },

    NUMBER_SHORT_VALUE(Number::class.java.name, "shortValue", 0) {
        override fun ConvertCallData.convertCall() = convertWithChangedName("toShort", argumentsNotNull())
    },

    NUMBER_INT_VALUE(Number::class.java.name, "intValue", 0) {
        override fun ConvertCallData.convertCall() = convertWithChangedName("toInt", argumentsNotNull())
    },

    NUMBER_LONG_VALUE(Number::class.java.name, "longValue", 0) {
        override fun ConvertCallData.convertCall() = convertWithChangedName("toLong", argumentsNotNull())
    },

    NUMBER_FLOAT_VALUE(Number::class.java.name, "floatValue", 0) {
        override fun ConvertCallData.convertCall() = convertWithChangedName("toFloat", argumentsNotNull())
    },

    NUMBER_DOUBLE_VALUE(Number::class.java.name, "doubleValue", 0) {
        override fun ConvertCallData.convertCall() = convertWithChangedName("toDouble", argumentsNotNull())
    },

    LIST_REMOVE(List::class.java.name, "remove", 1) {
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher)
                = super.matches(method, superMethodsSearcher) && method.parameterList.parameters.single().type.canonicalText == "int"

        override fun ConvertCallData.convertCall() = convertWithChangedName("removeAt", argumentsNotNull())
    },

    THROWABLE_GET_MESSAGE(Throwable::class.java.name, "getMessage", 0) {
        override fun ConvertCallData.convertCall() = convertMethodCallToPropertyUse("message")
    },

    THROWABLE_GET_CAUSE(Throwable::class.java.name, "getCause", 0) {
        override fun ConvertCallData.convertCall() = convertMethodCallToPropertyUse("cause")
    },

    MAP_ENTRY_GET_KEY(Map::class.java.name + ".Entry", "getKey", 0) {
        override fun ConvertCallData.convertCall() = convertMethodCallToPropertyUse("key")
    },

    MAP_ENTRY_GET_VALUE(Map::class.java.name + ".Entry", "getValue", 0) {
        override fun ConvertCallData.convertCall() = convertMethodCallToPropertyUse("value")
    },

    OBJECT_EQUALS(null, "equals", 1) {
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean
                = super.matches(method, superMethodsSearcher) && method.parameterList.parameters.single().type.canonicalText == JAVA_LANG_OBJECT

        override fun ConvertCallData.convertCall(): Expression? {
            if (qualifier == null || qualifier is PsiSuperExpression) return null
            return BinaryExpression(codeConverter.convertExpression(qualifier), codeConverter.convertExpression(arguments.single()), Operator.EQEQ)
        }
    },

    OBJECT_GET_CLASS(JAVA_LANG_OBJECT, "getClass", 0) {
        override fun ConvertCallData.convertCall(): Expression {
            val identifier = Identifier.withNoPrototype("javaClass", isNullable = false)
            return if (qualifier != null)
                QualifiedExpression(codeConverter.convertExpression(qualifier), identifier, null)
            else
                identifier
        }
    },

    OBJECTS_EQUALS("java.util.Objects", "equals", 2) {
        override fun ConvertCallData.convertCall()
                = BinaryExpression(codeConverter.convertExpression(arguments[0]), codeConverter.convertExpression(arguments[1]), Operator.EQEQ)
    },

    COLLECTIONS_EMPTY_LIST(Collections::class.java.name, "emptyList", 0) {
        override fun ConvertCallData.convertCall()
                = MethodCallExpression.buildNonNull(null, "emptyList", ArgumentList.withNoPrototype(), typeArgumentsConverted)
    },

    COLLECTIONS_EMPTY_SET(Collections::class.java.name, "emptySet", 0) {
        override fun ConvertCallData.convertCall()
                = MethodCallExpression.buildNonNull(null, "emptySet", ArgumentList.withNoPrototype(), typeArgumentsConverted)
    },

    COLLECTIONS_EMPTY_MAP(Collections::class.java.name, "emptyMap", 0) {
        override fun ConvertCallData.convertCall()
                = MethodCallExpression.buildNonNull(null, "emptyMap", ArgumentList.withNoPrototype(), typeArgumentsConverted)
    },

    COLLECTIONS_SINGLETON_LIST(Collections::class.java.name, "singletonList", 1) {
        override fun ConvertCallData.convertCall(): MethodCallExpression {
            val argumentList = ArgumentList.withNoPrototype(codeConverter.convertExpression(arguments.single()))
            return MethodCallExpression.buildNonNull(null, "listOf", argumentList, typeArgumentsConverted)
        }
    },

    COLLECTIONS_SINGLETON(Collections::class.java.name, "singleton", 1) {
        override fun ConvertCallData.convertCall(): MethodCallExpression {
            val argumentList = ArgumentList.withNoPrototype(codeConverter.convertExpression(arguments.single()))
            return MethodCallExpression.buildNonNull(null, "setOf", argumentList, typeArgumentsConverted)
        }
    },

    STRING_TRIM(JAVA_LANG_STRING, "trim", 0) {
        override fun ConvertCallData.convertCall(): Expression? {
            val comparison = BinaryExpression(Identifier.withNoPrototype("it", isNullable = false), LiteralExpression("' '").assignNoPrototype(), Operator(JavaTokenType.LE).assignNoPrototype()).assignNoPrototype()
            val argumentList = ArgumentList.withNoPrototype(LambdaExpression(null, Block.of(comparison).assignNoPrototype()))
            return MethodCallExpression.buildNonNull(codeConverter.convertExpression(qualifier), "trim", argumentList, dotPrototype = dot)
        }
    },

    STRING_REPLACE_ALL(JAVA_LANG_STRING, "replaceAll", 2) {
        override fun ConvertCallData.convertCall(): MethodCallExpression {
            val argumentList = ArgumentList.withNoPrototype(
                    codeConverter.convertToRegex(arguments[0]),
                    codeConverter.convertExpression(arguments[1])
            )
            return MethodCallExpression.buildNonNull(codeConverter.convertExpression(qualifier), "replace", argumentList, dotPrototype = dot)
        }
    },

    STRING_REPLACE_FIRST(JAVA_LANG_STRING, "replaceFirst", 2) {
        override fun ConvertCallData.convertCall(): MethodCallExpression {
            return MethodCallExpression.buildNonNull(
                    codeConverter.convertExpression(qualifier), "replaceFirst",
                    ArgumentList.withNoPrototype(
                            codeConverter.convertToRegex(arguments[0]),
                            codeConverter.convertExpression(arguments[1])
                    ),
                    dotPrototype = dot
            )
        }
    },

    STRING_MATCHES(JAVA_LANG_STRING, "matches", 1) {
        override fun ConvertCallData.convertCall(): MethodCallExpression {
            val argumentList = ArgumentList.withNoPrototype(codeConverter.convertToRegex(arguments.single()))
            return MethodCallExpression.buildNonNull(codeConverter.convertExpression(qualifier), "matches", argumentList, dotPrototype = dot)
        }
    },

    STRING_SPLIT(JAVA_LANG_STRING, "split", 1) {
        override fun ConvertCallData.convertCall(): Expression? {
            val splitCall = MethodCallExpression.buildNonNull(
                    codeConverter.convertExpression(qualifier),
                    "split",
                    ArgumentList.withNoPrototype(codeConverter.convertToRegex(arguments.single())),
                    dotPrototype = dot
            ).assignNoPrototype()
            val isEmptyCall = MethodCallExpression.buildNonNull(Identifier.withNoPrototype("it", isNullable = false), "isEmpty").assignNoPrototype()
            val isEmptyCallBlock = Block.of(isEmptyCall).assignNoPrototype()
            val dropLastCall = MethodCallExpression.buildNonNull(
                    splitCall,
                    "dropLastWhile",
                    ArgumentList.withNoPrototype(LambdaExpression(null, isEmptyCallBlock).assignNoPrototype())
            ).assignNoPrototype()
            return MethodCallExpression.buildNonNull(dropLastCall, "toTypedArray")
        }
    },

    STRING_SPLIT_LIMIT(JAVA_LANG_STRING, "split", 2) {
        override fun ConvertCallData.convertCall(): Expression?  {
            val patternArgument = codeConverter.convertToRegex(arguments[0])
            val limitArgument = codeConverter.convertExpression(arguments[1])
            val evaluator = PsiExpressionEvaluator()
            val limit = evaluator.computeConstantExpression(arguments[1], /* throwExceptionOnOverflow = */ false) as? Int
            val splitArguments = when {
                    limit == null ->  // not a constant
                        listOf(patternArgument, MethodCallExpression.buildNonNull(limitArgument, "coerceAtLeast", ArgumentList.withNoPrototype(LiteralExpression("0").assignNoPrototype())).assignNoPrototype())
                    limit < 0 ->      // negative, same behavior as split(regex) in kotlin
                        listOf(patternArgument)
                    limit == 0 -> { // zero, same replacement as for split without limit
                        val newCallData = copy(arguments = listOf(arguments[0]))
                        return STRING_SPLIT.convertCall(newCallData)
                    }
                    else ->           // positive, same behavior as split(regex, limit) in kotlin
                        listOf(patternArgument, limitArgument)
            }

            val splitCall = MethodCallExpression.buildNonNull(
                    codeConverter.convertExpression(qualifier),
                    "split",
                    ArgumentList.withNoPrototype(splitArguments),
                    dotPrototype = dot
            ).assignNoPrototype()
            return MethodCallExpression.buildNonNull(splitCall, "toTypedArray")
        }
    },

    STRING_JOIN(JAVA_LANG_STRING, "join", 2) {
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean
                = super.matches(method, superMethodsSearcher) && method.parameterList.parameters.last().type.canonicalText == "java.lang.Iterable<? extends java.lang.CharSequence>"

        override fun ConvertCallData.convertCall(): Expression? {
            val argumentList = ArgumentList.withNoPrototype(codeConverter.convertExpressionsInList(arguments.take(1)))
            return MethodCallExpression.buildNonNull(codeConverter.convertExpression(arguments[1]), "joinToString", argumentList)
        }
    },

    STRING_JOIN_VARARG(JAVA_LANG_STRING, "join", null) {
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean
                = super.matches(method, superMethodsSearcher) && method.parameterList.let { it.parametersCount == 2 && it.parameters.last().isVarArgs }

        override fun ConvertCallData.convertCall(): Expression? {
            if (arguments.size == 2 && arguments.last().isAssignableToCharSequenceArray()) {
                return STRING_JOIN.convertCall(this)
            }
            else {
                return MethodCallExpression.buildNonNull(
                        MethodCallExpression.buildNonNull(null, "arrayOf", ArgumentList.withNoPrototype(codeConverter.convertExpressionsInList(arguments.drop(1)))).assignNoPrototype(),
                        "joinToString",
                        ArgumentList.withNoPrototype(codeConverter.convertExpressionsInList(arguments.take(1)))
                )
            }
        }

        private fun PsiExpression.isAssignableToCharSequenceArray(): Boolean {
            val charSequenceType = PsiType.getTypeByName("java.lang.CharSequence", project, resolveScope)
            return (type as? PsiArrayType)?.componentType?.let { charSequenceType.isAssignableFrom(it) } ?: false
        }
    },

    STRING_CONCAT(JAVA_LANG_STRING, "concat", 1) {
        override fun ConvertCallData.convertCall()
                = BinaryExpression(codeConverter.convertExpression(qualifier), codeConverter.convertExpression(arguments.single()), Operator(JavaTokenType.PLUS).assignNoPrototype())
    },

    STRING_COMPARE_TO_IGNORE_CASE(JAVA_LANG_STRING, "compareToIgnoreCase", 1) {
        override fun ConvertCallData.convertCall() = convertWithIgnoreCaseArgument("compareTo")
    },

    STRING_EQUALS_IGNORE_CASE(JAVA_LANG_STRING, "equalsIgnoreCase", 1) {
        override fun ConvertCallData.convertCall() = convertWithIgnoreCaseArgument("equals")
    },

    STRING_REGION_MATCHES(JAVA_LANG_STRING, "regionMatches", 5) {
        override fun ConvertCallData.convertCall()
                = copy(arguments = arguments.drop(1)).convertWithIgnoreCaseArgument("regionMatches", ignoreCaseArgument = arguments.first())
    },

    STRING_GET_BYTES(JAVA_LANG_STRING, "getBytes", null) {
        override fun ConvertCallData.convertCall(): MethodCallExpression {
            val charsetArg = arguments.lastOrNull()?.check { it.type?.canonicalText == JAVA_LANG_STRING }
            val convertedArguments = codeConverter.convertExpressionsInList(arguments).map {
                if (charsetArg != null && it.prototypes?.singleOrNull()?.element == charsetArg)
                    MethodCallExpression.buildNonNull(null, "charset", ArgumentList.withNoPrototype(it)).assignNoPrototype()
                else
                    it
            }
            return MethodCallExpression.buildNonNull(
                    codeConverter.convertExpression(qualifier),
                    "toByteArray",
                    ArgumentList.withNoPrototype(convertedArguments),
                    dotPrototype = dot
            )
        }
    },

    STRING_GET_CHARS(JAVA_LANG_STRING, "getChars", 4) {
        override fun ConvertCallData.convertCall(): MethodCallExpression {
            // reorder parameters: srcBegin(0), srcEnd(1), dst(2), dstOffset(3) -> destination(2), destinationOffset(3), startIndex(0), endIndex(1)
            val argumentList = ArgumentList.withNoPrototype(codeConverter.convertExpressionsInList(arguments.slice(listOf(2, 3, 0, 1))))
            return MethodCallExpression.buildNonNull(
                    codeConverter.convertExpression(qualifier),
                    "toCharArray",
                    argumentList,
                    dotPrototype = dot
            )
        }
    },

    STRING_VALUE_OF_CHAR_ARRAY(JAVA_LANG_STRING, "valueOf", null) {
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean {
            return super.matches(method, superMethodsSearcher)
                    && method.parameterList.parametersCount.let { it == 1 || it == 3}
                    && method.parameterList.parameters.first().type.canonicalText == "char[]"
        }

        override fun ConvertCallData.convertCall()
                = MethodCallExpression.buildNonNull(null, "String", ArgumentList.withNoPrototype(codeConverter.convertExpressionsInList(arguments)))
    },

    STRING_COPY_VALUE_OF_CHAR_ARRAY(JAVA_LANG_STRING, "copyValueOf", null) {
        override fun matches(method: PsiMethod, superMethodsSearcher: SuperMethodsSearcher): Boolean {
            return super.matches(method, superMethodsSearcher)
                    && method.parameterList.parametersCount.let { it == 1 || it == 3 }
                    && method.parameterList.parameters.first().type.canonicalText == "char[]"
        }

        override fun ConvertCallData.convertCall()
                = STRING_VALUE_OF_CHAR_ARRAY.convertCall(this)
    },

    STRING_VALUE_OF(JAVA_LANG_STRING, "valueOf", 1) {
        override fun ConvertCallData.convertCall()
                = MethodCallExpression.buildNonNull(codeConverter.convertExpression(arguments.single(), shouldParenthesize = true), "toString")
    },

    SYSTEM_OUT_PRINTLN(PrintStream::class.java.name, "println", null) {
        override fun ConvertCallData.convertCall() = convertSystemOutMethodCall(methodName)
    },

    SYSTEM_OUT_PRINT(PrintStream::class.java.name, "print", null) {
        override fun ConvertCallData.convertCall() = convertSystemOutMethodCall(methodName)
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

    data class ConvertCallData(
            val qualifier: PsiExpression?,
            @Suppress("ArrayInDataClass") val arguments: List<PsiExpression>,
            val typeArgumentsConverted: List<Type>,
            val dot: PsiElement?,
            val lPar: PsiElement?,
            val rPar: PsiElement?,
            val codeConverter: CodeConverter
    )

    @JvmName("convertCallPublic")
    fun convertCall(data: ConvertCallData): Expression? = data.convertCall()

    protected abstract fun ConvertCallData.convertCall(): Expression?

    protected fun ConvertCallData.convertMethodCallToPropertyUse(propertyName: String = methodName): Expression {
        val identifier = Identifier.withNoPrototype(propertyName, isNullable = false)
        return if (qualifier != null)
            QualifiedExpression(codeConverter.convertExpression(qualifier), identifier, dot)
        else
            identifier
    }

    protected fun ConvertCallData.argumentsNotNull() = arguments.map { Nullability.NotNull }

    protected fun ConvertCallData.convertWithChangedName(name: String, argumentNullabilities: List<Nullability>): MethodCallExpression {
        assert(argumentNullabilities.size == arguments.size)
        val argumentsConverted = arguments.zip(argumentNullabilities).map {
            codeConverter.convertExpression(it.first, null, it.second).assignPrototype(it.first, CommentsAndSpacesInheritance.LINE_BREAKS)
        }
        val argumentList = ArgumentList(argumentsConverted, LPar.withPrototype(lPar), RPar.withPrototype(rPar)).assignNoPrototype()
        return MethodCallExpression.buildNonNull(
                codeConverter.convertExpression(qualifier),
                name,
                argumentList,
                typeArgumentsConverted,
                dot)
    }

    protected fun ConvertCallData.convertWithReceiverCast(): MethodCallExpression? {
        val convertedArguments = codeConverter.convertExpressionsInList(arguments)
        val qualifierWithCast = castQualifierToType(codeConverter, qualifier!!, qualifiedClassName!!) ?: return null
        return MethodCallExpression.buildNonNull(
                qualifierWithCast,
                methodName,
                ArgumentList.withNoPrototype(convertedArguments),
                typeArgumentsConverted,
                dot)
    }

    private fun castQualifierToType(codeConverter: CodeConverter, qualifier: PsiExpression, type: String): TypeCastExpression? {
        val convertedQualifier = codeConverter.convertExpression(qualifier)
        val qualifierType = codeConverter.typeConverter.convertType(qualifier.type)
        val typeArgs = if (qualifierType is ClassType) qualifierType.referenceElement.typeArgs else emptyList()
        val referenceElement = ReferenceElement(Identifier.withNoPrototype(type), typeArgs).assignNoPrototype()
        val newType = ClassType(referenceElement, Nullability.Default, codeConverter.settings).assignNoPrototype()
        return TypeCastExpression(newType, convertedQualifier).assignNoPrototype()
    }

    protected fun ConvertCallData.convertWithIgnoreCaseArgument(methodName: String, ignoreCaseArgument: PsiExpression? = null): Expression {
        val ignoreCaseExpression = ignoreCaseArgument?.let { codeConverter.convertExpression(it) }
                                   ?: LiteralExpression("true").assignNoPrototype()
        val ignoreCaseArgumentExpression = AssignmentExpression(Identifier.withNoPrototype("ignoreCase"), ignoreCaseExpression, Operator.EQ).assignNoPrototype()
        val convertedArguments = arguments.map {
            codeConverter.convertExpression(it, null, Nullability.NotNull).assignPrototype(it, CommentsAndSpacesInheritance.LINE_BREAKS)
        } + ignoreCaseArgumentExpression
        val argumentList = ArgumentList(convertedArguments, LPar.withPrototype(lPar), RPar.withPrototype(rPar)).assignNoPrototype()
        return MethodCallExpression.buildNonNull(
                codeConverter.convertExpression(qualifier),
                methodName,
                argumentList,
                typeArgumentsConverted,
                dot)
    }

    protected fun ConvertCallData.convertSystemOutMethodCall(methodName: String): Expression? {
        if (qualifier !is PsiReferenceExpression) return null
        val qqualifier = qualifier.qualifierExpression as? PsiReferenceExpression ?: return null
        if (qqualifier.canonicalText != "java.lang.System") return null
        if (qualifier.referenceName != "out") return null
        if (typeArgumentsConverted.isNotEmpty()) return null
        val argumentList = ArgumentList(
                codeConverter.convertExpressionsInList(arguments),
                LPar.withPrototype(lPar),
                RPar.withPrototype(rPar)
        ).assignNoPrototype()
        return MethodCallExpression.buildNonNull(null, methodName, argumentList)
    }

    protected fun CodeConverter.convertToRegex(expression: PsiExpression?): Expression
            = MethodCallExpression.buildNonNull(convertExpression(expression, shouldParenthesize = true), "toRegex").assignNoPrototype()

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

