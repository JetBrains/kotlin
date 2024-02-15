/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.objcexport.analysisApiUtils

import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.symbols.KtClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.analysis.api.types.KtNonErrorClassType
import org.jetbrains.kotlin.analysis.api.types.KtType
import org.jetbrains.kotlin.analysis.api.types.KtTypeNullability
import org.jetbrains.kotlin.backend.konan.InteropFqNames
import org.jetbrains.kotlin.backend.konan.KonanFqNames

context(KtAnalysisSession)
internal fun KtType.getInlineTargetTypeOrNull(): KtType? {
    if (this !is KtNonErrorClassType) return null
    val classSymbol = classSymbol as? KtNamedClassOrObjectSymbol ?: return null
    return classSymbol.getInlineTargetTypeOrNull()?.markNullableIf(isMarkedNullable)
}

context(KtAnalysisSession)
internal fun KtNamedClassOrObjectSymbol.getInlineTargetTypeOrNull(): KtType? {
    if (!isInlineIncludingKotlinNativeSpecialClasses()) return null

    val constructor = getDeclaredMemberScope().getConstructors()
        .find { constructor -> constructor.isPrimary && constructor.valueParameters.size == 1 }
        ?: return null

    val inlinedType = constructor.valueParameters.single().returnType

    /* What if this type is also inline type? */
    val inlineInlineType = inlinedType.getInlineTargetTypeOrNull()
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
context(KtAnalysisSession)
private fun KtNamedClassOrObjectSymbol.isInlineIncludingKotlinNativeSpecialClasses(): Boolean {
    if (this.isInline) return true
    val classId = classIdIfNonLocal ?: return false

    /* Top Level symbols can be special K/N types */
    if (getContainingSymbol() is KtClassOrObjectSymbol) return false

    if (classId.packageFqName == KonanFqNames.internalPackageName && classId.shortClassName == KonanFqNames.nativePtr.shortName()) {
        return true
    }

    if (classId.packageFqName == InteropFqNames.packageName && classId.shortClassName == InteropFqNames.cPointer.shortName()) {
        return true
    }

    return false
}

context(KtAnalysisSession)
private fun KtType.markNullable(): KtType {
    if (this.nullability == KtTypeNullability.NULLABLE) return this
    return this.withNullability(KtTypeNullability.NULLABLE)
}

context(KtAnalysisSession)
private fun KtType.markNullableIf(shouldMarkNullable: Boolean): KtType {
    return if (shouldMarkNullable) markNullable() else this
}