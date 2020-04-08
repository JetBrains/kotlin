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

package org.jetbrains.kotlin.idea.core

import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.builtins.isFunctionType
import org.jetbrains.kotlin.lexer.KotlinLexer
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getOutermostParenthesizerOrThis
import org.jetbrains.kotlin.psi.psiUtil.isIdentifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getParentResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ArgumentMatch
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeUtils
import org.jetbrains.kotlin.types.checker.KotlinTypeChecker
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.decapitalizeSmart
import java.util.*

object KotlinNameSuggester {
    fun suggestNamesByExpressionAndType(
        expression: KtExpression,
        type: KotlinType?,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean,
        defaultName: String?
    ): Collection<String> {
        val result = LinkedHashSet<String>()

        result.addNamesByExpression(expression, bindingContext, validator)

        (type ?: bindingContext?.getType(expression))?.let {
            result.addNamesByType(it, validator)
        }

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    fun suggestNamesByType(type: KotlinType, validator: (String) -> Boolean, defaultName: String? = null): List<String> {
        val result = ArrayList<String>()

        result.addNamesByType(type, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    fun suggestNamesByExpressionOnly(
        expression: KtExpression,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean, defaultName: String? = null
    ): List<String> {
        val result = ArrayList<String>()

        result.addNamesByExpression(expression, bindingContext, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    fun suggestIterationVariableNames(
        collection: KtExpression,
        elementType: KotlinType,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean, defaultName: String?
    ): Collection<String> {
        val result = LinkedHashSet<String>()

        suggestNamesByExpressionOnly(collection, bindingContext, { true })
            .mapNotNull { StringUtil.unpluralize(it) }
            .mapTo(result) { suggestNameByName(it, validator) }

        result.addNamesByType(elementType, validator)

        if (result.isEmpty()) {
            result.addName(defaultName, validator)
        }

        return result
    }

    fun suggestNamesByFqName(
        fqName: FqName,
        ignoreCompanion: Boolean = true,
        validator: (String) -> Boolean = { true },
        defaultName: () -> String? = { null }
    ): Collection<String> {
        val result = LinkedHashSet<String>()

        var name = ""
        fqName.asString().split('.').asReversed().forEach {
            if (ignoreCompanion && it == "Companion") return@forEach
            name = name.withPrefix(it)
            result.addName(name, validator)
        }

        if (result.isEmpty()) {
            result.addName(defaultName(), validator)
        }

        return result
    }

    private fun String.withPrefix(prefix: String): String {
        if (isEmpty()) return prefix
        val c = this[0]
        return (if (c in 'a'..'z') prefix.decapitalizeAsciiOnly()
        else prefix.capitalizeAsciiOnly()) + capitalizeAsciiOnly()
    }

    private val COMMON_TYPE_PARAMETER_NAMES = listOf("T", "U", "V", "W", "X", "Y", "Z")
    private const val MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS = 1000

    fun suggestNamesForTypeParameters(count: Int, validator: (String) -> Boolean): List<String> {
        val result = ArrayList<String>()
        for (i in 0 until count) {
            result.add(suggestNameByMultipleNames(COMMON_TYPE_PARAMETER_NAMES, validator))
        }
        return result
    }

    fun suggestTypeAliasNameByPsi(typeElement: KtTypeElement, validator: (String) -> Boolean): String {
        fun KtTypeElement.render(): String {
            return when (this) {
                is KtNullableType -> "Nullable${innerType?.render() ?: ""}"
                is KtFunctionType -> {
                    val arguments = listOfNotNull(receiverTypeReference) + parameters.mapNotNull { it.typeReference }
                    val argText = arguments.joinToString(separator = "") { it.typeElement?.render() ?: "" }
                    val returnText = returnTypeReference?.typeElement?.render() ?: "Unit"
                    "${argText}To$returnText"
                }
                is KtUserType -> {
                    val argText = typeArguments.joinToString(separator = "") { it.typeReference?.typeElement?.render() ?: "" }
                    "$argText${referenceExpression?.text ?: ""}"
                }
                else -> text.capitalize()
            }
        }

        return suggestNameByName(typeElement.render(), validator)
    }

    /**
     * Validates name, and slightly improves it by adding number to name in case of conflicts
     * @param name to check it in scope
     * @return name or nameI, where I is number
     */
    fun suggestNameByName(name: String, validator: (String) -> Boolean): String {
        if (validator(name)) return name
        var i = 1
        while (i <= MAX_NUMBER_OF_SUGGESTED_NAME_CHECKS && !validator(name + i)) {
            ++i
        }

        return name + i
    }

    /**
     * Validates name using set of variants which are tried in succession (and extended with suffixes if necessary)
     * For example, when given sequence of a, b, c possible names are tried out in the following order: a, b, c, a1, b1, c1, a2, b2, c2, ...
     * @param names to check it in scope
     * @return name or nameI, where name is one of variants and I is a number
     */
    fun suggestNameByMultipleNames(names: Collection<String>, validator: (String) -> Boolean): String {
        var i = 0
        while (true) {
            for (name in names) {
                val candidate = if (i > 0) name + i else name
                if (validator(candidate)) return candidate
            }
            i++
        }
    }

    private fun MutableCollection<String>.addNamesByType(type: KotlinType, validator: (String) -> Boolean) {
        val myType = TypeUtils.makeNotNullable(type) // wipe out '?'
        val builtIns = myType.builtIns
        val typeChecker = KotlinTypeChecker.DEFAULT
        if (ErrorUtils.containsErrorType(myType)) return

        when {
            typeChecker.equalTypes(builtIns.booleanType, myType) -> addName("b", validator)
            typeChecker.equalTypes(builtIns.intType, myType) -> addName("i", validator)
            typeChecker.equalTypes(builtIns.byteType, myType) -> addName("byte", validator)
            typeChecker.equalTypes(builtIns.longType, myType) -> addName("l", validator)
            typeChecker.equalTypes(builtIns.floatType, myType) -> addName("fl", validator)
            typeChecker.equalTypes(builtIns.doubleType, myType) -> addName("d", validator)
            typeChecker.equalTypes(builtIns.shortType, myType) -> addName("sh", validator)
            typeChecker.equalTypes(builtIns.charType, myType) -> addName("c", validator)
            typeChecker.equalTypes(builtIns.stringType, myType) -> addName("s", validator)
            KotlinBuiltIns.isArray(myType) || KotlinBuiltIns.isPrimitiveArray(myType) -> {
                val elementType = builtIns.getArrayElementType(myType)
                when {
                    typeChecker.equalTypes(builtIns.booleanType, elementType) -> addName("booleans", validator)
                    typeChecker.equalTypes(builtIns.intType, elementType) -> addName("ints", validator)
                    typeChecker.equalTypes(builtIns.byteType, elementType) -> addName("bytes", validator)
                    typeChecker.equalTypes(builtIns.longType, elementType) -> addName("longs", validator)
                    typeChecker.equalTypes(builtIns.floatType, elementType) -> addName("floats", validator)
                    typeChecker.equalTypes(builtIns.doubleType, elementType) -> addName("doubles", validator)
                    typeChecker.equalTypes(builtIns.shortType, elementType) -> addName("shorts", validator)
                    typeChecker.equalTypes(builtIns.charType, elementType) -> addName("chars", validator)
                    typeChecker.equalTypes(builtIns.stringType, elementType) -> addName("strings", validator)
                    else -> {
                        val classDescriptor = TypeUtils.getClassDescriptor(elementType)
                        if (classDescriptor != null) {
                            val className = classDescriptor.name
                            addName("arrayOf" + StringUtil.capitalize(className.asString()) + "s", validator)
                        }
                    }
                }
            }
            myType.isFunctionType -> addName("function", validator)
            else -> {
                val descriptor = myType.constructor.declarationDescriptor
                if (descriptor != null) {
                    val className = descriptor.name
                    if (!className.isSpecial) {
                        addCamelNames(className.asString(), validator)
                    }
                }
            }
        }
    }

    private val ACCESSOR_PREFIXES = arrayOf("get", "is", "set")

    fun getCamelNames(name: String, validator: (String) -> Boolean, startLowerCase: Boolean): List<String> {
        val result = ArrayList<String>()
        result.addCamelNames(name, validator, startLowerCase)
        return result
    }

    private fun MutableCollection<String>.addCamelNames(name: String, validator: (String) -> Boolean, startLowerCase: Boolean = true) {
        if (name === "" || !name.unquote().isIdentifier()) return
        var s = extractIdentifiers(name)

        for (prefix in ACCESSOR_PREFIXES) {
            if (!s.startsWith(prefix)) continue

            val len = prefix.length
            if (len < s.length && Character.isUpperCase(s[len])) {
                s = s.substring(len)
                break
            }
        }

        var upperCaseLetterBefore = false
        for (i in 0 until s.length) {
            val c = s[i]
            val upperCaseLetter = Character.isUpperCase(c)

            if (i == 0) {
                addName(if (startLowerCase) s.decapitalizeSmart() else s, validator)
            } else {
                if (upperCaseLetter && !upperCaseLetterBefore) {
                    val substring = s.substring(i)
                    addName(if (startLowerCase) substring.decapitalizeSmart() else substring, validator)
                }
            }

            upperCaseLetterBefore = upperCaseLetter
        }
    }

    private fun extractIdentifiers(s: String): String {
        return buildString {
            val lexer = KotlinLexer()
            lexer.start(s)
            while (lexer.tokenType != null) {
                if (lexer.tokenType == KtTokens.IDENTIFIER) {
                    append(lexer.tokenText)
                }
                lexer.advance()
            }
        }
    }

    private fun MutableCollection<String>.addNamesByExpressionPSI(expression: KtExpression?, validator: (String) -> Boolean) {
        if (expression == null) return
        when (val deparenthesized = KtPsiUtil.safeDeparenthesize(expression)) {
            is KtSimpleNameExpression -> addCamelNames(deparenthesized.getReferencedName(), validator)
            is KtQualifiedExpression -> addNamesByExpressionPSI(deparenthesized.selectorExpression, validator)
            is KtCallExpression -> addNamesByExpressionPSI(deparenthesized.calleeExpression, validator)
            is KtPostfixExpression -> addNamesByExpressionPSI(deparenthesized.baseExpression, validator)
        }
    }

    private fun MutableCollection<String>.addNamesByExpression(
        expression: KtExpression?,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean
    ) {
        if (expression == null) return

        addNamesByValueArgument(expression, bindingContext, validator)
        addNamesByExpressionPSI(expression, validator)
    }

    private fun MutableCollection<String>.addNamesByValueArgument(
        expression: KtExpression,
        bindingContext: BindingContext?,
        validator: (String) -> Boolean
    ) {
        if (bindingContext == null) return
        val argumentExpression = expression.getOutermostParenthesizerOrThis()
        val valueArgument = argumentExpression.parent as? KtValueArgument ?: return
        val resolvedCall = argumentExpression.getParentResolvedCall(bindingContext) ?: return
        val argumentMatch = resolvedCall.getArgumentMapping(valueArgument) as? ArgumentMatch ?: return
        val parameter = argumentMatch.valueParameter
        if (parameter.containingDeclaration.hasStableParameterNames()) {
            addName(parameter.name.asString(), validator)
        }
    }

    private fun MutableCollection<String>.addName(name: String?, validator: (String) -> Boolean) {
        if (name == null) return
        val correctedName = when {
            name.isIdentifier() -> name
            name == "class" -> "clazz"
            else -> return
        }
        add(suggestNameByName(correctedName, validator))
    }
}
