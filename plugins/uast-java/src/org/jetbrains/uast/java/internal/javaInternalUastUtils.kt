/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast.java

import com.intellij.openapi.application.ApplicationManager
import com.intellij.psi.*
import com.intellij.psi.tree.IElementType
import org.jetbrains.uast.*

private val MODIFIER_MAP = mapOf(
        UastModifier.ABSTRACT to PsiModifier.ABSTRACT,
        UastModifier.FINAL to PsiModifier.FINAL,
        UastModifier.STATIC to PsiModifier.STATIC
)

internal fun PsiModifierListOwner.hasModifier(modifier: UastModifier): Boolean {
    if (modifier == UastModifier.OVERRIDE && this is PsiAnnotationOwner) {
        return this.annotations.any { it.qualifiedName == "java.lang.Override" }
    }
    if (modifier == UastModifier.VARARG && this is PsiParameter) {
        return this.isVarArgs
    }
    if (modifier == UastModifier.IMMUTABLE && this is PsiVariable) {
        return this.hasModifierProperty(PsiModifier.FINAL)
    }
    if (modifier == UastModifier.FINAL && this is PsiVariable) {
        return false;
    }
    val javaModifier = MODIFIER_MAP[modifier] ?: return false
    return hasModifierProperty(javaModifier)
}

internal fun PsiAnnotationOwner?.getAnnotations(owner: UElement): List<UAnnotation> {
    if (this == null) return emptyList()
    return annotations.map { JavaConverter.convert(it, owner) }
}

internal fun PsiModifierListOwner.getVisibility(): UastVisibility {
    if (hasModifierProperty(PsiModifier.PUBLIC)) return UastVisibility.PUBLIC
    if (hasModifierProperty(PsiModifier.PROTECTED)) return UastVisibility.PROTECTED
    if (hasModifierProperty(PsiModifier.PRIVATE)) return UastVisibility.PRIVATE
    return JavaUastVisibilities.PACKAGE_LOCAL
}

internal fun IElementType.getOperatorType() = when (this) {
    JavaTokenType.EQ -> UastBinaryOperator.ASSIGN
    JavaTokenType.PLUS -> UastBinaryOperator.PLUS
    JavaTokenType.MINUS -> UastBinaryOperator.MINUS
    JavaTokenType.ASTERISK -> UastBinaryOperator.MULT
    JavaTokenType.DIV -> UastBinaryOperator.DIV
    JavaTokenType.PERC -> UastBinaryOperator.MOD
    JavaTokenType.OR -> UastBinaryOperator.BITWISE_OR
    JavaTokenType.AND -> UastBinaryOperator.BITWISE_AND
    JavaTokenType.EQEQ -> UastBinaryOperator.IDENTITY_EQUALS
    JavaTokenType.NE -> UastBinaryOperator.IDENTITY_NOT_EQUALS
    JavaTokenType.GT -> UastBinaryOperator.GREATER
    JavaTokenType.GE -> UastBinaryOperator.GREATER_OR_EQUAL
    JavaTokenType.LT -> UastBinaryOperator.LESS
    JavaTokenType.LE -> UastBinaryOperator.LESS_OR_EQUAL
    JavaTokenType.LTLT -> UastBinaryOperator.SHIFT_LEFT
    JavaTokenType.GTGT -> UastBinaryOperator.SHIFT_RIGHT
    JavaTokenType.GTGTGT -> UastBinaryOperator.UNSIGNED_SHIFT_RIGHT
    JavaTokenType.PLUSEQ -> UastBinaryOperator.PLUS_ASSIGN
    JavaTokenType.MINUSEQ -> UastBinaryOperator.MINUS_ASSIGN
    JavaTokenType.ASTERISKEQ -> UastBinaryOperator.MULTIPLY_ASSIGN
    JavaTokenType.DIVEQ -> UastBinaryOperator.DIVIDE_ASSIGN
    JavaTokenType.PERCEQ -> UastBinaryOperator.REMAINDER_ASSIGN
    JavaTokenType.ANDEQ -> UastBinaryOperator.AND_ASSIGN
    JavaTokenType.XOREQ -> UastBinaryOperator.XOR_ASSIGN
    JavaTokenType.OREQ -> UastBinaryOperator.OR_ASSIGN
    JavaTokenType.LTLTEQ -> UastBinaryOperator.SHIFT_LEFT_ASSIGN
    JavaTokenType.GTGTEQ -> UastBinaryOperator.SHIFT_RIGHT_ASSIGN
    JavaTokenType.GTGTGTEQ -> UastBinaryOperator.UNSIGNED_SHIFT_RIGHT_ASSIGN
    else -> UastBinaryOperator.UNKNOWN
}

internal fun <T> singletonListOrEmpty(element: T?) = if (element != null) listOf(element) else emptyList<T>()

@Suppress("NOTHING_TO_INLINE")
internal inline fun String?.orAnonymous(kind: String = ""): String {
    return this ?: "<anonymous" + (if (kind.isNotBlank()) " $kind" else "") + ">"
}

internal fun <T> runReadAction(action: () -> T): T {
    return ApplicationManager.getApplication().runReadAction<T>(action)
}

internal fun <T> lz(initializer: () -> T) = lazy(LazyThreadSafetyMode.NONE, initializer)