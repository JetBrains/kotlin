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

@file:JvmName("UastExpressionUtils")
package org.jetbrains.uast.util

import org.jetbrains.uast.*

fun UElement.isConstructorCall() = (this as? UCallExpression)?.kind == UastCallKind.CONSTRUCTOR_CALL

fun UElement.isMethodCall() = (this as? UCallExpression)?.kind == UastCallKind.METHOD_CALL

fun UElement.isNewArray() = isNewArrayWithDimensions() || isNewArrayWithInitializer()

fun UElement.isNewArrayWithDimensions() = (this as? UCallExpression)?.kind == UastCallKind.NEW_ARRAY_WITH_DIMENSIONS

fun UElement.isNewArrayWithInitializer() = (this as? UCallExpression)?.kind == UastCallKind.NEW_ARRAY_WITH_INITIALIZER

@Deprecated("Use isArrayInitializer()", ReplaceWith("isArrayInitializer()"))
fun UElement.isNestedArrayInitializer() = isArrayInitializer()

fun UElement.isArrayInitializer() = (this as? UCallExpression)?.kind == UastCallKind.NESTED_ARRAY_INITIALIZER

fun UElement.isTypeCast() = (this as? UBinaryExpressionWithType)?.operationKind is UastBinaryExpressionWithTypeKind.TypeCast

fun UElement.isInstanceCheck() = (this as? UBinaryExpressionWithType)?.operationKind is UastBinaryExpressionWithTypeKind.InstanceCheck

fun UElement.isAssignment() = (this as? UBinaryExpression)?.operator is UastBinaryOperator.AssignOperator