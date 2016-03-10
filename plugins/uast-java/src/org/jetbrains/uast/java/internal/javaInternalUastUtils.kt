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
    return JavaUastVisibilities.DEFAULT
}

internal fun IElementType.getOperatorType() = when (this) {
    JavaTokenType.PLUS -> UastBinaryOperator.PLUS
    JavaTokenType.MINUS -> UastBinaryOperator.MINUS
    JavaTokenType.ASTERISK -> UastBinaryOperator.MULT
    JavaTokenType.DIV -> UastBinaryOperator.DIV
    JavaTokenType.PERC -> UastBinaryOperator.MOD
    JavaTokenType.OR -> UastBinaryOperator.BITWISE_OR
    JavaTokenType.AND -> UastBinaryOperator.BITWISE_AND
    JavaTokenType.TILDE -> UastBinaryOperator.BITWISE_XOR
    JavaTokenType.EQEQ -> UastBinaryOperator.IDENTITY_EQUALS
    JavaTokenType.NE -> UastBinaryOperator.IDENTITY_NOT_EQUALS
    JavaTokenType.GT -> UastBinaryOperator.GREATER
    JavaTokenType.GE -> UastBinaryOperator.GREATER_OR_EQUAL
    JavaTokenType.LT -> UastBinaryOperator.LESS
    JavaTokenType.LE -> UastBinaryOperator.LESS_OR_EQUAL
    JavaTokenType.LTLT -> UastBinaryOperator.SHIFT_LEFT
    JavaTokenType.GTGT -> UastBinaryOperator.SHIFT_RIGHT
    JavaTokenType.GTGTGT -> UastBinaryOperator.BITWISE_SHIFT_RIGHT
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