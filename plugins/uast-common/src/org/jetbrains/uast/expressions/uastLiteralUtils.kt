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

fun UElement.isNullLiteral(): Boolean = this is ULiteralExpression && this.isNull

fun UElement.isStringLiteral(): Boolean = this is ULiteralExpression && this.isString

fun UElement.getValueIfStringLiteral(): String? =
        if (isStringLiteral()) (this as ULiteralExpression).value as String else null

fun UElement.isNumberLiteral(): Boolean = this is ULiteralExpression && this.value is Number

fun UElement.isIntegralLiteral(): Boolean = this is ULiteralExpression && when (value) {
    is Int -> true
    is Long -> true
    is Short -> true
    is Char -> true
    is Byte -> true
    else -> false
}

fun UElement.isBooleanLiteral(): Boolean = this is ULiteralExpression && this.isBoolean

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