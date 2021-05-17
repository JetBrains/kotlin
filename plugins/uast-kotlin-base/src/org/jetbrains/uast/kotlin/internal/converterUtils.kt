/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.uast.DEFAULT_EXPRESSION_TYPES_LIST
import org.jetbrains.uast.DEFAULT_TYPES_LIST
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression

fun expressionTypes(requiredType: Class<out UElement>?) =
    requiredType?.let { arrayOf(it) } ?: DEFAULT_EXPRESSION_TYPES_LIST

fun elementTypes(requiredType: Class<out UElement>?) =
    requiredType?.let { arrayOf(it) } ?: DEFAULT_TYPES_LIST

fun <T : UElement> Array<out Class<out T>>.nonEmptyOr(default: Array<out Class<out UElement>>) =
    takeIf { it.isNotEmpty() } ?: default

inline fun <reified ActualT : UElement> Array<out Class<out UElement>>.el(f: () -> UElement?): UElement? {
    return if (isAssignableFrom(ActualT::class.java)) f() else null
}

inline fun <reified ActualT : UElement> Array<out Class<out UElement>>.expr(f: () -> UExpression?): UExpression? {
    return if (isAssignableFrom(ActualT::class.java)) f() else null
}

fun Array<out Class<out UElement>>.isAssignableFrom(cls: Class<*>) = any { it.isAssignableFrom(cls) }

fun <U : UElement> Array<out Class<out UElement>>.accommodate(vararg makers: UElementAlternative<out U>): Sequence<UElement> {
    val makersSeq = makers.asSequence()
    return this.asSequence()
        .flatMap { requiredType -> makersSeq.filter { requiredType.isAssignableFrom(it.uType) } }
        .distinct()
        .mapNotNull { it.make.invoke() }
}

inline fun <reified U : UElement> alternative(noinline make: () -> U?) = UElementAlternative(U::class.java, make)

class UElementAlternative<U : UElement>(val uType: Class<U>, val make: () -> U?)

val identifiersTokens = setOf(
    KtTokens.IDENTIFIER, KtTokens.CONSTRUCTOR_KEYWORD, KtTokens.OBJECT_KEYWORD,
    KtTokens.THIS_KEYWORD, KtTokens.SUPER_KEYWORD,
    KtTokens.GET_KEYWORD, KtTokens.SET_KEYWORD
)
