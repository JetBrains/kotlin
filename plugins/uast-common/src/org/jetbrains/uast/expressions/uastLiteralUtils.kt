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
@file:JvmName("UastLiteralUtils")
package org.jetbrains.uast

/**
 * Checks if the [UElement] is a null literal.
 *
 * @return true if the receiver is a null literal, false otherwise.
 */
fun UElement.isNullLiteral(): Boolean = this is ULiteralExpression && this.isNull

/**
 * Checks if the [UElement] is a boolean literal.
 *
 * @return true if the receiver is a boolean literal, false otherwise.
 */
fun UElement.isBooleanLiteral(): Boolean = this is ULiteralExpression && this.isBoolean

/**
 * Checks if the [UElement] is a `true` boolean literal.
 *
 * @return true if the receiver is a `true` boolean literal, false otherwise.
 */
fun UElement.isTrueLiteral(): Boolean = this is ULiteralExpression && this.isBoolean && this.value == true

/**
 * Checks if the [UElement] is a `false` boolean literal.
 *
 * @return true if the receiver is a `false` boolean literal, false otherwise.
 */
fun UElement.isFalseLiteral(): Boolean = this is ULiteralExpression && this.isBoolean && this.value == false

/**
 * Checks if the [UElement] is a [String] literal.
 *
 * @return true if the receiver is a [String] literal, false otherwise.
 */
fun UElement.isStringLiteral(): Boolean = this is ULiteralExpression && this.isString

/**
 * Returns the [String] literal value.
 *
 * @return literal text if the receiver is a valid [String] literal, null otherwise.
 */
fun UElement.getValueIfStringLiteral(): String? =
        if (isStringLiteral()) (this as ULiteralExpression).value as String else null

/**
 * Checks if the [UElement] is a [Number] literal (Integer, Long, Float, Double, etc.).
 *
 * @return true if the receiver is a [Number] literal, false otherwise.
 */
fun UElement.isNumberLiteral(): Boolean = this is ULiteralExpression && this.value is Number

/**
 * Checks if the [UElement] is an integral literal (is an [Integer], [Long], [Short], [Char] or [Byte]).
 *
 * @return true if the receiver is an integral literal, false otherwise.
 */
fun UElement.isIntegralLiteral(): Boolean = this is ULiteralExpression && when (value) {
    is Int -> true
    is Long -> true
    is Short -> true
    is Char -> true
    is Byte -> true
    else -> false
}

/**
 * Returns the integral value of the literal.
 *
 * @return long representation of the literal expression value,
 *         0 if the receiver literal expression is not a integral one.
 */
fun ULiteralExpression.getLongValue(): Long = value.let {
    when (it) {
        is Long -> it
        is Int -> it.toLong()
        is Short -> it.toLong()
        is Char -> it.toLong()
        is Byte -> it.toLong()
        else -> 0
    }
}