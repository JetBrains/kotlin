/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.backend.konan.KonanFqNames


internal fun KaSession.getInlineTargetTypeOrNull(type: KaType): KaType? {
    if (type !is KaClassType) return null
    val classSymbol = type.symbol as? KaNamedClassSymbol ?: return null
    val inlinedType = getInlineTargetTypeOrNull(classSymbol) ?: return null
    return markNullableIf(inlinedType, type.isMarkedNullable)
}

internal fun KaSession.getInlineTargetTypeOrNull(symbol: KaNamedClassSymbol): KaType? {
    if (!isInlineIncludingKotlinNativeSpecialClasses(symbol)) return null

    val constructor = symbol.declaredMemberScope.constructors
        .find { constructor -> constructor.isPrimary && constructor.valueParameters.size == 1 }
        ?: return null

    val inlinedType = constructor.valueParameters.single().returnType

    /* What if this type is also inline type? */
    val inlineInlineType = getInlineTargetTypeOrNull(inlinedType)
    if (inlineInlineType != null) {
        return inlineInlineType
    }

    return inlinedType
}

/**
 * Kotlin/Native specific implementation for testing if a certain class can be considered 'inline'.
 * Classes that are marked as 'inline' (e.g. ```value class X(val value: Int)```) will be considered inline (of course).
 * However, there seemingly exist classes like 'kotlin.native.internal.NativePtr' which shall also be considered 'inline'
 * despite no modifier being present. This is considered a 'special Kotlin Native' class in the context of this function.
 */
private fun KaSession.isInlineIncludingKotlinNativeSpecialClasses(symbol: KaNamedClassSymbol): Boolean {
    if (symbol.isInline) return true
    val classId = symbol.classId ?: return false

    /* Top Level symbols can be special K/N types */
    if (symbol.containingDeclaration is KaClassSymbol) return false

    if (classId.packageFqName == KonanFqNames.internalPackageName && classId.shortClassName == KonanFqNames.nativePtr.shortName()) {
        return true
    }

    if (classId.packageFqName == InteropFqNames.packageName && classId.shortClassName == InteropFqNames.cPointer.shortName()) {
        return true
    }

    return false
}

private fun KaSession.markNullable(type: KaType): KaType {
    if (type.isMarkedNullable) return type
    return type.withNullability(isMarkedNullable = true)
}

private fun KaSession.markNullableIf(type: KaType, shouldMarkNullable: Boolean): KaType {
    return if (shouldMarkNullable) markNullable(type) else type
}
