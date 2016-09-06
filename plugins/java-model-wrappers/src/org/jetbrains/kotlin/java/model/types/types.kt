/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

@file:JvmName("JeTypeUtils")
package org.jetbrains.kotlin.java.model.types

import com.intellij.psi.*
import javax.lang.model.type.TypeKind
import javax.lang.model.type.TypeMirror
import java.lang.reflect.Array as RArray

private val PSI_PRIMITIVES_MAP = listOf(
        PsiType.BYTE, PsiType.CHAR, PsiType.DOUBLE,
        PsiType.FLOAT, PsiType.INT, PsiType.LONG,
        PsiType.SHORT, PsiType.BOOLEAN
).associate { it to JePrimitiveType(it) }

private val TYPE_KIND_TO_PSI_PRIMITIVE_MAP = mapOf(
        TypeKind.BYTE to PsiType.BYTE,
        TypeKind.CHAR to PsiType.CHAR,
        TypeKind.DOUBLE to PsiType.DOUBLE,
        TypeKind.FLOAT to PsiType.FLOAT,
        TypeKind.INT to PsiType.INT,
        TypeKind.LONG to PsiType.LONG,
        TypeKind.SHORT to PsiType.SHORT,
        TypeKind.BOOLEAN to PsiType.BOOLEAN)

fun TypeKind?.toJePrimitiveType() = PSI_PRIMITIVES_MAP[TYPE_KIND_TO_PSI_PRIMITIVE_MAP[this]]

fun PsiType.toJePrimitiveType() = PSI_PRIMITIVES_MAP[this]

fun PsiType.toJeType(manager: PsiManager, isRaw: Boolean = false): TypeMirror = when (this) {
    PsiType.VOID -> JeVoidType
    PsiType.NULL -> JeNullType
    is PsiPrimitiveType -> PSI_PRIMITIVES_MAP[this] ?: JeErrorType
    is PsiArrayType -> JeArrayType(this, manager, isRaw)
    is PsiWildcardType -> JeWildcardType(this, isRaw)
    is PsiCapturedWildcardType -> JeCapturedWildcardType(this, manager, isRaw)
    is PsiClassType -> {
        val resolvedClass = this.resolve()
        when (resolvedClass) {
            is PsiTypeParameter -> JeTypeVariableType(this, resolvedClass)
            is PsiClass -> JeDeclaredType(this, resolvedClass, isRaw = isRaw)
            else -> JeErrorType
        }
    }
    is PsiIntersectionType -> JeIntersectionType(this, manager, isRaw)
    else -> JeErrorType
}